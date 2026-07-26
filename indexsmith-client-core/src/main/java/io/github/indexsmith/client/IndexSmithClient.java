package io.github.indexsmith.client;

import io.github.indexsmith.client.request.ClientRequest;
import io.github.indexsmith.client.request.SearchRequest;
import io.github.indexsmith.client.response.ClientResponse;
import io.github.indexsmith.client.response.SearchResult;

import java.io.Closeable;
import java.io.IOException;

public interface IndexSmithClient extends Closeable {
    String getAdapterId();
    EsVersion getServerVersion();
    ClientResponse perform(ClientRequest request) throws IOException;
    SearchResult search(SearchRequest request) throws IOException;
}
