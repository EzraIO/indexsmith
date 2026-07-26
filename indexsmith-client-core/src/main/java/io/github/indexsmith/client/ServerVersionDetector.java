package io.github.indexsmith.client;

import java.io.IOException;

public interface ServerVersionDetector {
    EsVersion detect(ClientConfiguration configuration) throws IOException;
}
