package io.github.indexsmith.api.query;

import java.util.LinkedHashMap;
import java.util.Map;

/** Inclusive and exclusive bounds for a range query. */
public final class RangeBounds {
    private final Object gt;
    private final Object gte;
    private final Object lt;
    private final Object lte;

    private RangeBounds(Builder builder) {
        this.gt = builder.gt;
        this.gte = builder.gte;
        this.lt = builder.lt;
        this.lte = builder.lte;
        if (isEmpty()) {
            throw new IllegalArgumentException("At least one range bound is required");
        }
        if (gt != null && gte != null) {
            throw new IllegalArgumentException("Use either gt or gte, not both");
        }
        if (lt != null && lte != null) {
            throw new IllegalArgumentException("Use either lt or lte, not both");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (gt != null) result.put("gt", gt);
        if (gte != null) result.put("gte", gte);
        if (lt != null) result.put("lt", lt);
        if (lte != null) result.put("lte", lte);
        return result;
    }

    private boolean isEmpty() {
        return gt == null && gte == null && lt == null && lte == null;
    }

    public static final class Builder {
        private Object gt;
        private Object gte;
        private Object lt;
        private Object lte;

        public Builder gt(Object value) { this.gt = value; return this; }
        public Builder gte(Object value) { this.gte = value; return this; }
        public Builder lt(Object value) { this.lt = value; return this; }
        public Builder lte(Object value) { this.lte = value; return this; }
        public RangeBounds build() { return new RangeBounds(this); }
    }
}
