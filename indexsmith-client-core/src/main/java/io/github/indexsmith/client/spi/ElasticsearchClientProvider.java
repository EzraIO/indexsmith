package io.github.indexsmith.client.spi;

import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.EsVersion;
import io.github.indexsmith.client.IndexSmithClient;

import java.io.IOException;

public interface ElasticsearchClientProvider {
    String getId();
    boolean supports(EsVersion serverVersion);
    IndexSmithClient create(ClientConfiguration configuration, EsVersion serverVersion) throws IOException;
}
