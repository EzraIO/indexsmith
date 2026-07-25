package io.github.outakeshi.indexsmith.api;

import io.github.outakeshi.indexsmith.client.ClientRequest;
import io.github.outakeshi.indexsmith.client.ClientResponse;
import io.github.outakeshi.indexsmith.client.IndexSmithClient;
import io.github.outakeshi.indexsmith.client.EsVersion;
import io.github.outakeshi.indexsmith.client.SearchRequest;
import io.github.outakeshi.indexsmith.client.SearchResult;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BaseElasticsearchRepositoryTest {
    private StubClient client;
    private UserRepository repository;

    @Before
    public void setUp() {
        client = new StubClient();
        repository = new UserRepository(client);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyBatchWithoutCallingElasticsearch() throws Exception {
        try {
            repository.batchInsert(Collections.<BaseHit<User>>emptyList());
        } finally {
            assertTrue(client.requests.isEmpty());
        }
    }

    @Test
    public void returnsPerItemBulkResultsAndNdjsonRequest() throws Exception {
        BulkWriteResult result = repository.batchInsert(Arrays.asList(
                hit("1", null, new User("Ada")), hit("2", "archive", new User("Bob"))));

        assertTrue(result.isSuccessful());
        assertEquals(2, result.getSuccessfulCount());
        assertEquals(0, result.getFailedCount());
        assertEquals("users", result.getItems().get(0).getIndex());
        ClientRequest request = client.lastRequest();
        assertEquals("/_bulk", request.getPath());
        assertEquals(ClientRequest.NDJSON, request.getContentType());
        assertTrue(request.getBody().contains("\"_index\":\"archive\""));
    }

    @Test
    public void reportsPartialBulkFailureWithoutDiscardingSuccessfulItems() throws Exception {
        client.forcedResponse = new ClientResponse(200, "{\"took\":4,\"errors\":true,\"items\":["
                + "{\"index\":{\"_index\":\"users\",\"_id\":\"1\",\"status\":201,\"result\":\"created\"}},"
                + "{\"index\":{\"_index\":\"users\",\"_id\":\"2\",\"status\":409,"
                + "\"error\":{\"type\":\"version_conflict_engine_exception\",\"reason\":\"conflict\"}}}]}");

        BulkWriteResult result = repository.batchInsert(Arrays.asList(
                hit("1", null, new User("Ada")), hit("2", null, new User("Bob"))));

        assertFalse(result.isSuccessful());
        assertTrue(result.hasErrors());
        assertEquals(4, result.getTookMillis());
        assertEquals(1, result.getSuccessfulCount());
        assertEquals(1, result.getFailedCount());
        assertEquals("version_conflict_engine_exception", result.getItems().get(1).getError().getType());
    }

    @Test
    public void neverTreatsIncompleteBulkErrorResponseAsSuccess() throws Exception {
        client.forcedResponse = new ClientResponse(200, "{\"took\":1,\"errors\":true,\"items\":[]}");

        BulkWriteResult result = repository.batchInsert(Collections.singletonList(
                hit("1", null, new User("Ada"))));

        assertFalse(result.isSuccessful());
        assertTrue(result.hasErrors());
        assertEquals("incomplete_bulk_response", result.getError().getType());
    }

    @Test
    public void rejectsInvalidWriteInputBeforeCallingElasticsearch() throws Exception {
        try {
            repository.insert(hit("1", null, null));
            fail("Expected missing data to be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("data"));
        }
        assertTrue(client.requests.isEmpty());
    }

    @Test
    public void returnsStructuredIndexDeleteAndHttpFailureResults() throws Exception {
        WriteResult indexed = repository.insert(hit("42", null, new User("Ada")));
        assertTrue(indexed.isSuccessful());
        assertEquals(WriteOperation.INDEX, indexed.getOperation());
        assertEquals("PUT", client.lastRequest().getMethod());

        WriteResult deleted = repository.deleteByPk("42");
        assertTrue(deleted.isSuccessful());
        assertEquals("deleted", deleted.getResult());

        client.forcedResponse = new ClientResponse(409,
                "{\"error\":{\"type\":\"version_conflict_engine_exception\",\"reason\":\"conflict\"}}");
        WriteResult failed = repository.insert(hit("42", null, new User("Ada")));
        assertFalse(failed.isSuccessful());
        assertEquals(409, failed.getStatusCode());
        assertEquals("conflict", failed.getError().getReason());
    }

    @Test
    public void mapsHighlightsSortValuesAndTypedAggregations() throws Exception {
        AggregationSpec average = AggregationSpec.builder("average_price", AggregationType.AVG)
                .field("price").build();
        AggregationSpec categories = AggregationSpec.builder("categories", AggregationType.TERMS)
                .field("status").subAggregation(average).build();
        AggregationSpec ranges = AggregationSpec.builder("price_ranges", AggregationType.RANGE)
                .field("price").range(new AggregationRange("cheap", null, 50)).build();

        PageSearchResponse<BaseHit<User>> page = repository.queryCondition(
                "{\"query\":{\"match_all\":{}}}", Arrays.asList(categories, ranges));

        assertTrue(page.isSucceed());
        assertEquals(1, page.getTotal());
        assertEquals(5, page.getTotalHits());
        BaseHit<User> hit = page.getHits().get(0);
        assertEquals("Ada", hit.getData().name);
        assertEquals("<em>Ada</em>", hit.getHighlight().get("name").get(0));
        assertEquals(100, ((Number) hit.getSortValues().get(0)).intValue());
        AggregationResult result = page.getAggregations().get("categories");
        assertEquals(AggregationType.TERMS, result.getType());
        assertEquals("paid", result.getBuckets().get(0).getKey());
        AggregationResult metric = result.getBuckets().get(0).getAggregations().get("average_price");
        assertEquals(AggregationType.AVG, metric.getType());
        assertEquals(42.5, metric.getValue().doubleValue(), 0.001);
        AggregationBucket range = page.getAggregations().get("price_ranges").getBuckets().get(0);
        assertEquals(50, ((Number) range.getTo()).intValue());
    }

    @Test
    public void propagatesTechnicalFailuresWithOperationContext() throws Exception {
        client.throwTransport = true;
        try {
            repository.insert(hit("42", null, new User("Ada")));
            fail("Expected transport failure");
        } catch (ElasticsearchOperationException exception) {
            assertEquals("index", exception.getOperation());
            assertNotNull(exception.getCause());
        }

        client.throwTransport = false;
        client.forcedResponse = new ClientResponse(201, "not-json");
        try {
            repository.insert(hit("42", null, new User("Ada")));
            fail("Expected decode failure");
        } catch (ElasticsearchOperationException exception) {
            assertTrue(exception.getMessage().contains("decode"));
        }
    }

    private static BaseHit<User> hit(String id, String index, User user) {
        BaseHit<User> hit = new BaseHit<>();
        hit.setId(id);
        hit.setIndex(index);
        hit.setData(user);
        return hit;
    }

    private static final class UserRepository extends BaseElasticsearchRepository<User> {
        private UserRepository(IndexSmithClient client) { super(client, User.class); }
        @Override protected String getIndex() { return "users"; }
    }

    private static final class StubClient implements IndexSmithClient {
        private final List<ClientRequest> requests = new ArrayList<>();
        private SearchRequest lastSearch;
        private ClientResponse forcedResponse;
        private boolean throwTransport;

        @Override public String getAdapterId() { return "stub"; }
        @Override public EsVersion getServerVersion() { return EsVersion.parse("8.17.3"); }

        @Override
        public ClientResponse perform(ClientRequest request) throws IOException {
            requests.add(request);
            if (throwTransport) throw new IOException("connection refused");
            if (forcedResponse != null) {
                ClientResponse response = forcedResponse;
                forcedResponse = null;
                return response;
            }
            if ("/_bulk".equals(request.getPath())) {
                return new ClientResponse(200, "{\"took\":2,\"errors\":false,\"items\":["
                        + "{\"index\":{\"_index\":\"users\",\"_id\":\"1\",\"status\":201,\"result\":\"created\"}},"
                        + "{\"index\":{\"_index\":\"archive\",\"_id\":\"2\",\"status\":201,\"result\":\"created\"}}]}");
            }
            if ("DELETE".equals(request.getMethod())) {
                return new ClientResponse(200, "{\"_index\":\"users\",\"_id\":\"42\",\"result\":\"deleted\"}");
            }
            return new ClientResponse(201, "{\"_index\":\"users\",\"_id\":\"42\",\"result\":\"created\"}");
        }

        @Override
        public SearchResult search(SearchRequest request) {
            lastSearch = request;
            return new SearchResult("{\"timed_out\":false,\"hits\":{"
                    + "\"total\":{\"value\":5,\"relation\":\"eq\"},"
                    + "\"hits\":[{\"_index\":\"users\",\"_id\":\"42\","
                    + "\"_source\":{\"name\":\"Ada\"},\"highlight\":{\"name\":[\"<em>Ada</em>\"]},"
                    + "\"sort\":[100,\"42\"]}]},"
                    + "\"aggregations\":{\"sterms#categories\":{\"buckets\":[{\"key\":\"paid\",\"doc_count\":3,"
                    + "\"avg#average_price\":{\"value\":42.5}}]},"
                    + "\"range#price_ranges\":{\"buckets\":[{\"key\":\"cheap\",\"to\":50.0,\"doc_count\":2}]}}}");
        }

        private ClientRequest lastRequest() { return requests.get(requests.size() - 1); }
        @Override public void close() throws IOException { }
    }

    public static class User {
        public String name;
        public User() { }
        public User(String name) { this.name = name; }
    }
}
