package io.github.outakeshi.indexsmith.api;

import java.util.Objects;

/** One Elasticsearch sort field and direction. */
public final class SortOption {
    private final String field;
    private final SortDirection direction;

    public SortOption(String field, SortDirection direction) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("sort field must not be blank");
        }
        this.field = field.trim();
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    public SortOption(QueryField field, SortDirection direction) {
        this(Objects.requireNonNull(field, "field").getEsFieldPathName(), direction);
    }

    public String getField() { return field; }
    public SortDirection getDirection() { return direction; }
}
