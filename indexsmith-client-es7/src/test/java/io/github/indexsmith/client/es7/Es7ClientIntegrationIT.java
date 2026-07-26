package io.github.indexsmith.client.es7;

import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.request.ClientRequest;
import io.github.indexsmith.client.response.ClientResponse;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.client.IndexSmithClientFactory;
import io.github.indexsmith.client.request.SearchRequest;
import io.github.indexsmith.client.response.SearchResult;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Es7ClientIntegrationIT {
    @ClassRule
    public static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            System.getProperty("indexsmith.test.es7.image",
                    "docker.elastic.co/elasticsearch/elasticsearch:7.17.22"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @Test
    public void searchesRealElasticsearchCluster() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("http://" + ELASTICSEARCH.getHttpHostAddress())
                .build();

        try (IndexSmithClient client = new IndexSmithClientFactory().create(configuration)) {
            ClientResponse indexed = client.perform(ClientRequest.json("PUT",
                    "/generated_it/_doc/1?refresh=true", "{\"name\":\"bike\"}"));
            assertTrue(indexed.isSuccessful());
            SearchResult result = client.search(new SearchRequest("generated_it",
                    "{\"query\":{\"term\":{\"name.keyword\":\"bike\"}}}"));
            assertEquals("es7", client.getAdapterId());
            assertEquals(7, client.getServerVersion().getMajor());
            assertTrue(result.getResponseBody().contains("\"name\":\"bike\""));
        }
    }
}
