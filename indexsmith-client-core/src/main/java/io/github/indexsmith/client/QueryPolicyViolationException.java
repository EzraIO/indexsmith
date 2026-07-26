package io.github.indexsmith.client;

import java.io.IOException;

public final class QueryPolicyViolationException extends IOException {
    public QueryPolicyViolationException(String message) {
        super(message);
    }

    public QueryPolicyViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
