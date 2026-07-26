package io.github.indexsmith.client.es8;

import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.ClientRequest;
import io.github.indexsmith.client.ClientResponse;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.client.IndexSmithClientFactory;
import io.github.indexsmith.client.SearchRequest;
import io.github.indexsmith.client.SearchResult;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Es8ClientIntegrationIT {
    @ClassRule
    public static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            System.getProperty("indexsmith.test.es8.image",
                    "docker.elastic.co/elasticsearch/elasticsearch:8.17.3"))
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
            assertEquals("es8", client.getAdapterId());
            assertEquals(8, client.getServerVersion().getMajor());
            assertTrue(result.getResponseBody().contains("\"name\":\"bike\""));
        }
    }
}
