package io.github.indexsmith.client;

public final class SearchResult {
    private final String responseBody;
    private final int retryCount;

    public SearchResult(String responseBody) {
        this(responseBody, 0);
    }

    public SearchResult(String responseBody, int retryCount) {
        if (retryCount < 0) throw new IllegalArgumentException("retryCount must not be negative");
        this.responseBody = responseBody;
        this.retryCount = retryCount;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public int getRetryCount() { return retryCount; }
}
