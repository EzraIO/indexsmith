package io.github.indexsmith.api.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Top-level and per-item result of an Elasticsearch bulk request. */
public final class BulkWriteResult {
    private final int statusCode;
    private final long tookMillis;
    private final List<WriteResult> items;
    private final boolean errors;
    private final ElasticsearchError error;
    private final String responseBody;

    public BulkWriteResult(int statusCode, long tookMillis, List<WriteResult> items,
                           ElasticsearchError error, String responseBody) {
        this(statusCode, tookMillis, items, error != null || containsFailedItem(items), error, responseBody);
    }

    public BulkWriteResult(int statusCode, long tookMillis, List<WriteResult> items,
                           boolean errors, ElasticsearchError error, String responseBody) {
        this.statusCode = statusCode;
        this.tookMillis = tookMillis;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.errors = errors;
        this.error = error;
        this.responseBody = responseBody;
    }

    public int getStatusCode() { return statusCode; }
    public long getTookMillis() { return tookMillis; }
    public List<WriteResult> getItems() { return items; }
    public boolean hasErrors() { return errors; }
    public ElasticsearchError getError() { return error; }
    public String getResponseBody() { return responseBody; }

    public long getSuccessfulCount() {
        return items.stream().filter(WriteResult::isSuccessful).count();
    }

    public long getFailedCount() { return items.size() - getSuccessfulCount(); }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300 && !errors && error == null && getFailedCount() == 0;
    }

    private static boolean containsFailedItem(List<WriteResult> items) {
        if (items == null) return false;
        for (WriteResult item : items) {
            if (item == null || !item.isSuccessful()) return true;
        }
        return false;
    }
}
