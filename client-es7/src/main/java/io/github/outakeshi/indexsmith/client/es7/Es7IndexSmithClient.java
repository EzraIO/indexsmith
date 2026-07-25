package io.github.outakeshi.indexsmith.client.es7;

import io.github.outakeshi.indexsmith.client.ClientConfiguration;
import io.github.outakeshi.indexsmith.client.ClientRequest;
import io.github.outakeshi.indexsmith.client.ClientRequestException;
import io.github.outakeshi.indexsmith.client.ClientResponse;
import io.github.outakeshi.indexsmith.client.EsVersion;
import io.github.outakeshi.indexsmith.client.IndexSmithClient;
import io.github.outakeshi.indexsmith.client.JdkHttpTransport;
import io.github.outakeshi.indexsmith.client.SearchRequest;
import io.github.outakeshi.indexsmith.client.SearchResult;

import java.io.IOException;

final class Es7IndexSmithClient implements IndexSmithClient {
    private final JdkHttpTransport transport;
    private final EsVersion serverVersion;

    Es7IndexSmithClient(ClientConfiguration configuration, EsVersion serverVersion) {
        this.transport = new JdkHttpTransport(configuration);
        this.serverVersion = serverVersion;
    }

    @Override
    public String getAdapterId() { return "es7"; }

    @Override
    public EsVersion getServerVersion() { return serverVersion; }

    @Override
    public ClientResponse perform(ClientRequest request) throws IOException {
        return transport.perform(request);
    }

    @Override
    public SearchResult search(SearchRequest searchRequest) throws IOException {
        ClientResponse response = perform(ClientRequest.json(
                "POST", "/" + searchRequest.getIndex() + "/_search", searchRequest.getBody())
                .withRetryable(true));
        if (!response.isSuccessful()) {
            throw new ClientRequestException("Elasticsearch search failed with HTTP " + response.getStatusCode()
                    + ": " + response.getBody(), response.getStatusCode(), response.getRetryCount(), null);
        }
        return new SearchResult(response.getBody(), response.getRetryCount());
    }

    @Override
    public void close() { }
}
