package io.github.outakeshi.indexsmith.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Version-neutral bucket, metric, or custom aggregation response. */
public final class AggregationResult {
    private final AggregationType type;
    private final Number value;
    private final Long docCount;
    private final List<AggregationBucket> buckets;
    private final Map<String, Object> raw;

    public AggregationResult(AggregationType type, Number value, Long docCount,
                             List<AggregationBucket> buckets, Map<String, Object> raw) {
        this.type = type == null ? AggregationType.UNKNOWN : type;
        this.value = value;
        this.docCount = docCount;
        this.buckets = Collections.unmodifiableList(new ArrayList<>(buckets));
        this.raw = Collections.unmodifiableMap(new LinkedHashMap<>(raw));
    }

    public AggregationType getType() { return type; }
    public Number getValue() { return value; }
    public Long getDocCount() { return docCount; }
    public List<AggregationBucket> getBuckets() { return buckets; }
    public Map<String, Object> getRaw() { return raw; }
}
