package io.github.outakeshi.indexsmith.api;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 */
@Data
public class PageQuery {

    private String sortFieldName;

    private int pageNo = 1;

    private int pageSize = 20;

    private boolean descending;

    private List<QueryCondition> conditions = new ArrayList<>();

    private List<SortOption> sorts = new ArrayList<>();

    private List<Object> searchAfter = new ArrayList<>();

    private SourceFilter sourceFilter;

    private HighlightSpec highlight;

    private List<AggregationSpec> aggregations = new ArrayList<>();

    private Integer minimumShouldMatch;

    public PageQuery addCondition(QueryCondition condition) {
        if (conditions == null) conditions = new ArrayList<>();
        conditions.add(Objects.requireNonNull(condition, "condition"));
        return this;
    }

    public PageQuery addSort(SortOption sort) {
        if (sorts == null) sorts = new ArrayList<>();
        sorts.add(Objects.requireNonNull(sort, "sort"));
        return this;
    }

    public PageQuery addAggregation(AggregationSpec aggregation) {
        if (aggregations == null) aggregations = new ArrayList<>();
        aggregations.add(Objects.requireNonNull(aggregation, "aggregation"));
        return this;
    }
}
