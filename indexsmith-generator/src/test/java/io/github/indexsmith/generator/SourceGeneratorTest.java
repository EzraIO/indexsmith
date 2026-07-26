package io.github.indexsmith.generator;

import io.github.indexsmith.generator.generate.EsClientGenerator;
import io.github.indexsmith.generator.generate.EsFieldEnumGenerator;
import io.github.indexsmith.generator.generate.EsGenerator;
import io.github.indexsmith.generator.generate.EsMappingModelGenerator;
import io.github.indexsmith.generator.generate.EsQueryExampleReqGenerator;
import io.github.indexsmith.generator.model.EsConfigContext;
import io.github.indexsmith.generator.model.EsField;
import io.github.indexsmith.generator.model.EsFieldNode;
import io.github.indexsmith.generator.model.EsIndexInfo;
import io.github.indexsmith.generator.model.GeneratorJavaFile;
import io.github.indexsmith.generator.util.FileUtil;
import io.github.indexsmith.generator.util.EsMappingParseUtil;
import io.github.indexsmith.api.JsonUtil;
import io.github.indexsmith.api.AggregationSpec;
import io.github.indexsmith.api.AggregationType;
import io.github.indexsmith.api.BoolClause;
import io.github.indexsmith.api.HighlightSpec;
import io.github.indexsmith.api.QueryCondition;
import io.github.indexsmith.api.QueryField;
import io.github.indexsmith.api.QueryOperator;
import io.github.indexsmith.api.RangeBounds;
import io.github.indexsmith.api.SortDirection;
import io.github.indexsmith.api.SortOption;
import io.github.indexsmith.api.SourceFilter;
import io.github.indexsmith.client.ClientRequest;
import io.github.indexsmith.client.ClientResponse;
import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.client.EsVersion;
import io.github.indexsmith.client.SearchRequest;
import io.github.indexsmith.client.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Name;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SourceGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generatesUniqueModelSourcesForNestedMapping() {
        List<CompilationUnit> units = new EsMappingModelGenerator().buildEsModel(
                "OrdersModel", mapping(), new Name("example.model"));

        assertEquals(2, units.size());
        String root = units.get(0).toString();
        assertTrue(root.contains("class OrdersModel"));
        assertTrue(root.contains("private String status"));
        assertTrue(root.contains("private List<OrdersItems> items"));
        assertTrue(units.get(1).toString().contains("class OrdersItems"));
    }

    @Test
    public void mappingSnapshotProducesStableGeneratedSchema() throws Exception {
        String mappingJson;
        try (InputStream input = getClass().getResourceAsStream("/snapshots/orders-mapping.json")) {
            assertTrue("mapping snapshot is missing", input != null);
            mappingJson = new String(readAll(input), StandardCharsets.UTF_8);
        }
        JsonNode properties = JsonUtil.readTree(mappingJson)
                .path("orders").path("mappings");
        EsFieldNode parsed = EsMappingParseUtil.parse2Node(properties.toString(),
                Collections.singleton("items"));
        List<CompilationUnit> units = new EsMappingModelGenerator().buildEsModel(
                "OrdersModel", parsed, new Name("example.model"));

        assertEquals(2, units.size());
        String root = units.get(0).toString();
        String nested = units.get(1).toString();
        assertTrue(root.contains("private String status"));
        assertTrue(root.contains("private String createdAt"));
        assertTrue(root.contains("private List<OrdersItems> items"));
        assertTrue(root.contains("public String getCreatedAt()"));
        assertTrue(!root.contains("lombok"));
        assertTrue(nested.contains("private Long skuId"));
        assertTrue(nested.contains("private Integer quantity"));
    }

    @Test
    public void usesObjectPathsToAvoidNestedModelNameCollisions() throws Exception {
        EsFieldNode mapping = EsMappingParseUtil.parse2Node("{\"properties\":{"
                + "\"billing\":{\"properties\":{\"address\":{\"properties\":{"
                + "\"city\":{\"type\":\"keyword\"}}}}},"
                + "\"shipping\":{\"properties\":{\"address\":{\"properties\":{"
                + "\"zip\":{\"type\":\"keyword\"}}}}}}}}}", null);

        List<CompilationUnit> units = new EsMappingModelGenerator().buildEsModel(
                "OrderModel", mapping, new Name("example.model"));
        List<String> typeNames = units.stream()
                .map(unit -> unit.getType(0).getNameAsString())
                .collect(java.util.stream.Collectors.toList());

        assertEquals(5, typeNames.size());
        assertTrue(typeNames.contains("OrderBillingAddress"));
        assertTrue(typeNames.contains("OrderShippingAddress"));
    }

    @Test
    public void generatesFieldEnumQueryModelAndCompilableClientShape() throws Exception {
        EsConfigContext context = context();
        EsFieldNode mapping = mapping();

        GeneratorJavaFile fieldEnum = new EsFieldEnumGenerator().buildFieldEnum("orders", mapping, context);
        GeneratorJavaFile request = new EsQueryExampleReqGenerator().buildReqExample(
                "orders", mapping, context, fieldEnum);
        GeneratorJavaFile model = new EsMappingModelGenerator().buildEsModel("orders", mapping, context).get(0);
        GeneratorJavaFile client = new EsClientGenerator().buildReqExample(
                "orders", mapping, model.getCompilationUnit(), context);

        assertTrue(fieldEnum.getCompilationUnit().toString().contains("ITEMS_SKU_ID"));
        assertTrue(fieldEnum.getCompilationUnit().toString().contains("\"items.skuId\""));
        assertTrue(fieldEnum.getCompilationUnit().toString().contains("implements QueryField"));
        assertTrue(fieldEnum.getCompilationUnit().toString().contains("\"items.skuId\", \"long\", \"items\""));
        assertTrue(request.getCompilationUnit().toString().contains("private Long itemsSkuId"));
        assertTrue(request.getCompilationUnit().toString().contains("public Long getItemsSkuId()"));
        assertTrue(!request.getCompilationUnit().toString().contains("lombok"));
        String clientSource = client.getCompilationUnit().toString();
        assertTrue(clientSource.contains("ClientConfiguration configuration"));
        assertTrue(clientSource.contains("IndexSmithClient client"));
        assertTrue(clientSource.contains("queryBuilder.mustTerm(\"status\", request.getStatus())"));
        assertTrue(clientSource.contains("queryBuilder.mustMatchPhrase(\"description\", request.getDescription())"));
        assertTrue(clientSource.contains("queryBuilder.mustNestedTerm(\"items\", \"items.skuId\", request.getItemsSkuId())"));
        assertTrue(clientSource.contains("queryBuilder.page(request.getPageNo(), request.getPageSize())"));
        assertTrue(clientSource.contains("queryBuilder.sort(request.getSortFieldName(), request.isDescending())"));
        assertTrue(clientSource.contains("queryBuilder.conditions(request.getConditions())"));
        assertTrue(clientSource.contains("queryBuilder.aggregations(request.getAggregations())"));
        assertTrue(clientSource.contains("queryCondition(buildQuery(request), request.getAggregations())"));
        assertTrue(!clientSource.contains("RestHighLevelClient"));
        assertTrue(client.getCompilationUnit().toString().contains("return \"orders\";"));
    }

    @Test
    public void writesGeneratedFileToPackageDirectory() throws Exception {
        CompilationUnit unit = new CompilationUnit("example.generated");
        unit.addClass("Sample").setPublic(true);
        GeneratorJavaFile javaFile = new GeneratorJavaFile(
                temporaryFolder.getRoot().getAbsolutePath(), "example.generated", unit);

        FileUtil.writeFile(javaFile);

        File output = new File(temporaryFolder.getRoot(), "example/generated/Sample.java");
        assertTrue(output.isFile());
        assertTrue(new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8)
                .contains("public class Sample"));
    }

    @Test
    public void generatesCompleteSourceSetIntoConfiguredPackages() throws Exception {
        EsConfigContext context = context();
        context.setEsFieldNodeMap(Collections.singletonMap("orders", mapping()));

        EsGenerator.generator(context);

        assertTrue(new File(temporaryFolder.getRoot(), "example/model/OrdersModel.java").isFile());
        assertTrue(new File(temporaryFolder.getRoot(), "example/model/OrdersItems.java").isFile());
        assertTrue(new File(temporaryFolder.getRoot(), "example/fields/OrdersFieldEnum.java").isFile());
        assertTrue(new File(temporaryFolder.getRoot(), "example/request/OrdersReq.java").isFile());
        assertTrue(new File(temporaryFolder.getRoot(), "example/client/OrdersClient.java").isFile());

        try (Stream<java.nio.file.Path> generated = Files.walk(temporaryFolder.getRoot().toPath())) {
            assertEquals(5, generated.filter(path -> path.toString().endsWith(".java")).count());
        }

        File compiledClasses = compileGeneratedSources();
        assertGeneratedQueryRuns(compiledClasses);
        assertGeneratedConfigurationHonorsPolicy(compiledClasses);
    }

    @Test(expected = IllegalStateException.class)
    public void reportsInvalidOutputDirectory() throws Exception {
        EsConfigContext context = context();
        context.setEsFieldNodeMap(Collections.singletonMap("orders", mapping()));
        context.setTargetProject(temporaryFolder.newFile("not-a-directory").getAbsolutePath());

        EsGenerator.generator(context);
    }

    private EsConfigContext context() {
        EsIndexInfo info = new EsIndexInfo();
        info.setIndexNames("orders");
        info.setQueryConditionFields(new HashSet<>(Arrays.asList("status", "description", "items.skuId")));
        EsConfigContext context = new EsConfigContext();
        context.setTargetProject(temporaryFolder.getRoot().getAbsolutePath());
        context.setModelTargetPackage("example.model");
        context.setFieldEnumTargetPackage("example.fields");
        context.setReqTargetPackage("example.request");
        context.setClientTargetPackage("example.client");
        context.setEsIndexInfoMap(Collections.singletonMap("orders", info));
        return context;
    }

    private File compileGeneratedSources() throws Exception {
        File classes = temporaryFolder.newFolder("compiled-classes");
        GeneratedSourceCompiler.compile(temporaryFolder.getRoot().toPath(), classes);
        return classes;
    }

    private void assertGeneratedQueryRuns(File compiledClasses) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(
                new java.net.URL[]{compiledClasses.toURI().toURL()}, getClass().getClassLoader())) {
            Class<?> requestType = loader.loadClass("example.request.OrdersReq");
            Object request = requestType.getDeclaredConstructor().newInstance();
            requestType.getMethod("setStatus", String.class).invoke(request, "paid");
            requestType.getMethod("setDescription", String.class).invoke(request, "red bike");
            requestType.getMethod("setItemsSkuId", Long.class).invoke(request, 42L);
            requestType.getMethod("setPageNo", int.class).invoke(request, 2);
            requestType.getMethod("setPageSize", int.class).invoke(request, 10);
            requestType.getMethod("setSortFieldName", String.class).invoke(request, "createdAt");
            requestType.getMethod("setDescending", boolean.class).invoke(request, true);
            requestType.getMethod("setConditions", List.class).invoke(request, Collections.singletonList(
                    QueryCondition.builder(QueryOperator.RANGE, "price").clause(BoolClause.FILTER)
                            .range(RangeBounds.builder().gte(10).build()).build()));
            requestType.getMethod("setSorts", List.class).invoke(request, Arrays.asList(
                    new SortOption("createdAt", SortDirection.ASC),
                    new SortOption("_id", SortDirection.ASC)));
            requestType.getMethod("setSearchAfter", List.class).invoke(request, Arrays.<Object>asList(100, "42"));
            requestType.getMethod("setSourceFilter", SourceFilter.class).invoke(request,
                    new SourceFilter(Collections.singletonList("status"), Collections.singletonList("secret")));
            requestType.getMethod("setHighlight", HighlightSpec.class).invoke(request,
                    HighlightSpec.builder().field("description").build());
            requestType.getMethod("setAggregations", List.class).invoke(request, Collections.singletonList(
                    AggregationSpec.builder("statuses", AggregationType.TERMS).field("status").build()));

            Class<?> clientType = loader.loadClass("example.client.OrdersClient");
            Object client = clientType.getConstructor(IndexSmithClient.class)
                    .newInstance(new StubClient());
            String body = (String) clientType.getMethod("buildQuery", requestType).invoke(client, request);
            JsonNode json = JsonUtil.readTree(body);

            assertTrue(!json.has("from"));
            assertEquals(10, json.path("size").asInt());
            assertEquals("red bike", json.path("query").path("bool").path("must").get(0)
                    .path("match_phrase").path("description").asText());
            assertEquals("paid", json.path("query").path("bool").path("must").get(1)
                    .path("term").path("status").asText());
            assertEquals("items", json.path("query").path("bool").path("must").get(2)
                    .path("nested").path("path").asText());
            assertEquals(10, json.path("query").path("bool").path("filter").get(0)
                    .path("range").path("price").path("gte").asInt());
            assertEquals("asc", json.path("sort").get(0).path("createdAt").path("order").asText());
            assertEquals(2, json.path("search_after").size());
            assertEquals("secret", json.path("_source").path("excludes").get(0).asText());
            assertTrue(json.path("highlight").path("fields").has("description"));
            assertTrue(json.path("aggs").has("statuses"));

            Class<?> enumType = loader.loadClass("example.fields.OrdersFieldEnum");
            Object nestedField = Enum.valueOf((Class) enumType, "ITEMS_SKU_ID");
            assertTrue(nestedField instanceof QueryField);
            assertEquals("items", ((QueryField) nestedField).getNestedPath());
        }
    }

    private void assertGeneratedConfigurationHonorsPolicy(File compiledClasses) throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress(0), 0);
        java.util.concurrent.atomic.AtomicInteger searchRequests = new java.util.concurrent.atomic.AtomicInteger();
        server.createContext("/", exchange -> {
            byte[] response = "{\"version\":{\"number\":\"7.17.22\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/orders/_search", exchange -> {
            searchRequests.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("http://localhost:" + server.getAddress().getPort())
                .queryPolicy(operation -> {
                    if (operation.getType()
                            == io.github.indexsmith.client.ClientOperation.Type.SEARCH) {
                        throw new io.github.indexsmith.client.QueryPolicyViolationException("blocked");
                    }
                })
                .build();
        try (URLClassLoader loader = new URLClassLoader(
                new java.net.URL[]{compiledClasses.toURI().toURL()}, getClass().getClassLoader())) {
            Class<?> requestType = loader.loadClass("example.request.OrdersReq");
            Class<?> clientType = loader.loadClass("example.client.OrdersClient");
            Object generatedClient = clientType.getConstructor(ClientConfiguration.class).newInstance(configuration);
            try {
                try {
                    clientType.getMethod("query", requestType)
                            .invoke(generatedClient, requestType.getDeclaredConstructor().newInstance());
                    throw new AssertionError("Expected generated query to be rejected");
                } catch (java.lang.reflect.InvocationTargetException exception) {
                    assertTrue(exception.getCause() instanceof
                            io.github.indexsmith.client.QueryPolicyViolationException);
                }
            } finally {
                clientType.getMethod("close").invoke(generatedClient);
            }
        } finally {
            server.stop(0);
        }

        assertEquals(0, searchRequests.get());
    }

    private static final class StubClient implements IndexSmithClient {
        @Override
        public String getAdapterId() { return "stub"; }

        @Override
        public EsVersion getServerVersion() { return EsVersion.parse("8.17.3"); }

        @Override
        public ClientResponse perform(ClientRequest request) { return new ClientResponse(200, "{}"); }

        @Override
        public SearchResult search(SearchRequest request) { return new SearchResult("{}"); }

        @Override
        public void close() throws IOException { }
    }

    private static EsFieldNode mapping() {
        LinkedHashMap<String, EsField> rootFields = new LinkedHashMap<>();
        rootFields.put("status", EsField.builder().esType("keyword").arrayField(false).build());
        rootFields.put("description", EsField.builder().esType("text").arrayField(false).build());
        LinkedHashMap<String, EsField> itemFields = new LinkedHashMap<>();
        itemFields.put("skuId", EsField.builder().esType("long").arrayField(false).build());
        EsFieldNode item = EsFieldNode.builder().esType("nested").arrayField(true)
                .fieldMap(itemFields).nodeMap(Collections.emptyMap()).build();
        LinkedHashMap<String, EsFieldNode> nodes = new LinkedHashMap<>();
        nodes.put("items", item);
        return EsFieldNode.builder().fieldMap(rootFields).nodeMap(nodes).build();
    }

    private static byte[] readAll(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
        return output.toByteArray();
    }
}
