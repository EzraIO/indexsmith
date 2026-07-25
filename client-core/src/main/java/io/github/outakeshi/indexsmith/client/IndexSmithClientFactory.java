package io.github.outakeshi.indexsmith.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public final class IndexSmithClientFactory {
    private final ServerVersionDetector versionDetector;
    private final Iterable<ElasticsearchClientProvider> providers;

    public IndexSmithClientFactory() {
        this(new HttpServerVersionDetector(), ServiceLoader.load(ElasticsearchClientProvider.class));
    }

    public IndexSmithClientFactory(ServerVersionDetector versionDetector,
                                   Iterable<ElasticsearchClientProvider> providers) {
        this.versionDetector = Objects.requireNonNull(versionDetector, "versionDetector");
        this.providers = Objects.requireNonNull(providers, "providers");
    }

    public IndexSmithClient create(ClientConfiguration configuration) throws IOException {
        Objects.requireNonNull(configuration, "configuration");
        EsVersion serverVersion = versionDetector.detect(configuration);
        List<String> available = new ArrayList<>();
        ElasticsearchClientProvider match = null;
        for (ElasticsearchClientProvider provider : providers) {
            if (provider == null) continue;
            available.add(provider.getId());
            boolean requested = configuration.getAdapter() == null
                    || configuration.getAdapter().equalsIgnoreCase(provider.getId());
            if (requested && provider.supports(serverVersion)) {
                if (match != null) {
                    throw new IllegalStateException("Multiple adapters support Elasticsearch " + serverVersion
                            + "; set ClientConfiguration.adapter explicitly");
                }
                match = provider;
            }
        }
        if (match == null) {
            throw new IllegalStateException("No compatible client adapter for Elasticsearch " + serverVersion
                    + ". Requested=" + configuration.getAdapter() + ", available=" + available);
        }
        return new ConfiguredIndexSmithClient(match.create(configuration, serverVersion), configuration);
    }
}
