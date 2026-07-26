package io.github.indexsmith.client;

import java.io.Closeable;
import java.io.IOException;

public interface IndexSmithClient extends Closeable {
    String getAdapterId();
    EsVersion getServerVersion();
    ClientResponse perform(ClientRequest request) throws IOException;
    SearchResult search(SearchRequest request) throws IOException;
}
