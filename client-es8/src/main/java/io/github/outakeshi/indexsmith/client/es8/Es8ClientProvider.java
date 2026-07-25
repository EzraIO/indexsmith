package io.github.outakeshi.indexsmith.client.es8;

import io.github.outakeshi.indexsmith.client.ClientConfiguration;
import io.github.outakeshi.indexsmith.client.ElasticsearchClientProvider;
import io.github.outakeshi.indexsmith.client.EsVersion;
import io.github.outakeshi.indexsmith.client.IndexSmithClient;

public final class Es8ClientProvider implements ElasticsearchClientProvider {
    @Override
    public String getId() { return "es8"; }

    @Override
    public boolean supports(EsVersion serverVersion) {
        // The official client guarantees forward compatibility, not arbitrary minor-version rollback.
        return serverVersion.getMajor() == 8 && serverVersion.getMinor() >= 17;
    }

    @Override
    public IndexSmithClient create(ClientConfiguration configuration, EsVersion serverVersion) {
        return new Es8IndexSmithClient(configuration, serverVersion);
    }
}
