package io.github.outakeshi.indexsmith.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.SSLContext;

public final class ClientConfiguration {
    private final List<String> endpoints;
    private final String username;
    private final String password;
    private final String apiKey;
    private final SSLContext sslContext;
    private final String adapter;
    private final int connectTimeoutMillis;
    private final int socketTimeoutMillis;
    private final int connectionRequestTimeoutMillis;
    private final List<QueryPolicy> queryPolicies;
    private final List<RequestListener> requestListeners;

    private ClientConfiguration(Builder builder) {
        if (builder.endpoints.isEmpty()) {
            throw new IllegalArgumentException("At least one Elasticsearch endpoint is required");
        }
        if (builder.connectTimeoutMillis < 0 || builder.socketTimeoutMillis < 0
                || builder.connectionRequestTimeoutMillis < 0) {
            throw new IllegalArgumentException("Elasticsearch timeouts must not be negative");
        }
        this.endpoints = Collections.unmodifiableList(new ArrayList<>(builder.endpoints));
        this.username = builder.username;
        this.password = builder.password;
        this.apiKey = builder.apiKey;
        this.sslContext = builder.sslContext;
        this.adapter = builder.adapter;
        this.connectTimeoutMillis = builder.connectTimeoutMillis;
        this.socketTimeoutMillis = builder.socketTimeoutMillis;
        this.connectionRequestTimeoutMillis = builder.connectionRequestTimeoutMillis;
        this.queryPolicies = Collections.unmodifiableList(new ArrayList<>(builder.queryPolicies));
        this.requestListeners = Collections.unmodifiableList(new ArrayList<>(builder.requestListeners));
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<String> getEndpoints() { return endpoints; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getApiKey() { return apiKey; }
    public SSLContext getSslContext() { return sslContext; }
    public String getAdapter() { return adapter; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public int getSocketTimeoutMillis() { return socketTimeoutMillis; }
    public int getConnectionRequestTimeoutMillis() { return connectionRequestTimeoutMillis; }
    public List<QueryPolicy> getQueryPolicies() { return queryPolicies; }
    public List<RequestListener> getRequestListeners() { return requestListeners; }

    public static final class Builder {
        private final List<String> endpoints = new ArrayList<>();
        private String username;
        private String password;
        private String apiKey;
        private SSLContext sslContext;
        private String adapter;
        private int connectTimeoutMillis = 5000;
        private int socketTimeoutMillis = 30000;
        private int connectionRequestTimeoutMillis = 30000;
        private final List<QueryPolicy> queryPolicies = new ArrayList<>();
        private final List<RequestListener> requestListeners = new ArrayList<>();

        public Builder endpoint(String endpoint) {
            endpoints.add(normalizeEndpoint(endpoint));
            return this;
        }

        public Builder credentials(String username, String password) {
            if (apiKey != null) {
                throw new IllegalStateException("Basic authentication and API key authentication are mutually exclusive");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Elasticsearch username must not be blank");
            }
            if (password == null) {
                throw new IllegalArgumentException("Elasticsearch password must not be null");
            }
            this.username = username.trim();
            this.password = password;
            return this;
        }

        public Builder apiKey(String id, String secret) {
            if (username != null) {
                throw new IllegalStateException("Basic authentication and API key authentication are mutually exclusive");
            }
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Elasticsearch API key id must not be blank");
            }
            if (secret == null || secret.trim().isEmpty()) {
                throw new IllegalArgumentException("Elasticsearch API key secret must not be blank");
            }
            String token = id.trim() + ":" + secret;
            this.apiKey = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder adapter(String adapter) {
            this.adapter = adapter == null || adapter.trim().isEmpty() ? null : adapter.trim();
            return this;
        }

        public Builder connectTimeoutMillis(int value) {
            this.connectTimeoutMillis = value;
            return this;
        }

        public Builder socketTimeoutMillis(int value) {
            this.socketTimeoutMillis = value;
            return this;
        }

        public Builder connectionRequestTimeoutMillis(int value) {
            this.connectionRequestTimeoutMillis = value;
            return this;
        }

        public Builder queryPolicy(QueryPolicy queryPolicy) {
            if (queryPolicy == null) throw new IllegalArgumentException("queryPolicy must not be null");
            this.queryPolicies.add(queryPolicy);
            return this;
        }

        public Builder requestListener(RequestListener requestListener) {
            if (requestListener == null) throw new IllegalArgumentException("requestListener must not be null");
            this.requestListeners.add(requestListener);
            return this;
        }

        public ClientConfiguration build() {
            return new ClientConfiguration(this);
        }

        private static String normalizeEndpoint(String endpoint) {
            if (endpoint == null || endpoint.trim().isEmpty()) {
                throw new IllegalArgumentException("Elasticsearch endpoint must not be blank");
            }
            String value = endpoint.trim();
            value = value.contains("://") ? value : "http://" + value;
            URI uri;
            try {
                uri = new URI(value);
            } catch (URISyntaxException exception) {
                throw new IllegalArgumentException("Elasticsearch endpoint is not a valid URI", exception);
            }
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException("Elasticsearch endpoint must use HTTP or HTTPS");
            }
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("Elasticsearch endpoint must include a valid host");
            }
            if (uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Elasticsearch credentials must not be embedded in the endpoint");
            }
            if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("Elasticsearch endpoint must not include a query or fragment");
            }
            while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
            return value;
        }
    }
}
