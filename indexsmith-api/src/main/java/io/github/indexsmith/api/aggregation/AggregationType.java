package io.github.indexsmith.api.aggregation;

/** Aggregations with a stable, version-neutral representation. */
public enum AggregationType {
    TERMS,
    RANGE,
    DATE_HISTOGRAM,
    AVG,
    SUM,
    MIN,
    MAX,
    CARDINALITY,
    CUSTOM,
    UNKNOWN
}
