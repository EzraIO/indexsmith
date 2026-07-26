package io.github.indexsmith.client;

import io.github.indexsmith.client.observation.*;
import io.github.indexsmith.client.policy.*;
import io.github.indexsmith.client.request.*;
import io.github.indexsmith.client.response.*;
import io.github.indexsmith.client.spi.*;
import io.github.indexsmith.client.transport.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JdkHttpTransportTest {
    private HttpServer server;
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicInteger unavailableRequests = new AtomicInteger();

    @Before
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/_bulk", exchange -> {
            method.set(exchange.getRequestMethod());
            body.set(read(exchange.getRequestBody()));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            respond(exchange, 201, "{\"errors\":false}");
        });
        server.createContext("/missing", exchange -> respond(exchange, 404, "{\"error\":\"missing\"}"));
        server.createContext("/empty", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.createContext("/unavailable/_bulk", exchange -> {
            unavailableRequests.incrementAndGet();
            respond(exchange, 503, "{\"error\":\"busy\"}");
        });
        server.start();
    }

    @After
    public void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    public void sendsBodyAuthenticationAndContentType() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("http://localhost:" + server.getAddress().getPort())
                .credentials("elastic", "secret")
                .connectTimeoutMillis(100)
                .build();

        ClientResponse response = new JdkHttpTransport(configuration)
                .perform(ClientRequest.ndjson("POST", "/_bulk", "{}\n"));

        assertEquals(201, response.getStatusCode());
        assertEquals("POST", method.get());
        assertEquals("{}\n", body.get());
        assertEquals("Basic ZWxhc3RpYzpzZWNyZXQ=", authorization.get());
        assertTrue(contentType.get().startsWith(ClientRequest.NDJSON));
    }

    @Test
    public void sendsApiKeyAuthentication() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("http://localhost:" + server.getAddress().getPort())
                .apiKey("id", "secret")
                .build();

        new JdkHttpTransport(configuration).perform(ClientRequest.ndjson("POST", "/_bulk", "{}\n"));

        assertEquals("ApiKey aWQ6c2VjcmV0", authorization.get());
    }

    @Test
    public void appliesConfiguredSslContextWithoutDisablingHostnameVerification() throws Exception {
        SSLContext sslContext = SSLContext.getDefault();
        FakeHttpsConnection connection = new FakeHttpsConnection();
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("https://localhost:9200")
                .sslContext(sslContext)
                .build();

        JdkHttpTransport.configureTls(connection, configuration);

        assertEquals(connection.configuredSocketFactory, connection.getSSLSocketFactory());
        assertTrue(connection.configuredSocketFactory != null);
        assertTrue(connection.getHostnameVerifier() != null);
    }

    @Test
    public void returnsNonSuccessfulResponseWithoutDiscardingBody() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("http://localhost:" + server.getAddress().getPort())
                .build();

        ClientResponse response = new JdkHttpTransport(configuration).perform(ClientRequest.get("/missing"));

        assertFalse(response.isSuccessful());
        assertEquals("{\"error\":\"missing\"}", response.getBody());
    }

    @Test
    public void retriesTransientServerFailuresOnAnotherEndpoint() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("http://localhost:" + server.getAddress().getPort() + "/unavailable")
                .endpoint("http://localhost:" + server.getAddress().getPort())
                .build();

        ClientResponse response = new JdkHttpTransport(configuration)
                .perform(ClientRequest.ndjson("POST", "/_bulk", "{}\n").withRetryable(true));

        assertEquals(201, response.getStatusCode());
        assertEquals(1, response.getRetryCount());
        assertEquals("{}\n", body.get());
    }

    @Test
    public void doesNotReplayNonIdempotentBulkRequest() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("http://localhost:" + server.getAddress().getPort() + "/unavailable")
                .endpoint("http://localhost:" + server.getAddress().getPort())
                .build();

        ClientResponse response = new JdkHttpTransport(configuration)
                .perform(ClientRequest.ndjson("POST", "/_bulk", "{}\n"));

        assertEquals(503, response.getStatusCode());
        assertEquals(0, response.getRetryCount());
        assertEquals(1, unavailableRequests.get());
    }

    @Test
    public void countsFailedAttemptsAfterTheLastHttpResponse() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("http://localhost:" + server.getAddress().getPort())
                .endpoint("http://127.0.0.1:1")
                .connectTimeoutMillis(100)
                .build();

        ClientResponse response = new JdkHttpTransport(configuration)
                .perform(ClientRequest.get("/unavailable/_bulk"));

        assertEquals(503, response.getStatusCode());
        assertEquals(1, response.getRetryCount());
    }

    @Test
    public void handlesErrorResponsesWithoutAnErrorStream() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("http://localhost:" + server.getAddress().getPort())
                .build();

        ClientResponse response = new JdkHttpTransport(configuration).perform(ClientRequest.get("/empty"));

        assertEquals(404, response.getStatusCode());
        assertEquals("", response.getBody());
    }

    private static String read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[128];
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

    private static final class FakeHttpsConnection extends HttpsURLConnection {
        private SSLSocketFactory configuredSocketFactory;
        private FakeHttpsConnection() throws Exception { super(new URL("https://localhost")); }
        @Override public void setSSLSocketFactory(SSLSocketFactory socketFactory) {
            super.setSSLSocketFactory(socketFactory);
            this.configuredSocketFactory = socketFactory;
        }
        @Override public String getCipherSuite() { return "test"; }
        @Override public Certificate[] getLocalCertificates() { return null; }
        @Override public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException { return null; }
        @Override public void disconnect() { }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }
    }
}
