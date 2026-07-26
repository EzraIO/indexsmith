package example.quickstart.generated;

import io.github.indexsmith.client.ClientRequest;
import io.github.indexsmith.client.ClientResponse;
import io.github.indexsmith.client.EsVersion;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.client.SearchRequest;
import io.github.indexsmith.client.SearchResult;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProductRepositoryTest {
    @Test
    public void buildsTypedProductQuery() throws Exception {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setName("city bike");
        request.setCategory("bikes");

        ProductRepository repository = new ProductRepository(new StubClient());
        String query = repository.buildQuery(request);

        assertEquals("indexsmith-demo-products", repository.getIndex());
        assertTrue(query.contains("match_phrase"));
        assertTrue(query.contains("\"name\":\"city bike\""));
        assertTrue(query.contains("\"category\":\"bikes\""));
    }

    private static final class StubClient implements IndexSmithClient {
        @Override
        public String getAdapterId() { return "es8"; }

        @Override
        public EsVersion getServerVersion() { return EsVersion.parse("8.17.3"); }

        @Override
        public ClientResponse perform(ClientRequest request) {
            return new ClientResponse(200, "{}");
        }

        @Override
        public SearchResult search(SearchRequest request) {
            return new SearchResult("{\"hits\":{\"total\":{\"value\":0},\"hits\":[]}}");
        }

        @Override
        public void close() throws IOException { }
    }
}
