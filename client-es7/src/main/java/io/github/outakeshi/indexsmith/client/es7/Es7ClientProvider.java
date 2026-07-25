package io.github.outakeshi.indexsmith.client.es7;

import io.github.outakeshi.indexsmith.client.ClientConfiguration;
import io.github.outakeshi.indexsmith.client.ElasticsearchClientProvider;
import io.github.outakeshi.indexsmith.client.EsVersion;
import io.github.outakeshi.indexsmith.client.IndexSmithClient;

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
