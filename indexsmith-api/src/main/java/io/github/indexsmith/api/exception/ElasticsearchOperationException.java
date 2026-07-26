package io.github.indexsmith.api.exception;

import java.io.IOException;

/** Transport, serialization, or response-decoding failure before a usable ES result exists. */
public class ElasticsearchOperationException extends IOException {
    private final String operation;

    public ElasticsearchOperationException(String operation, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
    }

    public String getOperation() { return operation; }
}
