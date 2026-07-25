package io.github.outakeshi.indexsmith.client;

import java.io.IOException;

public interface ServerVersionDetector {
    EsVersion detect(ClientConfiguration configuration) throws IOException;
}
