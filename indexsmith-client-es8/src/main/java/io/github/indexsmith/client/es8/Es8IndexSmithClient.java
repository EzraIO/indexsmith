package io.github.indexsmith.client.es8;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.request.ClientRequest;
import io.github.indexsmith.client.request.ClientRequestException;
import io.github.indexsmith.client.response.ClientResponse;
import io.github.indexsmith.client.EsVersion;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.client.transport.JdkHttpTransport;
import io.github.indexsmith.client.request.SearchRequest;
import io.github.indexsmith.client.response.SearchResult;
import org.apache.http.HttpHost;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.message.BasicHeader;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

final class Es8IndexSmithClient implements IndexSmithClient {
    private final List<EndpointClient> endpointClients;
    private final JdkHttpTransport rawTransport;
    private final JsonpMapper mapper;
    private final EsVersion serverVersion;

    Es8IndexSmithClient(ClientConfiguration configuration, EsVersion serverVersion) {
        this.mapper = new JacksonJsonpMapper();
        this.endpointClients = new ArrayList<>();
        for (String endpoint : configuration.getEndpoints()) {
            endpointClients.add(buildEndpointClient(endpoint, configuration, mapper));
        }
        this.rawTransport = new JdkHttpTransport(configuration);
        this.serverVersion = serverVersion;
    }

    private static EndpointClient buildEndpointClient(String endpoint, ClientConfiguration configuration,
                                                       JsonpMapper mapper) {
        URI endpointUri = URI.create(endpoint);
        HttpHost host = new HttpHost(endpointUri.getHost(), endpointUri.getPort(), endpointUri.getScheme());
        RestClientBuilder restClientBuilder = RestClient.builder(host)
                .setRequestConfigCallback(builder -> builder
                        .setConnectTimeout(configuration.getConnectTimeoutMillis())
                        .setSocketTimeout(configuration.getSocketTimeoutMillis())
                        .setConnectionRequestTimeout(configuration.getConnectionRequestTimeoutMillis()))
                .setHttpClientConfigCallback(builder -> {
                    if (configuration.getSslContext() != null) {
                        builder.setSSLContext(configuration.getSslContext());
                    }
                    if (configuration.getUsername() != null) {
                        BasicCredentialsProvider credentials = new BasicCredentialsProvider();
                        credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                                configuration.getUsername(), configuration.getPassword()));
                        builder.setDefaultCredentialsProvider(credentials);
                    }
                    return builder;
                });
        String pathPrefix = endpointUri.getRawPath();
        if (pathPrefix != null && !pathPrefix.isEmpty() && !"/".equals(pathPrefix)) {
            restClientBuilder.setPathPrefix(pathPrefix);
        }
        if (configuration.getApiKey() != null) {
            restClientBuilder.setDefaultHeaders(new org.apache.http.Header[]{
                    new BasicHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + configuration.getApiKey())
            });
        }
        RestClientTransport transport = new RestClientTransport(restClientBuilder.build(), mapper);
        return new EndpointClient(transport, new ElasticsearchClient(transport));
    }

    @Override
    public String getAdapterId() { return "es8"; }

    @Override
    public EsVersion getServerVersion() { return serverVersion; }

    @Override
    public SearchResult search(SearchRequest searchRequest) throws IOException {
        IOException previousFailure = null;
        int retryCount = 0;
        for (EndpointClient endpointClient : endpointClients) {
            try {
                SearchResponse<JsonData> response = endpointClient.client.search(builder -> builder
                        .index(searchRequest.getIndex())
                        .withJson(new StringReader(searchRequest.getBody())), JsonData.class);
                // Generated response objects prefix diagnostic toString() output with the type name.
                // JsonpUtils serializes only the payload expected by the version-neutral API.
                return new SearchResult(JsonpUtils.toJsonString(response, mapper), retryCount);
            } catch (ElasticsearchException exception) {
                IOException failure = searchFailure(exception, exception.status(), retryCount);
                if (previousFailure != null) failure.addSuppressed(previousFailure);
                if (!isRetryable(exception.status())) throw failure;
                previousFailure = failure;
            } catch (IOException exception) {
                IOException failure = searchFailure(exception, -1, retryCount);
                if (previousFailure != null) failure.addSuppressed(previousFailure);
                previousFailure = failure;
            } catch (RuntimeException exception) {
                IOException failure = searchFailure(exception, -1, retryCount);
                if (previousFailure != null) failure.addSuppressed(previousFailure);
                throw failure;
            }
            retryCount++;
        }
        throw previousFailure == null
                ? new IOException("No Elasticsearch endpoint is configured") : previousFailure;
    }

    @Override
    public ClientResponse perform(ClientRequest clientRequest) throws IOException {
        return rawTransport.perform(clientRequest);
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (EndpointClient endpointClient : endpointClients) {
            try {
                endpointClient.transport.close();
            } catch (IOException exception) {
                if (failure != null) exception.addSuppressed(failure);
                failure = exception;
            }
        }
        if (failure != null) throw failure;
    }

    private static boolean isRetryable(int statusCode) {
        return statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private static IOException searchFailure(Exception exception, int statusCode, int retryCount) {
        String status = statusCode < 0 ? " before receiving an HTTP response" : " with HTTP " + statusCode;
        return new ClientRequestException("Elasticsearch search failed" + status + ": "
                + exception.getMessage(), statusCode, retryCount, exception);
    }

    private static final class EndpointClient {
        private final RestClientTransport transport;
        private final ElasticsearchClient client;

        private EndpointClient(RestClientTransport transport, ElasticsearchClient client) {
            this.transport = transport;
            this.client = client;
        }
    }
}
