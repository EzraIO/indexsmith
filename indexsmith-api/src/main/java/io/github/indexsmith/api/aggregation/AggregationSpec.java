package io.github.indexsmith.api.aggregation;

import io.github.indexsmith.api.query.QueryField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable request definition for a named aggregation. */
public final class AggregationSpec {
    private final String name;
    private final AggregationType type;
    private final String field;
    private final Integer size;
    private final String calendarInterval;
    private final String fixedInterval;
    private final List<AggregationRange> ranges;
    private final List<AggregationSpec> subAggregations;
    private final Map<String, Object> customBody;

    private AggregationSpec(Builder builder) {
        this.name = requireText(builder.name, "aggregation name");
        this.type = Objects.requireNonNull(builder.type, "type");
        this.field = trimToNull(builder.field);
        this.size = builder.size;
        this.calendarInterval = trimToNull(builder.calendarInterval);
        this.fixedInterval = trimToNull(builder.fixedInterval);
        this.ranges = Collections.unmodifiableList(new ArrayList<>(builder.ranges));
        this.subAggregations = Collections.unmodifiableList(new ArrayList<>(builder.subAggregations));
        this.customBody = Collections.unmodifiableMap(new LinkedHashMap<>(builder.customBody));
        validate();
    }

    public static Builder builder(String name, AggregationType type) { return new Builder(name, type); }
    public static Builder builder(String name, AggregationType type, QueryField field) {
        return new Builder(name, type).field(Objects.requireNonNull(field, "field").getEsFieldPathName());
    }

    public String getName() { return name; }
    public AggregationType getType() { return type; }
    public String getField() { return field; }
    public Integer getSize() { return size; }
    public String getCalendarInterval() { return calendarInterval; }
    public String getFixedInterval() { return fixedInterval; }
    public List<AggregationRange> getRanges() { return ranges; }
    public List<AggregationSpec> getSubAggregations() { return subAggregations; }
    public Map<String, Object> getCustomBody() { return customBody; }

    private void validate() {
        if (type == AggregationType.UNKNOWN) {
            throw new IllegalArgumentException("UNKNOWN is only valid for response values");
        }
        Set<String> subAggregationNames = new HashSet<>();
        for (AggregationSpec subAggregation : subAggregations) {
            if (!subAggregationNames.add(subAggregation.getName())) {
                throw new IllegalArgumentException("duplicate sub-aggregation name: " + subAggregation.getName());
            }
        }
        if (type == AggregationType.CUSTOM) {
            if (customBody.isEmpty()) throw new IllegalArgumentException("CUSTOM aggregation requires a body");
            if (field != null || size != null || calendarInterval != null
                    || fixedInterval != null || !ranges.isEmpty()) {
                throw new IllegalArgumentException("CUSTOM aggregation only accepts customBody and sub-aggregations");
            }
            if (!subAggregations.isEmpty()
                    && (customBody.containsKey("aggs") || customBody.containsKey("aggregations"))) {
                throw new IllegalArgumentException("CUSTOM aggregation must not define raw and typed sub-aggregations");
            }
            return;
        }
        if (!customBody.isEmpty()) throw new IllegalArgumentException("customBody is only valid for CUSTOM aggregations");
        if (field == null) throw new IllegalArgumentException(type + " aggregation requires a field");
        if (size != null && size <= 0) throw new IllegalArgumentException("aggregation size must be positive");
        if (size != null && type != AggregationType.TERMS) {
            throw new IllegalArgumentException("size is only valid for TERMS aggregations");
        }
        if (!ranges.isEmpty() && type != AggregationType.RANGE) {
            throw new IllegalArgumentException("ranges are only valid for RANGE aggregations");
        }
        if (type == AggregationType.RANGE && ranges.isEmpty()) {
            throw new IllegalArgumentException("RANGE aggregation requires at least one range");
        }
        if (type != AggregationType.DATE_HISTOGRAM
                && (calendarInterval != null || fixedInterval != null)) {
            throw new IllegalArgumentException("intervals are only valid for DATE_HISTOGRAM aggregations");
        }
        if (type == AggregationType.DATE_HISTOGRAM) {
            boolean calendar = calendarInterval != null;
            boolean fixed = fixedInterval != null;
            if (calendar == fixed) {
                throw new IllegalArgumentException("DATE_HISTOGRAM requires exactly one interval type");
            }
        }
        if (!subAggregations.isEmpty() && isMetric(type)) {
            throw new IllegalArgumentException(type + " metric aggregation cannot contain sub-aggregations");
        }
    }

    private static boolean isMetric(AggregationType value) {
        return value == AggregationType.AVG || value == AggregationType.SUM
                || value == AggregationType.MIN || value == AggregationType.MAX
                || value == AggregationType.CARDINALITY;
    }

    private static String requireText(String value, String name) {
        String result = trimToNull(value);
        if (result == null) throw new IllegalArgumentException(name + " must not be blank");
        return result;
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public static final class Builder {
        private final String name;
        private final AggregationType type;
        private String field;
        private Integer size;
        private String calendarInterval;
        private String fixedInterval;
        private final List<AggregationRange> ranges = new ArrayList<>();
        private final List<AggregationSpec> subAggregations = new ArrayList<>();
        private final Map<String, Object> customBody = new LinkedHashMap<>();

        private Builder(String name, AggregationType type) { this.name = name; this.type = type; }
        public Builder field(String value) { field = value; return this; }
        public Builder field(QueryField value) {
            return field(Objects.requireNonNull(value, "field").getEsFieldPathName());
        }
        public Builder size(int value) { size = value; return this; }
        public Builder calendarInterval(String value) { calendarInterval = value; return this; }
        public Builder fixedInterval(String value) { fixedInterval = value; return this; }
        public Builder range(AggregationRange value) { ranges.add(Objects.requireNonNull(value, "range")); return this; }
        public Builder subAggregation(AggregationSpec value) {
            subAggregations.add(Objects.requireNonNull(value, "subAggregation"));
            return this;
        }
        public Builder customBody(Map<String, Object> value) {
            customBody.clear();
            if (value != null) customBody.putAll(value);
            return this;
        }
        public AggregationSpec build() { return new AggregationSpec(this); }
    }
}
