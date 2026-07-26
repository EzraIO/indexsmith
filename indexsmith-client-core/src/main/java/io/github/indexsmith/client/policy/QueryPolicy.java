package io.github.indexsmith.client.policy;

import io.github.indexsmith.client.request.ClientOperation;

/** Validates an operation before it is sent to Elasticsearch. */
public interface QueryPolicy {
    void validate(ClientOperation operation) throws QueryPolicyViolationException;
}
