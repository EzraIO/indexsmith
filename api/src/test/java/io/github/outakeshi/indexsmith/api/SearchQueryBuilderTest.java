package io.github.outakeshi.indexsmith.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SearchQueryBuilderTest {
    @Test
    public void buildsMatchAllWhenNoConditionsExist() throws Exception {
        JsonNode query = JsonUtil.readTree(SearchQueryBuilder.create().build());
        assertTrue(query.path("query").has("match_all"));
        assertFalse(query.has("size"));
    }

    @Test
    public void buildsLegacyConditionsAndGroupsNestedConditionsByPath() throws Exception {
        JsonNode query = JsonUtil.readTree(SearchQueryBuilder.create()
                .mustTerm("status", "paid")
                .mustMatchPhrase("description", "red bike")
                .mustNestedTerm("items", "items.skuId", 42L)
                .mustNestedMatchPhrase("items", "items.name", "bike")
                .mustTerm("ignored", null)
                .build());

        JsonNode must = query.path("query").path("bool").path("must");
        assertEquals(3, must.size());
        assertEquals("paid", must.get(0).path("term").path("status").asText());
        JsonNode nested = must.get(2).path("nested");
        assertEquals("items", nested.path("path").asText());
        assertEquals(2, nested.path("query").path("bool").path("must").size());
    }

    @Test
    public void buildsAllBooleanClausesAndCommonOperators() throws Exception {
        JsonNode query = JsonUtil.readTree(SearchQueryBuilder.create()
                .condition(QueryCondition.builder(QueryOperator.MATCH, "name").value("bike").build())
                .condition(QueryCondition.builder(QueryOperator.TERMS, "category")
                        .clause(BoolClause.FILTER).values(Arrays.asList("road", "city")).build())
                .condition(QueryCondition.builder(QueryOperator.RANGE, "price")
                        .clause(BoolClause.FILTER)
                        .range(RangeBounds.builder().gte(10).lt(100).build()).build())
                .condition(QueryCondition.builder(QueryOperator.EXISTS, "deletedAt")
                        .clause(BoolClause.MUST_NOT).build())
                .condition(QueryCondition.builder(QueryOperator.PREFIX, "sku").clause(BoolClause.SHOULD)
                        .value("BI").build())
                .condition(QueryCondition.builder(QueryOperator.WILDCARD, "code").clause(BoolClause.SHOULD)
                        .value("*-2026").build())
                .minimumShouldMatch(1)
                .build());

        JsonNode bool = query.path("query").path("bool");
        assertEquals("bike", bool.path("must").get(0).path("match").path("name").asText());
        assertEquals(2, bool.path("filter").size());
        assertEquals(10, bool.path("filter").get(1).path("range").path("price").path("gte").asInt());
        assertEquals("deletedAt", bool.path("must_not").get(0).path("exists").path("field").asText());
        assertEquals(2, bool.path("should").size());
        assertEquals(1, bool.path("minimum_should_match").asInt());
    }

    @Test
    public void appliesSearchAfterPresentationAndAggregations() throws Exception {
        AggregationSpec average = AggregationSpec.builder("average_price", AggregationType.AVG)
                .field("price").build();
        AggregationSpec categories = AggregationSpec.builder("categories", AggregationType.TERMS)
                .field("category").size(5).subAggregation(average).build();
        AggregationSpec ranges = AggregationSpec.builder("price_ranges", AggregationType.RANGE)
                .field("price")
                .range(new AggregationRange("cheap", null, 50))
                .range(new AggregationRange("premium", 50, null)).build();
        AggregationSpec histogram = AggregationSpec.builder("by_day", AggregationType.DATE_HISTOGRAM)
                .field("createdAt").calendarInterval("day").build();
        AggregationSpec custom = AggregationSpec.builder("active_only", AggregationType.CUSTOM)
                .customBody(Collections.<String, Object>singletonMap("filter",
                        Collections.singletonMap("term", Collections.singletonMap("active", true))))
                .build();

        JsonNode query = JsonUtil.readTree(SearchQueryBuilder.create()
                .page(3, 25)
                .sort("legacy", true)
                .sort(new SortOption("createdAt", SortDirection.DESC))
                .sort(new SortOption("_id", SortDirection.ASC))
                .searchAfter(Arrays.<Object>asList("2026-07-22T00:00:00Z", "42"))
                .sourceFilter(new SourceFilter(Arrays.asList("name", "price"), Collections.singletonList("secret")))
                .highlight(HighlightSpec.builder().field("name").tags("<em>", "</em>")
                        .fragmentSize(100).numberOfFragments(2).build())
                .aggregation(categories).aggregation(ranges).aggregation(histogram).aggregation(custom)
                .build());

        assertFalse(query.has("from"));
        assertEquals(25, query.path("size").asInt());
        assertEquals(2, query.path("sort").size());
        assertEquals(2, query.path("search_after").size());
        assertEquals("secret", query.path("_source").path("excludes").get(0).asText());
        assertTrue(query.path("highlight").path("fields").has("name"));
        assertEquals(5, query.path("aggs").path("categories").path("terms").path("size").asInt());
        assertTrue(query.path("aggs").path("categories").path("aggs").has("average_price"));
        assertEquals(2, query.path("aggs").path("price_ranges").path("range").path("ranges").size());
        assertEquals("day", query.path("aggs").path("by_day").path("date_histogram")
                .path("calendar_interval").asText());
        assertTrue(query.path("aggs").path("active_only").has("filter"));
    }

    @Test
    public void appliesOneBasedPaginationAndLegacySortDirection() throws Exception {
        JsonNode query = JsonUtil.readTree(SearchQueryBuilder.create()
                .page(3, 25)
                .sort("createdAt", true)
                .build());
        assertEquals(50, query.path("from").asInt());
        assertEquals(25, query.path("size").asInt());
        assertEquals("desc", query.path("sort").get(0).path("createdAt").path("order").asText());
    }

    @Test
    public void keepsNestedShouldConditionsDistinctForMinimumShouldMatch() throws Exception {
        JsonNode query = JsonUtil.readTree(SearchQueryBuilder.create()
                .condition(QueryCondition.builder(QueryOperator.TERM, "items.sku")
                        .nestedPath("items").clause(BoolClause.SHOULD).value("A").build())
                .condition(QueryCondition.builder(QueryOperator.RANGE, "items.price")
                        .nestedPath("items").clause(BoolClause.SHOULD)
                        .range(RangeBounds.builder().gte(10).build()).build())
                .minimumShouldMatch(2)
                .build());

        JsonNode should = query.path("query").path("bool").path("should");
        assertEquals(2, should.size());
        assertTrue(should.get(0).has("nested"));
        assertTrue(should.get(1).has("nested"));
    }

    @Test
    public void validatesAmbiguousAndIncompleteInput() throws Exception {
        assertFails(() -> RangeBounds.builder().build(), IllegalArgumentException.class);
        assertFails(() -> RangeBounds.builder().gt(1).gte(2).build(), IllegalArgumentException.class);
        assertFails(() -> QueryCondition.builder(QueryOperator.TERMS, "status").build(), IllegalArgumentException.class);
        assertFails(() -> QueryCondition.builder(QueryOperator.TERMS, "status")
                .values(Arrays.asList("paid", null)).build(), IllegalArgumentException.class);
        assertFails(() -> HighlightSpec.builder().field("name").tags("<b>", "</b>")
                .tags("<i>", null).build(), IllegalArgumentException.class);
        assertFails(() -> SearchQueryBuilder.create().searchAfter(Collections.singletonList("42")).build(),
                IllegalStateException.class);
        assertFails(() -> SearchQueryBuilder.create().sort("id", false)
                .searchAfter(Arrays.asList("42", "extra")).build(), IllegalStateException.class);
        assertFails(() -> SearchQueryBuilder.create().minimumShouldMatch(1).build(), IllegalStateException.class);
        assertFails(() -> SearchQueryBuilder.create().page(Integer.MAX_VALUE, 2), IllegalArgumentException.class);

        AggregationSpec duplicate = AggregationSpec.builder("same", AggregationType.AVG).field("price").build();
        assertFails(() -> SearchQueryBuilder.create().aggregation(duplicate).aggregation(duplicate).build(),
                IllegalArgumentException.class);
        assertFails(() -> AggregationSpec.builder("invalid", AggregationType.AVG)
                .field("price").size(10).build(), IllegalArgumentException.class);
        assertFails(() -> AggregationSpec.builder("invalid", AggregationType.AVG)
                .field("price").subAggregation(duplicate).build(), IllegalArgumentException.class);
        assertFails(() -> AggregationSpec.builder("invalid", AggregationType.CUSTOM)
                .size(10).customBody(Collections.singletonMap("filter", Collections.emptyMap())).build(),
                IllegalArgumentException.class);
        assertFails(() -> AggregationSpec.builder("invalid", AggregationType.TERMS)
                .field("status").subAggregation(duplicate).subAggregation(duplicate).build(),
                IllegalArgumentException.class);
    }

    private static void assertFails(ThrowingRunnable runnable, Class<? extends Throwable> expected) {
        try {
            runnable.run();
            fail("Expected " + expected.getSimpleName());
        } catch (Throwable throwable) {
            assertTrue("Unexpected " + throwable, expected.isInstance(throwable));
        }
    }

    private interface ThrowingRunnable { void run() throws Exception; }
}
