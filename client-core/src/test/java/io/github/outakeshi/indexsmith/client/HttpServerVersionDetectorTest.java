package io.github.outakeshi.indexsmith.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpServerVersionDetectorTest {
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
    public void detectsVersionAndSendsBasicAuthentication() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"version\":{\"number\":\"8.17.3\"}}");
        });
        server.start();

        EsVersion version = new HttpServerVersionDetector().detect(ClientConfiguration.builder()
                .endpoint(endpoint)
                .credentials("elastic", "secret")
                .build());

        assertEquals("8.17.3", version.toString());
        assertEquals("Basic ZWxhc3RpYzpzZWNyZXQ=", authorization.get());
    }

    @Test
    public void detectsVersionAndSendsApiKeyAuthentication() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"version\":{\"number\":\"8.17.3\"}}");
        });
        server.start();

        new HttpServerVersionDetector().detect(ClientConfiguration.builder()
                .endpoint(endpoint).apiKey("id", "secret").build());

        assertEquals("ApiKey aWQ6c2VjcmV0", authorization.get());
    }

    @Test
    public void reportsNonSuccessfulResponse() throws Exception {
        server.createContext("/", exchange -> respond(exchange, 401, "unauthorized"));
        server.start();

        try {
            new HttpServerVersionDetector().detect(ClientConfiguration.builder().endpoint(endpoint).build());
            fail("Expected IOException");
        } catch (IOException error) {
            assertTrue(error.getMessage().contains("HTTP 401"));
            assertTrue(error.getMessage().contains("unauthorized"));
        }
    }

    @Test
    public void fallsBackToAnotherConfiguredEndpoint() throws Exception {
        server.createContext("/unavailable/", exchange -> respond(exchange, 503, "not ready"));
        server.createContext("/", exchange -> respond(exchange, 200,
                "{\"version\":{\"number\":\"7.17.22\"}}"));
        server.start();

        EsVersion version = new HttpServerVersionDetector().detect(ClientConfiguration.builder()
                .endpoint(endpoint + "/unavailable")
                .endpoint(endpoint)
                .build());

        assertEquals("7.17.22", version.toString());
    }

    @Test(expected = IOException.class)
    public void rejectsResponseWithoutVersionNumber() throws Exception {
        server.createContext("/", exchange -> respond(exchange, 200, "{\"name\":\"node\"}"));
        server.start();
        new HttpServerVersionDetector().detect(ClientConfiguration.builder().endpoint(endpoint).build());
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
