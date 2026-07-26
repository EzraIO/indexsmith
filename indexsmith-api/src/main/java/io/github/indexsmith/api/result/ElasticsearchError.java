package io.github.indexsmith.api.result;

/** Structured Elasticsearch error returned for an accepted HTTP request. */
public final class ElasticsearchError {
    private final String type;
    private final String reason;

    public ElasticsearchError(String type, String reason) {
        this.type = type;
        this.reason = reason;
    }

    public String getType() { return type; }
    public String getReason() { return reason; }

    @Override
    public String toString() {
        return (type == null ? "elasticsearch_error" : type) + (reason == null ? "" : ": " + reason);
    }
}
