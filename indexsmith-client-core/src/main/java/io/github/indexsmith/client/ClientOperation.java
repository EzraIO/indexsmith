package io.github.indexsmith.client;

import java.util.Objects;

/** A request description exposed to query policies; listeners receive a redacted event instead. */
public final class ClientOperation {
    public enum Type { REQUEST, SEARCH }

    private final Type type;
    private final String method;
    private final String path;
    private final String index;
    private final String body;

    private ClientOperation(Type type, String method, String path, String index, String body) {
        this.type = type;
        this.method = method;
        this.path = path;
        this.index = index;
        this.body = body;
    }

    public static ClientOperation request(ClientRequest request) {
        Objects.requireNonNull(request, "request");
        return new ClientOperation(Type.REQUEST, request.getMethod(), request.getPath(), null, request.getBody());
    }

    public static ClientOperation search(SearchRequest request) {
        Objects.requireNonNull(request, "request");
        return new ClientOperation(Type.SEARCH, "POST", "/" + request.getIndex() + "/_search",
                request.getIndex(), request.getBody());
    }

    public Type getType() { return type; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getIndex() { return index; }
    public String getBody() { return body; }
}
