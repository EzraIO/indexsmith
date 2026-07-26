package io.github.indexsmith.api.result;

/** Result of one index or delete operation. */
public final class WriteResult {
    private final WriteOperation operation;
    private final String index;
    private final String id;
    private final int statusCode;
    private final String result;
    private final ElasticsearchError error;
    private final String responseBody;

    public WriteResult(WriteOperation operation, String index, String id, int statusCode,
                       String result, ElasticsearchError error, String responseBody) {
        this.operation = operation;
        this.index = index;
        this.id = id;
        this.statusCode = statusCode;
        this.result = result;
        this.error = error;
        this.responseBody = responseBody;
    }

    public WriteOperation getOperation() { return operation; }
    public String getIndex() { return index; }
    public String getId() { return id; }
    public int getStatusCode() { return statusCode; }
    public String getResult() { return result; }
    public ElasticsearchError getError() { return error; }
    public String getResponseBody() { return responseBody; }
    public boolean isSuccessful() { return statusCode >= 200 && statusCode < 300 && error == null; }
}
