package io.github.outakeshi.indexsmith.client.es7;

import io.github.outakeshi.indexsmith.client.ClientConfiguration;
import io.github.outakeshi.indexsmith.client.ClientRequest;
import io.github.outakeshi.indexsmith.client.ClientResponse;
import io.github.outakeshi.indexsmith.client.IndexSmithClient;
import io.github.outakeshi.indexsmith.client.IndexSmithClientFactory;
import io.github.outakeshi.indexsmith.client.SearchRequest;
import io.github.outakeshi.indexsmith.client.SearchResult;
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
