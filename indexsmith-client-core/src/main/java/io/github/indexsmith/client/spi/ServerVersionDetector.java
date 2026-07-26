package io.github.indexsmith.client.spi;

import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.EsVersion;

import java.io.IOException;

public interface ServerVersionDetector {
    EsVersion detect(ClientConfiguration configuration) throws IOException;
}
