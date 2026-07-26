package io.github.indexsmith.client.policy;

import java.io.IOException;

public final class QueryPolicyViolationException extends IOException {
    public QueryPolicyViolationException(String message) {
        super(message);
    }

    public QueryPolicyViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
