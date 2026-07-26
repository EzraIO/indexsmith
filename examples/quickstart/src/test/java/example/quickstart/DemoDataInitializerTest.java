package example.quickstart;

import example.quickstart.generated.ProductRepository;
import io.github.indexsmith.client.request.ClientRequest;
import io.github.indexsmith.client.response.ClientResponse;
import io.github.indexsmith.client.EsVersion;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.client.request.SearchRequest;
import io.github.indexsmith.client.response.SearchResult;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DemoDataInitializerTest {
    @Test
    public void createsMissingDemoIndexWithoutDeletingAnything() throws Exception {
        RecordingClient client = new RecordingClient(false);

        new DemoDataInitializer(client, new ProductRepository(client)).run(null);

        assertEquals(6, client.requests.size());
        assertRequest(client.requests.get(0), "GET", "/indexsmith-demo-products");
        assertRequest(client.requests.get(1), "PUT", "/indexsmith-demo-products");
        assertTrue(client.requests.get(1).getBody().contains("\"properties\""));
        assertRequest(client.requests.get(5), "GET", "/indexsmith-demo-products/_refresh");
        assertFalse(hasMethod(client.requests, "DELETE"));
    }

    @Test
    public void reusesExistingDemoIndexOnRestart() throws Exception {
        RecordingClient client = new RecordingClient(true);

        new DemoDataInitializer(client, new ProductRepository(client)).run(null);

        assertEquals(5, client.requests.size());
        assertRequest(client.requests.get(0), "GET", "/indexsmith-demo-products");
        assertRequest(client.requests.get(1), "PUT", "/indexsmith-demo-products/_doc/1");
        assertFalse(hasRootIndexPut(client.requests));
        assertFalse(hasMethod(client.requests, "DELETE"));
    }

    private static void assertRequest(ClientRequest request, String method, String path) {
        assertEquals(method, request.getMethod());
        assertEquals(path, request.getPath());
    }

    private static boolean hasMethod(List<ClientRequest> requests, String method) {
        for (ClientRequest request : requests) {
            if (method.equals(request.getMethod())) return true;
        }
        return false;
    }

    private static boolean hasRootIndexPut(List<ClientRequest> requests) {
        for (ClientRequest request : requests) {
            if ("PUT".equals(request.getMethod())
                    && "/indexsmith-demo-products".equals(request.getPath())) return true;
        }
        return false;
    }

    private static final class RecordingClient implements IndexSmithClient {
        private final boolean indexExists;
        private final List<ClientRequest> requests = new ArrayList<>();

        private RecordingClient(boolean indexExists) {
            this.indexExists = indexExists;
        }

        @Override
        public String getAdapterId() { return "es8"; }

        @Override
        public EsVersion getServerVersion() { return EsVersion.parse("8.17.3"); }

        @Override
        public ClientResponse perform(ClientRequest request) {
            requests.add(request);
            if ("GET".equals(request.getMethod())
                    && "/indexsmith-demo-products".equals(request.getPath())) {
                return new ClientResponse(indexExists ? 200 : 404, "{}");
            }
            if (request.getPath().contains("/_doc/")) {
                String id = request.getPath().substring(request.getPath().lastIndexOf('/') + 1);
                return new ClientResponse(201, "{\"_index\":\"indexsmith-demo-products\","
                        + "\"_id\":\"" + id + "\",\"result\":\"created\"}");
            }
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
