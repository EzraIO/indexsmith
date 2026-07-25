package io.github.outakeshi.indexsmith.client;

/** Redacted request lifecycle event. It never exposes credentials or payloads. */
public final class RequestEvent {
    public enum Phase { START, SUCCESS, FAILURE }

    private final Phase phase;
    private final String adapterId;
    private final ClientOperation.Type operationType;
    private final String method;
    private final String path;
    private final int statusCode;
    private final long durationMillis;
    private final int retryCount;
    private final String failureType;

    RequestEvent(Phase phase, String adapterId, ClientOperation operation, int statusCode,
                 long durationMillis, int retryCount, String failureType) {
        this.phase = phase;
        this.adapterId = adapterId;
        this.operationType = operation.getType();
        this.method = operation.getMethod();
        this.path = redactedPath(operation.getPath());
        this.statusCode = statusCode;
        this.durationMillis = durationMillis;
        this.retryCount = retryCount;
        this.failureType = failureType;
    }

    public Phase getPhase() { return phase; }
    public String getAdapterId() { return adapterId; }
    public ClientOperation.Type getOperationType() { return operationType; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public int getStatusCode() { return statusCode; }
    public long getDurationMillis() { return durationMillis; }
    public int getRetryCount() { return retryCount; }
    public String getFailureType() { return failureType; }

    private static String redactedPath(String path) {
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        int end = query < 0 ? path.length() : query;
        if (fragment >= 0 && fragment < end) end = fragment;
        return path.substring(0, end);
    }
}
