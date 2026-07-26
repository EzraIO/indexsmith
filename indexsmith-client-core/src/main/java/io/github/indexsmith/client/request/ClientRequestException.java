package io.github.indexsmith.client.request;

import java.io.IOException;

/** Transport failure with the HTTP status and retry count retained for diagnostics. */
public final class ClientRequestException extends IOException {
    private final int statusCode;
    private final int retryCount;

    public ClientRequestException(String message, int statusCode, int retryCount, Throwable cause) {
        super(message, cause);
        if (retryCount < 0) throw new IllegalArgumentException("retryCount must not be negative");
        this.statusCode = statusCode;
        this.retryCount = retryCount;
    }

    /** Returns -1 when no HTTP response was received. */
    public int getStatusCode() { return statusCode; }

    public int getRetryCount() { return retryCount; }
}
