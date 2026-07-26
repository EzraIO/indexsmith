package io.github.indexsmith.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds version-neutral Elasticsearch JSON DSL for hand-written and generated repositories. */
public final class SearchQueryBuilder {
    private final List<QueryCondition> conditions = new ArrayList<>();
    private final List<SortOption> sorts = new ArrayList<>();
    private final List<Object> searchAfter = new ArrayList<>();
    private final List<AggregationSpec> aggregations = new ArrayList<>();
    private Integer from;
    private Integer size;
    private String legacySortField;
    private boolean legacySortDescending;
    private Integer minimumShouldMatch;
    private SourceFilter sourceFilter;
    private HighlightSpec highlight;

    private SearchQueryBuilder() { }

    public static SearchQueryBuilder create() { return new SearchQueryBuilder(); }

    public SearchQueryBuilder mustTerm(String field, Object value) {
        return addLegacy(null, QueryOperator.TERM, field, value);
    }

    public SearchQueryBuilder mustMatchPhrase(String field, Object value) {
        return addLegacy(null, QueryOperator.MATCH_PHRASE, field, value);
    }

    public SearchQueryBuilder mustNestedTerm(String path, String field, Object value) {
        return addLegacy(path, QueryOperator.TERM, field, value);
    }

    public SearchQueryBuilder mustNestedMatchPhrase(String path, String field, Object value) {
        return addLegacy(path, QueryOperator.MATCH_PHRASE, field, value);
    }

    public SearchQueryBuilder condition(QueryCondition condition) {
        conditions.add(Objects.requireNonNull(condition, "condition"));
        return this;
    }

    public SearchQueryBuilder conditions(Collection<QueryCondition> values) {
        if (values != null) {
            for (QueryCondition value : values) condition(value);
        }
        return this;
    }

    public SearchQueryBuilder page(int pageNo, int pageSize) {
        if (pageNo <= 0) throw new IllegalArgumentException("pageNo must be positive");
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize must be positive");
        long offset = (long) (pageNo - 1) * pageSize;
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page offset exceeds Elasticsearch's integer limit; use searchAfter");
        }
        this.from = (int) offset;
        this.size = pageSize;
        return this;
    }

    /** Legacy single-field sort; ignored when one or more structured sorts are present. */
    public SearchQueryBuilder sort(String field, boolean descending) {
        if (!isAbsent(field)) {
            this.legacySortField = field.trim();
            this.legacySortDescending = descending;
        }
        return this;
    }

    public SearchQueryBuilder sort(SortOption value) {
        sorts.add(Objects.requireNonNull(value, "sort"));
        return this;
    }

    public SearchQueryBuilder sorts(Collection<SortOption> values) {
        if (values != null) {
            for (SortOption value : values) sort(value);
        }
        return this;
    }

    public SearchQueryBuilder searchAfter(Collection<?> values) {
        searchAfter.clear();
        if (values != null) searchAfter.addAll(values);
        return this;
    }

    public SearchQueryBuilder minimumShouldMatch(Integer value) {
        if (value != null && value < 0) throw new IllegalArgumentException("minimumShouldMatch must not be negative");
        this.minimumShouldMatch = value;
        return this;
    }

    public SearchQueryBuilder sourceFilter(SourceFilter value) { this.sourceFilter = value; return this; }
    public SearchQueryBuilder highlight(HighlightSpec value) { this.highlight = value; return this; }

    public SearchQueryBuilder aggregation(AggregationSpec value) {
        aggregations.add(Objects.requireNonNull(value, "aggregation"));
        return this;
    }

    public SearchQueryBuilder aggregations(Collection<AggregationSpec> values) {
        if (values != null) {
            for (AggregationSpec value : values) aggregation(value);
        }
        return this;
    }

    public String build() throws IOException {
        int sortCount = sorts.isEmpty() ? legacySortField == null ? 0 : 1 : sorts.size();
        if (!searchAfter.isEmpty() && searchAfter.size() != sortCount) {
            throw new IllegalStateException("searchAfter values must match the number of sort fields");
        }
        if (minimumShouldMatch != null && minimumShouldMatch > 0 && !hasShouldCondition()) {
            throw new IllegalStateException("minimumShouldMatch requires at least one SHOULD condition");
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("query", buildQuery());
        if (searchAfter.isEmpty() && from != null) root.put("from", from);
        if (size != null) root.put("size", size);
        if (!searchAfter.isEmpty()) root.put("search_after", new ArrayList<>(searchAfter));
        addSort(root);
        if (sourceFilter != null) root.put("_source", buildSourceFilter(sourceFilter));
        if (highlight != null) root.put("highlight", buildHighlight(highlight));
        if (!aggregations.isEmpty()) root.put("aggs", buildAggregations(aggregations));
        return JsonUtil.toJson(root);
    }

    private Map<String, Object> buildQuery() {
        if (conditions.isEmpty()) {
            return Collections.<String, Object>singletonMap("match_all", Collections.emptyMap());
        }

        Map<BoolClause, List<Object>> clauses = new EnumMap<>(BoolClause.class);
        Map<BoolClause, Map<String, List<QueryCondition>>> nested = new EnumMap<>(BoolClause.class);
        for (BoolClause clause : BoolClause.values()) {
            clauses.put(clause, new ArrayList<>());
            nested.put(clause, new LinkedHashMap<>());
        }
        for (QueryCondition condition : conditions) {
            if (condition.getNestedPath() == null) {
                clauses.get(condition.getClause()).add(buildLeaf(condition));
            } else if (condition.getClause() == BoolClause.SHOULD) {
                // Keep each SHOULD as a distinct parent-level clause so minimum_should_match
                // continues to count user conditions instead of nested paths.
                clauses.get(BoolClause.SHOULD).add(buildNested(condition.getNestedPath(),
                        BoolClause.SHOULD, Collections.singletonList(condition)));
            } else {
                Map<String, List<QueryCondition>> byPath = nested.get(condition.getClause());
                byPath.computeIfAbsent(condition.getNestedPath(), key -> new ArrayList<>()).add(condition);
            }
        }
        for (BoolClause clause : BoolClause.values()) {
            for (Map.Entry<String, List<QueryCondition>> entry : nested.get(clause).entrySet()) {
                clauses.get(clause).add(buildNested(entry.getKey(), clause, entry.getValue()));
            }
        }

        Map<String, Object> bool = new LinkedHashMap<>();
        for (BoolClause clause : BoolClause.values()) {
            if (!clauses.get(clause).isEmpty()) bool.put(clause.getJsonName(), clauses.get(clause));
        }
        if (minimumShouldMatch != null) bool.put("minimum_should_match", minimumShouldMatch);
        return Collections.<String, Object>singletonMap("bool", bool);
    }

    private static Object buildNested(String path, BoolClause outerClause, List<QueryCondition> values) {
        List<Object> leaves = new ArrayList<>();
        for (QueryCondition value : values) leaves.add(buildLeaf(value));
        Map<String, Object> innerBool = new LinkedHashMap<>();
        if (outerClause == BoolClause.MUST_NOT || outerClause == BoolClause.SHOULD) {
            innerBool.put("should", leaves);
            innerBool.put("minimum_should_match", 1);
        } else {
            innerBool.put(outerClause.getJsonName(), leaves);
        }
        Map<String, Object> nestedBody = new LinkedHashMap<>();
        nestedBody.put("path", path);
        nestedBody.put("query", Collections.singletonMap("bool", innerBool));
        return Collections.singletonMap("nested", nestedBody);
    }

    private static Object buildLeaf(QueryCondition condition) {
        Map<String, Object> body;
        switch (condition.getOperator()) {
            case EXISTS:
                body = Collections.<String, Object>singletonMap("field", condition.getField());
                return Collections.singletonMap(condition.getOperator().getJsonName(), body);
            case TERMS:
                body = Collections.<String, Object>singletonMap(condition.getField(), condition.getValues());
                return Collections.singletonMap(condition.getOperator().getJsonName(), body);
            case RANGE:
                body = Collections.<String, Object>singletonMap(condition.getField(), condition.getRange().toMap());
                return Collections.singletonMap(condition.getOperator().getJsonName(), body);
            default:
                body = Collections.<String, Object>singletonMap(condition.getField(), condition.getValue());
                return Collections.singletonMap(condition.getOperator().getJsonName(), body);
        }
    }

    private void addSort(Map<String, Object> root) {
        List<Object> output = new ArrayList<>();
        if (!sorts.isEmpty()) {
            for (SortOption sort : sorts) output.add(buildSort(sort.getField(), sort.getDirection().getJsonName()));
        } else if (legacySortField != null) {
            output.add(buildSort(legacySortField, legacySortDescending ? "desc" : "asc"));
        }
        if (!output.isEmpty()) root.put("sort", output);
    }

    private static Object buildSort(String field, String direction) {
        return Collections.singletonMap(field, Collections.singletonMap("order", direction));
    }

    private static Object buildSourceFilter(SourceFilter filter) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!filter.getIncludes().isEmpty()) result.put("includes", filter.getIncludes());
        if (!filter.getExcludes().isEmpty()) result.put("excludes", filter.getExcludes());
        return result;
    }

    private static Object buildHighlight(HighlightSpec spec) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!spec.getPreTags().isEmpty()) result.put("pre_tags", spec.getPreTags());
        if (!spec.getPostTags().isEmpty()) result.put("post_tags", spec.getPostTags());
        if (spec.getFragmentSize() != null) result.put("fragment_size", spec.getFragmentSize());
        if (spec.getNumberOfFragments() != null) result.put("number_of_fragments", spec.getNumberOfFragments());
        Map<String, Object> fields = new LinkedHashMap<>();
        for (String field : spec.getFields()) fields.put(field, Collections.emptyMap());
        result.put("fields", fields);
        return result;
    }

    private static Map<String, Object> buildAggregations(Collection<AggregationSpec> specs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (AggregationSpec spec : specs) {
            if (result.containsKey(spec.getName())) {
                throw new IllegalArgumentException("Duplicate aggregation name: " + spec.getName());
            }
            result.put(spec.getName(), buildAggregation(spec));
        }
        return result;
    }

    private static Object buildAggregation(AggregationSpec spec) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (spec.getType() == AggregationType.CUSTOM) {
            body.putAll(spec.getCustomBody());
        } else {
            Map<String, Object> options = new LinkedHashMap<>();
            options.put("field", spec.getField());
            switch (spec.getType()) {
                case TERMS:
                    if (spec.getSize() != null) options.put("size", spec.getSize());
                    body.put("terms", options);
                    break;
                case RANGE:
                    List<Object> ranges = new ArrayList<>();
                    for (AggregationRange range : spec.getRanges()) {
                        Map<String, Object> rangeBody = new LinkedHashMap<>();
                        if (range.getKey() != null) rangeBody.put("key", range.getKey());
                        if (range.getFrom() != null) rangeBody.put("from", range.getFrom());
                        if (range.getTo() != null) rangeBody.put("to", range.getTo());
                        ranges.add(rangeBody);
                    }
                    options.put("ranges", ranges);
                    body.put("range", options);
                    break;
                case DATE_HISTOGRAM:
                    if (spec.getCalendarInterval() != null) options.put("calendar_interval", spec.getCalendarInterval());
                    if (spec.getFixedInterval() != null) options.put("fixed_interval", spec.getFixedInterval());
                    body.put("date_histogram", options);
                    break;
                case AVG: case SUM: case MIN: case MAX: case CARDINALITY:
                    body.put(spec.getType().name().toLowerCase(), options);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported aggregation type: " + spec.getType());
            }
        }
        if (!spec.getSubAggregations().isEmpty()) {
            body.put("aggs", buildAggregations(spec.getSubAggregations()));
        }
        return body;
    }

    private SearchQueryBuilder addLegacy(String nestedPath, QueryOperator operator, String field, Object value) {
        if (isAbsent(value)) return this;
        return condition(QueryCondition.builder(operator, field)
                .nestedPath(nestedPath)
                .value(value)
                .build());
    }

    private boolean hasShouldCondition() {
        for (QueryCondition condition : conditions) {
            if (condition.getClause() == BoolClause.SHOULD) return true;
        }
        return false;
    }

    private static boolean isAbsent(Object value) {
        return value == null || value instanceof String && ((String) value).trim().isEmpty();
    }
}
