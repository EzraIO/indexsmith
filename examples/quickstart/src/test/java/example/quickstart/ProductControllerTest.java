package example.quickstart;

import example.quickstart.generated.Product;
import example.quickstart.generated.ProductRepository;
import io.github.indexsmith.api.repository.BaseHit;
import io.github.indexsmith.client.ClientRequest;
import io.github.indexsmith.client.ClientResponse;
import io.github.indexsmith.client.EsVersion;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.client.SearchRequest;
import io.github.indexsmith.client.SearchResult;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProductControllerTest {
    @Test
    public void returnsAdapterQueryAndTypedHits() throws Exception {
        StubClient client = new StubClient();
        ProductController controller = new ProductController(client, new ProductRepository(client));

        Map<String, Object> response = controller.search("city bike", "", 10);

        assertEquals("es8", response.get("adapter"));
        assertEquals("8.17.3", response.get("serverVersion"));
        assertEquals(1L, response.get("totalHits"));
        assertTrue(response.get("generatedQuery").toString().contains("match_phrase"));
        @SuppressWarnings("unchecked")
        List<BaseHit<Product>> hits = (List<BaseHit<Product>>) response.get("hits");
        assertEquals("City Bike", hits.get(0).getData().getName());
    }

    @Test
    public void rejectsUnsafePageSizeAsBadRequest() throws Exception {
        StubClient client = new StubClient();
        ProductController controller = new ProductController(client, new ProductRepository(client));

        try {
            controller.search("", "", 51);
            fail("Expected page size rejection");
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
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
            return new SearchResult("{\"hits\":{\"total\":{\"value\":1},\"hits\":[{"
                    + "\"_id\":\"1\",\"_index\":\"indexsmith-demo-products\","
                    + "\"_source\":{\"name\":\"City Bike\",\"category\":\"bikes\","
                    + "\"price\":699.0,\"inStock\":true}}]}}");
        }

        @Override
        public void close() throws IOException { }
    }
}
