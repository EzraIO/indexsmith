package io.github.indexsmith.api.aggregation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One bucket in an aggregation response. */
public final class AggregationBucket {
    private final Object key;
    private final String keyAsString;
    private final Object from;
    private final String fromAsString;
    private final Object to;
    private final String toAsString;
    private final long docCount;
    private final Map<String, AggregationResult> aggregations;

    public AggregationBucket(Object key, String keyAsString, long docCount,
                             Map<String, AggregationResult> aggregations) {
        this(key, keyAsString, null, null, null, null, docCount, aggregations);
    }

    public AggregationBucket(Object key, String keyAsString, Object from, String fromAsString,
                             Object to, String toAsString, long docCount,
                             Map<String, AggregationResult> aggregations) {
        this.key = key;
        this.keyAsString = keyAsString;
        this.from = from;
        this.fromAsString = fromAsString;
        this.to = to;
        this.toAsString = toAsString;
        this.docCount = docCount;
        this.aggregations = Collections.unmodifiableMap(new LinkedHashMap<>(aggregations));
    }

    public Object getKey() { return key; }
    public String getKeyAsString() { return keyAsString; }
    public Object getFrom() { return from; }
    public String getFromAsString() { return fromAsString; }
    public Object getTo() { return to; }
    public String getToAsString() { return toAsString; }
    public long getDocCount() { return docCount; }
    public Map<String, AggregationResult> getAggregations() { return aggregations; }
}
