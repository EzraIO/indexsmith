package io.github.indexsmith.client;

import io.github.indexsmith.client.observation.*;
import io.github.indexsmith.client.policy.*;
import io.github.indexsmith.client.request.*;
import io.github.indexsmith.client.response.*;
import io.github.indexsmith.client.spi.*;
import io.github.indexsmith.client.transport.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SearchModelsTest {
    @Test
    public void keepsIndexAndRequestBody() {
        SearchRequest request = new SearchRequest("products", "{\"query\":{\"match_all\":{}}}");

        assertEquals("products", request.getIndex());
        assertEquals("{\"query\":{\"match_all\":{}}}", request.getBody());
    }

    @Test
    public void defaultsMissingBodyToEmptyJsonObject() {
        assertEquals("{}", new SearchRequest("products", null).getBody());
        assertEquals("{}", new SearchRequest("products", "  ").getBody());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankIndex() {
        new SearchRequest("  ", "{}");
    }

    @Test
    public void exposesRawResponseBody() {
        assertEquals("{\"hits\":{}}", new SearchResult("{\"hits\":{}}").getResponseBody());
        assertNull(new SearchResult(null).getResponseBody());
    }

    @Test
    public void normalizesGenericRequestsAndClassifiesResponses() {
        ClientRequest request = ClientRequest.ndjson("post", "_bulk", "{}\n");
        assertEquals("POST", request.getMethod());
        assertEquals("/_bulk", request.getPath());
        assertEquals(ClientRequest.NDJSON, request.getContentType());

        assertTrue(new ClientResponse(201, "{}").isSuccessful());
        assertFalse(new ClientResponse(404, "{}").isSuccessful());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsGenericRequestWithoutMethod() {
        new ClientRequest(" ", "/_cluster/health", null, null);
    }
}
