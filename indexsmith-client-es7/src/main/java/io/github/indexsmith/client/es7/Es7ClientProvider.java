package io.github.indexsmith.client.es7;

import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.spi.ElasticsearchClientProvider;
import io.github.indexsmith.client.EsVersion;
import io.github.indexsmith.client.IndexSmithClient;

public final class Es7ClientProvider implements ElasticsearchClientProvider {
    @Override
    public String getId() { return "es7"; }

    @Override
    public boolean supports(EsVersion serverVersion) {
        return serverVersion.getMajor() == 7 && serverVersion.getMinor() >= 11;
    }

    @Override
    public IndexSmithClient create(ClientConfiguration configuration, EsVersion serverVersion) {
        return new Es7IndexSmithClient(configuration, serverVersion);
    }
}
