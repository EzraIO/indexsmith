package io.github.indexsmith.api;

import io.github.indexsmith.client.ClientOperation;
import io.github.indexsmith.client.ClientRequest;
import io.github.indexsmith.client.QueryPolicyViolationException;
import io.github.indexsmith.client.SearchRequest;
import org.junit.Test;

import static org.junit.Assert.fail;

public class RestrictedQueryPolicyTest {
    private final RestrictedQueryPolicy policy = RestrictedQueryPolicy.builder()
            .allowIndex("orders")
            .allowMethod("POST")
            .forbidPathPrefix("/_scripts")
            .maxResultWindow(100)
            .build();

    @Test
    public void acceptsAllowedSearchAtWindowBoundary() throws Exception {
        policy.validate(ClientOperation.search(new SearchRequest("orders", "{\"from\":90,\"size\":10}")));
    }

    @Test
    public void rejectsDisallowedIndex() throws Exception {
        expectViolation(ClientOperation.search(new SearchRequest("customers", "{}")));
    }

    @Test
    public void rejectsOversizedWindow() throws Exception {
        expectViolation(ClientOperation.search(new SearchRequest("orders", "{\"from\":91,\"size\":10}")));
        expectViolation(ClientOperation.search(new SearchRequest("orders",
                "{\"from\":9223372036854775807,\"size\":9223372036854775807}")));
    }

    @Test
    public void rejectsForbiddenPathAndMethod() throws Exception {
        expectViolation(ClientOperation.request(ClientRequest.json("POST", "/_scripts/a", "{}")));
        expectViolation(ClientOperation.request(ClientRequest.json("POST", "/_scripts#ignored", "{}")));
        expectViolation(ClientOperation.request(ClientRequest.get("/orders/_search")));
    }

    @Test
    public void rejectsGlobalIndexOperationsWhenIndexCannotBeVerified() throws Exception {
        expectViolation(ClientOperation.request(ClientRequest.ndjson("POST", "/_bulk", "{}\n")));
        expectViolation(ClientOperation.request(ClientRequest.json("POST", "/_search", "{}")));
        expectViolation(ClientOperation.request(ClientRequest.json("POST", "/_reindex", "{}")));
        expectViolation(ClientOperation.request(ClientRequest.json("POST", "/_mget", "{}")));
        expectViolation(ClientOperation.request(ClientRequest.json("POST", "/_field_caps", "{}")));
    }

    @Test
    public void rejectsRequestsWhoseBodyCanOverrideAllowedPathIndex() throws Exception {
        expectViolation(ClientOperation.request(ClientRequest.ndjson("POST", "/orders/_bulk", "{}\n")));
        expectViolation(ClientOperation.request(ClientRequest.ndjson("POST", "/orders/_msearch", "{}\n")));
        expectViolation(ClientOperation.request(ClientRequest.json("POST", "/orders/_mget", "{}")));
    }

    @Test
    public void validatesUrlPaginationTogetherWithBodyPagination() throws Exception {
        policy.validate(ClientOperation.request(ClientRequest.json(
                "POST", "/orders/_search?from=90&size=10", "{}")));
        expectViolation(ClientOperation.request(ClientRequest.json(
                "POST", "/orders/_search?size=11", "{\"from\":90}")));
        expectViolation(ClientOperation.request(ClientRequest.json(
                "POST", "/orders/_search?from=1&from=2", "{}")));
        expectViolation(ClientOperation.request(ClientRequest.json(
                "POST", "/orders/_search?size=invalid", "{}")));
    }

    @Test
    public void rejectsMultiSearchWhenWindowCannotBeVerified() throws Exception {
        RestrictedQueryPolicy windowOnly = RestrictedQueryPolicy.builder().maxResultWindow(100).build();
        try {
            windowOnly.validate(ClientOperation.request(
                    ClientRequest.ndjson("POST", "/orders/_msearch", "{}\n{}\n")));
            fail("Expected QueryPolicyViolationException");
        } catch (QueryPolicyViolationException expected) {
            // expected
        }
    }

    private void expectViolation(ClientOperation operation) throws Exception {
        try {
            policy.validate(operation);
            fail("Expected QueryPolicyViolationException");
        } catch (QueryPolicyViolationException expected) {
            // expected
        }
    }
}
