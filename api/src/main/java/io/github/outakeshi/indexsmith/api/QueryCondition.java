package io.github.outakeshi.indexsmith.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable query condition that is independent of an Elasticsearch client version. */
public final class QueryCondition {
    private final BoolClause clause;
    private final QueryOperator operator;
    private final String field;
    private final String nestedPath;
    private final Object value;
    private final List<Object> values;
    private final RangeBounds range;

    private QueryCondition(Builder builder) {
        this.clause = Objects.requireNonNull(builder.clause, "clause");
        this.operator = Objects.requireNonNull(builder.operator, "operator");
        this.field = requireText(builder.field, "field");
        this.nestedPath = trimToNull(builder.nestedPath);
        this.value = builder.value;
        this.values = Collections.unmodifiableList(new ArrayList<>(builder.values));
        this.range = builder.range;
        validate();
    }

    public static Builder builder(QueryOperator operator, String field) {
        return new Builder(operator, field);
    }

    public static Builder builder(QueryOperator operator, QueryField field) {
        Objects.requireNonNull(field, "field");
        return new Builder(operator, field.getEsFieldPathName()).nestedPath(field.getNestedPath());
    }

    public BoolClause getClause() { return clause; }
    public QueryOperator getOperator() { return operator; }
    public String getField() { return field; }
    public String getNestedPath() { return nestedPath; }
    public Object getValue() { return value; }
    public List<Object> getValues() { return values; }
    public RangeBounds getRange() { return range; }

    private void validate() {
        switch (operator) {
            case EXISTS:
                return;
            case TERMS:
                if (values.isEmpty()) throw new IllegalArgumentException("TERMS requires at least one value");
                for (Object item : values) {
                    if (item == null) throw new IllegalArgumentException("TERMS values must not contain null");
                }
                return;
            case RANGE:
                if (range == null) throw new IllegalArgumentException("RANGE requires range bounds");
                return;
            default:
                if (isAbsent(value)) {
                    throw new IllegalArgumentException(operator + " requires a non-blank value");
                }
        }
    }

    private static boolean isAbsent(Object value) {
        return value == null || value instanceof String && ((String) value).trim().isEmpty();
    }

    private static String requireText(String value, String name) {
        String result = trimToNull(value);
        if (result == null) throw new IllegalArgumentException(name + " must not be blank");
        return result;
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    public static final class Builder {
        private BoolClause clause = BoolClause.MUST;
        private final QueryOperator operator;
        private final String field;
        private String nestedPath;
        private Object value;
        private final List<Object> values = new ArrayList<>();
        private RangeBounds range;

        private Builder(QueryOperator operator, String field) {
            this.operator = operator;
            this.field = field;
        }

        public Builder clause(BoolClause value) { this.clause = value; return this; }
        public Builder nestedPath(String value) { this.nestedPath = value; return this; }
        public Builder value(Object value) { this.value = value; return this; }
        public Builder values(Collection<?> value) {
            this.values.clear();
            if (value != null) this.values.addAll(value);
            return this;
        }
        public Builder range(RangeBounds value) { this.range = value; return this; }
        public QueryCondition build() { return new QueryCondition(this); }
    }
}
