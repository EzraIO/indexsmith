package io.github.outakeshi.indexsmith.client;

/** Validates an operation before it is sent to Elasticsearch. */
public interface QueryPolicy {
    void validate(ClientOperation operation) throws QueryPolicyViolationException;
}
