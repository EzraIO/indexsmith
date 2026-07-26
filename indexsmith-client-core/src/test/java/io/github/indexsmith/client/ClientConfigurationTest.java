package io.github.indexsmith.client;

import io.github.indexsmith.client.observation.*;
import io.github.indexsmith.client.policy.*;
import io.github.indexsmith.client.request.*;
import io.github.indexsmith.client.response.*;
import io.github.indexsmith.client.spi.*;
import io.github.indexsmith.client.transport.*;

import org.junit.Test;

import javax.net.ssl.SSLContext;

import static org.junit.Assert.assertEquals;

public class ClientConfigurationTest {
    @Test
    public void preservesHttpsAndRemovesTrailingSlash() {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("https://localhost:9200/")
                .connectTimeoutMillis(100)
                .socketTimeoutMillis(200)
                .connectionRequestTimeoutMillis(300)
                .build();

        assertEquals("https://localhost:9200", configuration.getEndpoints().get(0));
        assertEquals(100, configuration.getConnectTimeoutMillis());
        assertEquals(200, configuration.getSocketTimeoutMillis());
        assertEquals(300, configuration.getConnectionRequestTimeoutMillis());
    }

    @Test
    public void configuresApiKeySslContextPoliciesAndListeners() throws Exception {
        SSLContext sslContext = SSLContext.getDefault();
        QueryPolicy policy = operation -> { };
        RequestListener listener = event -> { };
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint("https://localhost:9200")
                .apiKey("id", "secret")
                .sslContext(sslContext)
                .queryPolicy(policy)
                .requestListener(listener)
                .build();

        assertEquals("aWQ6c2VjcmV0", configuration.getApiKey());
        assertEquals(sslContext, configuration.getSslContext());
        assertEquals(policy, configuration.getQueryPolicies().get(0));
        assertEquals(listener, configuration.getRequestListeners().get(0));
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsApiKeyAfterBasicAuthentication() {
        ClientConfiguration.builder().endpoint("localhost:9200")
                .credentials("elastic", "secret").apiKey("id", "secret");
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsBasicAuthenticationAfterApiKey() {
        ClientConfiguration.builder().endpoint("localhost:9200")
                .apiKey("id", "secret").credentials("elastic", "secret");
    }

    @Test(expected = IllegalArgumentException.class)
    public void requiresAtLeastOneEndpoint() {
        ClientConfiguration.builder().build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankEndpoint() {
        ClientConfiguration.builder().endpoint("  ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeTimeout() {
        ClientConfiguration.builder().endpoint("localhost:9200").socketTimeoutMillis(-1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIncompleteCredentials() {
        ClientConfiguration.builder().endpoint("localhost:9200").credentials("elastic", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCredentialsEmbeddedInEndpoint() {
        ClientConfiguration.builder().endpoint("https://elastic:secret@localhost:9200");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEndpointQueryParameters() {
        ClientConfiguration.builder().endpoint("https://localhost:9200?token=secret");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedEndpointScheme() {
        ClientConfiguration.builder().endpoint("file://localhost/tmp/elasticsearch");
    }
}
