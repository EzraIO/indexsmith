package io.github.outakeshi.indexsmith.client;

public final class SearchRequest {
    private final String index;
    private final String body;

    public SearchRequest(String index, String body) {
        if (index == null || index.trim().isEmpty()) {
            throw new IllegalArgumentException("index must not be blank");
        }
        this.index = index;
        this.body = body == null || body.trim().isEmpty() ? "{}" : body;
    }

    public String getIndex() { return index; }
    public String getBody() { return body; }
}
