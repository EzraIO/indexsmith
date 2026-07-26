package io.github.indexsmith.client;

import java.io.IOException;

public interface ElasticsearchClientProvider {
    String getId();
    boolean supports(EsVersion serverVersion);
    IndexSmithClient create(ClientConfiguration configuration, EsVersion serverVersion) throws IOException;
}
