package io.github.indexsmith.api;

/** One keyed or unkeyed range used by a range aggregation. */
public final class AggregationRange {
    private final String key;
    private final Object from;
    private final Object to;

    public AggregationRange(String key, Object from, Object to) {
        if (from == null && to == null) {
            throw new IllegalArgumentException("Aggregation range requires from or to");
        }
        this.key = key == null || key.trim().isEmpty() ? null : key.trim();
        this.from = from;
        this.to = to;
    }

    public String getKey() { return key; }
    public Object getFrom() { return from; }
    public Object getTo() { return to; }
}
