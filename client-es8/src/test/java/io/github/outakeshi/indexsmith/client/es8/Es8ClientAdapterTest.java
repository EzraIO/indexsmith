package io.github.outakeshi.indexsmith.client.es8;

import io.github.outakeshi.indexsmith.client.ClientConfiguration;
import io.github.outakeshi.indexsmith.client.ClientRequest;
import io.github.outakeshi.indexsmith.client.ClientResponse;
import io.github.outakeshi.indexsmith.client.IndexSmithClient;
import io.github.outakeshi.indexsmith.client.IndexSmithClientFactory;
import io.github.outakeshi.indexsmith.client.EsVersion;
import io.github.outakeshi.indexsmith.client.ClientOperation;
import io.github.outakeshi.indexsmith.client.QueryPolicyViolationException;
import io.github.outakeshi.indexsmith.client.SearchRequest;
import io.github.outakeshi.indexsmith.client.SearchResult;
import io.github.outakeshi.indexsmith.client.RequestEvent;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Es8ClientAdapterTest {
    private HttpServer server;
    private String endpoint;

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        endpoint = "http://localhost:" + server.getAddress().getPort();
    }

    @After
    public void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    public void providerDeclaresSupportedRange() {
        Es8ClientProvider provider = new Es8ClientProvider();
        assertFalse(provider.supports(EsVersion.parse("8.16.6")));
        assertTrue(provider.supports(EsVersion.parse("8.17.0")));
        assertTrue(provider.supports(EsVersion.parse("8.19.3")));
        assertFalse(provider.supports(EsVersion.parse("9.0.0")));
    }

    @Test
    public void discoversProviderAndExecutesTypedClientSearch() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/", exchange -> respond(exchange, 200,
                "{\"version\":{\"number\":\"8.17.3\"}}"));
        server.createContext("/products/_search", exchange -> {
            method.set(exchange.getRequestMethod());
            requestBody.set(read(exchange.getRequestBody()));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, searchResponse());
        });
        server.createContext("/products/_doc/1", exchange ->
                respond(exchange, 201, "{\"result\":\"created\"}"));
        server.createContext("/missing", exchange ->
                respond(exchange, 404, "{\"error\":\"not_found\"}"));
        server.createContext("/empty", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint(endpoint)
                .credentials("elastic", "secret")
                .build();
        try (IndexSmithClient client = new IndexSmithClientFactory().create(configuration)) {
            SearchResult result = client.search(new SearchRequest(
                    "products", "{\"query\":{\"match_all\":{}}}"));

            assertEquals("es8", client.getAdapterId());
            assertEquals("POST", method.get());
            assertTrue(requestBody.get().contains("\"match_all\""));
            assertEquals("Basic ZWxhc3RpYzpzZWNyZXQ=", authorization.get());
            assertTrue(result.getResponseBody().trim().startsWith("{"));
            assertFalse(result.getResponseBody().startsWith("SearchResponse:"));
            assertTrue(result.getResponseBody().contains("\"_id\":\"1\""));

            ClientResponse created = client.perform(ClientRequest.json(
                    "PUT", "/products/_doc/1", "{\"name\":\"bike\"}"));
            assertEquals(201, created.getStatusCode());
            assertTrue(created.isSuccessful());
            ClientResponse missing = client.perform(ClientRequest.get("/missing"));
            assertFalse(missing.isSuccessful());
            assertEquals("{\"error\":\"not_found\"}", missing.getBody());
            assertEquals("", client.perform(ClientRequest.get("/empty")).getBody());
        }
    }

    @Test
    public void exposesOfficialClientSearchErrorsAsIoExceptions() throws Exception {
        List<RequestEvent> events = new ArrayList<>();
        server.createContext("/", exchange -> respond(exchange, 200,
                "{\"version\":{\"number\":\"8.17.3\"}}"));
        server.createContext("/products/_search", exchange -> respond(exchange, 400,
                "{\"error\":{\"type\":\"parsing_exception\",\"reason\":\"bad query\"},\"status\":400}"));
        server.start();

        try (IndexSmithClient client = new IndexSmithClientFactory().create(
                ClientConfiguration.builder().endpoint(endpoint).requestListener(events::add).build())) {
            try {
                client.search(new SearchRequest("products", "{\"query\":{\"unknown\":{}}}"));
                fail("Expected search failure");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("search failed"));
                assertTrue(expected.getCause() != null);
            }
        }
        assertEquals(2, events.size());
        assertEquals(RequestEvent.Phase.FAILURE, events.get(1).getPhase());
        assertEquals(400, events.get(1).getStatusCode());
        assertEquals(0, events.get(1).getRetryCount());
    }

    @Test
    public void sendsApiKeyThroughVersionDetectionAndOfficialClient() throws Exception {
        AtomicReference<String> versionAuthorization = new AtomicReference<>();
        AtomicReference<String> searchAuthorization = new AtomicReference<>();
        server.createContext("/", exchange -> {
            versionAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"version\":{\"number\":\"8.17.3\"}}");
        });
        server.createContext("/products/_search", exchange -> {
            searchAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, searchResponse());
        });
        server.start();

        try (IndexSmithClient client = new IndexSmithClientFactory().create(
                ClientConfiguration.builder().endpoint(endpoint).apiKey("id", "secret").build())) {
            client.search(new SearchRequest("products", "{}"));
        }

        assertEquals("ApiKey aWQ6c2VjcmV0", versionAuthorization.get());
        assertEquals("ApiKey aWQ6c2VjcmV0", searchAuthorization.get());
    }

    @Test
    public void preservesEndpointBasePathForVersionDetectionAndSearch() throws Exception {
        AtomicInteger searchRequests = new AtomicInteger();
        server.createContext("/proxy/", exchange -> respond(exchange, 200,
                "{\"version\":{\"number\":\"8.17.3\"}}"));
        server.createContext("/proxy/products/_search", exchange -> {
            searchRequests.incrementAndGet();
            respond(exchange, 200, searchResponse());
        });
        server.start();

        try (IndexSmithClient client = new IndexSmithClientFactory().create(
                ClientConfiguration.builder().endpoint(endpoint + "/proxy").build())) {
            client.search(new SearchRequest("products", "{}"));
        }

        assertEquals(1, searchRequests.get());
    }

    @Test
    public void appliesQueryPolicyBeforeEs8OfficialClient() throws Exception {
        AtomicInteger searchRequests = new AtomicInteger();
        server.createContext("/", exchange -> respond(exchange, 200,
                "{\"version\":{\"number\":\"8.17.3\"}}"));
        server.createContext("/products/_search", exchange -> {
            searchRequests.incrementAndGet();
            respond(exchange, 200, searchResponse());
        });
        server.start();

        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint(endpoint)
                .queryPolicy(operation -> {
                    if (operation.getType() == ClientOperation.Type.SEARCH) {
                        throw new QueryPolicyViolationException("blocked");
                    }
                })
                .build();
        try (IndexSmithClient client = new IndexSmithClientFactory().create(configuration)) {
            try {
                client.search(new SearchRequest("products", "{}"));
                fail("Expected QueryPolicyViolationException");
            } catch (QueryPolicyViolationException expected) {
                assertEquals("blocked", expected.getMessage());
            }
        }
        assertEquals(0, searchRequests.get());
    }

    @Test
    public void retriesSearchAcrossOfficialClientsAndReportsRetryCount() throws Exception {
        HttpServer fallback = HttpServer.create(new InetSocketAddress(0), 0);
        String fallbackEndpoint = "http://localhost:" + fallback.getAddress().getPort();
        List<RequestEvent> events = new ArrayList<>();
        server.createContext("/", exchange -> respond(exchange, 200,
                "{\"version\":{\"number\":\"8.17.3\"}}"));
        server.createContext("/products/_search", exchange -> respond(exchange, 503,
                "{\"error\":{\"type\":\"unavailable_shards_exception\",\"reason\":\"retry\"},\"status\":503}"));
        fallback.createContext("/products/_search", exchange -> respond(exchange, 200, searchResponse()));
        server.start();
        fallback.start();

        try (IndexSmithClient client = new IndexSmithClientFactory().create(
                ClientConfiguration.builder().endpoint(endpoint).endpoint(fallbackEndpoint)
                        .requestListener(events::add).build())) {
            SearchResult result = client.search(new SearchRequest("products", "{}"));
            assertEquals(1, result.getRetryCount());
        } finally {
            fallback.stop(0);
        }

        assertEquals(2, events.size());
        assertEquals(RequestEvent.Phase.SUCCESS, events.get(1).getPhase());
        assertEquals(1, events.get(1).getRetryCount());
    }

    @Test
    public void doesNotReplayOrdinaryPostAcrossEndpoints() throws Exception {
        HttpServer fallback = HttpServer.create(new InetSocketAddress(0), 0);
        String fallbackEndpoint = "http://localhost:" + fallback.getAddress().getPort();
        AtomicInteger fallbackRequests = new AtomicInteger();
        server.createContext("/", exchange -> respond(exchange, 200,
                "{\"version\":{\"number\":\"8.17.3\"}}"));
        server.createContext("/_bulk", exchange -> respond(exchange, 503, "{\"error\":\"busy\"}"));
        fallback.createContext("/_bulk", exchange -> {
            fallbackRequests.incrementAndGet();
            respond(exchange, 200, "{}");
        });
        server.start();
        fallback.start();

        try (IndexSmithClient client = new IndexSmithClientFactory().create(
                ClientConfiguration.builder().endpoint(endpoint).endpoint(fallbackEndpoint).build())) {
            ClientResponse response = client.perform(ClientRequest.ndjson("POST", "/_bulk", "{}\n"));
            assertEquals(503, response.getStatusCode());
            assertEquals(0, response.getRetryCount());
        } finally {
            fallback.stop(0);
        }
        assertEquals(0, fallbackRequests.get());
    }

    private static String searchResponse() {
        return "{\"took\":1,\"timed_out\":false,\"_shards\":{\"total\":1,\"successful\":1,"
                + "\"skipped\":0,\"failed\":0},\"hits\":{\"total\":{\"value\":1,\"relation\":\"eq\"},"
                + "\"max_score\":1.0,\"hits\":[{\"_index\":\"products\",\"_id\":\"1\","
                + "\"_score\":1.0,\"_source\":{\"name\":\"bike\"}}]}}";
    }

    private static String read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("X-Elastic-Product", "Elasticsearch");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
