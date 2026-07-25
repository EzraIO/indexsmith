package io.github.outakeshi.indexsmith.client;

import java.util.Locale;

public final class ClientRequest {
    public static final String JSON = "application/json";
    public static final String NDJSON = "application/x-ndjson";

    private final String method;
    private final String path;
    private final String body;
    private final String contentType;
    private final boolean retryable;

    public ClientRequest(String method, String path, String body, String contentType) {
        this(method, path, body, contentType, isSafeMethod(method));
    }

    public ClientRequest(String method, String path, String body, String contentType, boolean retryable) {
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP method must not be blank");
        }
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Request path must not be blank");
        }
        this.method = method.trim().toUpperCase(Locale.ROOT);
        this.path = path.startsWith("/") ? path : "/" + path;
        this.body = body;
        this.contentType = contentType == null || contentType.trim().isEmpty() ? JSON : contentType;
        this.retryable = retryable;
    }

    public static ClientRequest json(String method, String path, String body) {
        return new ClientRequest(method, path, body, JSON);
    }

    public static ClientRequest ndjson(String method, String path, String body) {
        return new ClientRequest(method, path, body, NDJSON);
    }

    public static ClientRequest get(String path) {
        return new ClientRequest("GET", path, null, JSON);
    }

    public static ClientRequest delete(String path) {
        return new ClientRequest("DELETE", path, null, JSON);
    }

    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getBody() { return body; }
    public String getContentType() { return contentType; }
    public boolean isRetryable() { return retryable; }

    public ClientRequest withRetryable(boolean value) {
        return new ClientRequest(method, path, body, contentType, value);
    }

    private static boolean isSafeMethod(String method) {
        if (method == null) return false;
        String value = method.trim().toUpperCase(Locale.ROOT);
        return "GET".equals(value) || "HEAD".equals(value) || "OPTIONS".equals(value)
                || "PUT".equals(value) || "DELETE".equals(value);
    }
}
