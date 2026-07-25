package io.github.outakeshi.indexsmith.generator;

import io.github.outakeshi.indexsmith.api.AggregationResult;
import io.github.outakeshi.indexsmith.api.AggregationSpec;
import io.github.outakeshi.indexsmith.api.AggregationType;
import io.github.outakeshi.indexsmith.api.BaseHit;
import io.github.outakeshi.indexsmith.api.BoolClause;
import io.github.outakeshi.indexsmith.api.HighlightSpec;
import io.github.outakeshi.indexsmith.api.PageSearchResponse;
import io.github.outakeshi.indexsmith.api.QueryCondition;
import io.github.outakeshi.indexsmith.api.QueryOperator;
import io.github.outakeshi.indexsmith.api.RangeBounds;
import io.github.outakeshi.indexsmith.api.SortDirection;
import io.github.outakeshi.indexsmith.api.SortOption;
import io.github.outakeshi.indexsmith.client.ClientConfiguration;
import io.github.outakeshi.indexsmith.client.ClientRequest;
import io.github.outakeshi.indexsmith.client.ClientResponse;
import io.github.outakeshi.indexsmith.client.IndexSmithClient;
import io.github.outakeshi.indexsmith.client.IndexSmithClientFactory;
import io.github.outakeshi.indexsmith.generator.generate.EsGenerator;
import io.github.outakeshi.indexsmith.generator.model.EsConfigContext;
import io.github.outakeshi.indexsmith.generator.model.EsIndexInfo;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

abstract class GeneratedRepositoryIntegrationSupport {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected void verifiesGeneratedRepository(String image, String expectedAdapter) throws Exception {
        ElasticsearchContainer container = new ElasticsearchContainer(image)
                .withEnv("xpack.security.enabled", "false")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
        container.start();
        try {
            ClientConfiguration configuration = ClientConfiguration.builder()
                    .endpoint("http://" + container.getHttpHostAddress()).build();
            createIndexAndGenerate(configuration);
            indexFixtures(configuration);
            executeGeneratedRepository(configuration, expectedAdapter);
        } finally {
            container.stop();
        }
    }

    private void createIndexAndGenerate(ClientConfiguration configuration) throws Exception {
        IndexSmithClient client = new IndexSmithClientFactory().create(configuration);
        EsConfigContext context = null;
        try {
            ClientResponse created = client.perform(ClientRequest.json("PUT", "/orders", "{\"mappings\":{\"properties\":{"
                    + "\"status\":{\"type\":\"keyword\"},\"description\":{\"type\":\"text\"},"
                    + "\"price\":{\"type\":\"double\"},\"items\":{\"type\":\"nested\",\"properties\":{"
                    + "\"skuId\":{\"type\":\"long\"}}}}}}"));
            assertTrue("Unable to create integration index: " + created.getBody(), created.isSuccessful());

            EsIndexInfo info = new EsIndexInfo();
            info.setIndexNames("orders");
            info.setArrFieldPath(Collections.singleton("items"));
            info.setQueryConditionFields(new HashSet<>(Arrays.asList("status", "description", "items.skuId")));
            context = new EsConfigContext(Collections.singletonList(info), client);
            context.setTargetProject(temporaryFolder.getRoot().getAbsolutePath());
            context.setModelTargetPackage("integration.model");
            context.setFieldEnumTargetPackage("integration.fields");
            context.setReqTargetPackage("integration.request");
            context.setClientTargetPackage("integration.client");
            EsGenerator.generator(context);
        } finally {
            if (context == null) client.close();
            else context.close();
        }
    }

    private void indexFixtures(ClientConfiguration configuration) throws Exception {
        try (IndexSmithClient client = new IndexSmithClientFactory().create(configuration)) {
            ClientResponse first = client.perform(ClientRequest.json("PUT", "/orders/_doc/1",
                    "{\"status\":\"paid\",\"description\":\"red bike\",\"price\":42.5,"
                            + "\"items\":[{\"skuId\":42}]}"));
            ClientResponse second = client.perform(ClientRequest.json("PUT", "/orders/_doc/2?refresh=true",
                    "{\"status\":\"paid\",\"description\":\"blue bike\",\"price\":120,"
                            + "\"items\":[{\"skuId\":99}]}"));
            assertTrue(first.isSuccessful());
            assertTrue(second.isSuccessful());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeGeneratedRepository(ClientConfiguration configuration, String expectedAdapter) throws Exception {
        File classes = compileGeneratedSources();
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{classes.toURI().toURL()},
                getClass().getClassLoader())) {
            Class<?> requestType = loader.loadClass("integration.request.OrdersReq");
            Object request = requestType.getDeclaredConstructor().newInstance();
            requestType.getMethod("setStatus", String.class).invoke(request, "paid");
            requestType.getMethod("setDescription", String.class).invoke(request, "red bike");
            requestType.getMethod("setItemsSkuId", Long.class).invoke(request, 42L);
            requestType.getMethod("setConditions", List.class).invoke(request, Collections.singletonList(
                    QueryCondition.builder(QueryOperator.RANGE, "price").clause(BoolClause.FILTER)
                            .range(RangeBounds.builder().gte(10).lte(100).build()).build()));
            requestType.getMethod("setSorts", List.class).invoke(request,
                    Collections.singletonList(new SortOption("price", SortDirection.ASC)));
            requestType.getMethod("setHighlight", HighlightSpec.class).invoke(request,
                    HighlightSpec.builder().field("description").tags("<em>", "</em>").build());
            requestType.getMethod("setAggregations", List.class).invoke(request, Collections.singletonList(
                    AggregationSpec.builder("statuses", AggregationType.TERMS).field("status").build()));

            Class<?> clientType = loader.loadClass("integration.client.OrdersClient");
            Object generatedClient = clientType.getConstructor(ClientConfiguration.class).newInstance(configuration);
            try {
                IndexSmithClient selected = (IndexSmithClient) clientType.getMethod("getEsClient")
                        .invoke(generatedClient);
                assertEquals(expectedAdapter, selected.getAdapterId());
                PageSearchResponse<BaseHit<?>> response = (PageSearchResponse<BaseHit<?>>) clientType
                        .getMethod("query", requestType).invoke(generatedClient, request);

                assertEquals(1, response.getHits().size());
                BaseHit<?> hit = response.getHits().get(0);
                assertEquals("1", hit.getId());
                assertFalse(hit.getHighlight().get("description").isEmpty());
                assertEquals(1, hit.getSortValues().size());
                AggregationResult statuses = response.getAggregations().get("statuses");
                assertEquals(AggregationType.TERMS, statuses.getType());
                assertEquals("paid", statuses.getBuckets().get(0).getKey());

                requestType.getMethod("setSearchAfter", List.class).invoke(request, hit.getSortValues());
                PageSearchResponse<?> next = (PageSearchResponse<?>) clientType.getMethod("query", requestType)
                        .invoke(generatedClient, request);
                assertTrue(next.getHits().isEmpty());
            } finally {
                clientType.getMethod("close").invoke(generatedClient);
            }
        }
    }

    private File compileGeneratedSources() throws Exception {
        File classes = temporaryFolder.newFolder("compiled-integration-classes");
        GeneratedSourceCompiler.compile(temporaryFolder.getRoot().toPath(), classes);
        return classes;
    }
}
