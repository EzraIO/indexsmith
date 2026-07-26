package io.github.indexsmith.client;

/** Validates an operation before it is sent to Elasticsearch. */
public interface QueryPolicy {
    void validate(ClientOperation operation) throws QueryPolicyViolationException;
}
