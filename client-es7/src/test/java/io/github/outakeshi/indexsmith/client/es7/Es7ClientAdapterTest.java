package io.github.outakeshi.indexsmith.client.es7;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Es7ClientAdapterTest {
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
        Es7ClientProvider provider = new Es7ClientProvider();
        assertFalse(provider.supports(EsVersion.parse("7.10.2")));
        assertTrue(provider.supports(EsVersion.parse("7.11.0")));
        assertTrue(provider.supports(EsVersion.parse("7.17.22")));
        assertFalse(provider.supports(EsVersion.parse("8.0.0")));
    }

    @Test
    public void discoversProviderAndExecutesSearch() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/", exchange -> respond(exchange, 200,
                "{\"version\":{\"number\":\"7.17.22\"}}"));
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
        server.start();

        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint(endpoint)
                .credentials("elastic", "secret")
                .build();
        try (IndexSmithClient client = new IndexSmithClientFactory().create(configuration)) {
            SearchResult result = client.search(new SearchRequest(
                    "products", "{\"query\":{\"match_all\":{}}}"));

            assertEquals("es7", client.getAdapterId());
            assertEquals("POST", method.get());
            assertEquals("{\"query\":{\"match_all\":{}}}", requestBody.get());
            assertEquals("Basic ZWxhc3RpYzpzZWNyZXQ=", authorization.get());
            assertTrue(result.getResponseBody().contains("\"_id\":\"1\""));

            ClientResponse created = client.perform(ClientRequest.json(
                    "PUT", "/products/_doc/1", "{\"name\":\"bike\"}"));
            assertEquals(201, created.getStatusCode());
            assertTrue(created.isSuccessful());
            assertFalse(client.perform(ClientRequest.get("/missing")).isSuccessful());
        }
    }

    @Test
    public void appliesQueryPolicyBeforeEs7SearchTransport() throws Exception {
        AtomicInteger searchRequests = new AtomicInteger();
        server.createContext("/", exchange -> respond(exchange, 200,
                "{\"version\":{\"number\":\"7.17.22\"}}"));
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
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
