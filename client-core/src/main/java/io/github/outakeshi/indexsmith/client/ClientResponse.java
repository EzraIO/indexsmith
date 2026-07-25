package io.github.outakeshi.indexsmith.client;

public final class ClientResponse {
    private final int statusCode;
    private final String body;
    private final int retryCount;

    public ClientResponse(int statusCode, String body) {
        this(statusCode, body, 0);
    }

    public ClientResponse(int statusCode, String body, int retryCount) {
        if (retryCount < 0) throw new IllegalArgumentException("retryCount must not be negative");
        this.statusCode = statusCode;
        this.body = body;
        this.retryCount = retryCount;
    }

    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public int getRetryCount() { return retryCount; }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
