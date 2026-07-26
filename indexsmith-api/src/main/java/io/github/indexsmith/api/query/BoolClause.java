package io.github.indexsmith.api.query;

/** Elasticsearch bool-query clause used by a {@link QueryCondition}. */
public enum BoolClause {
    MUST("must"),
    FILTER("filter"),
    SHOULD("should"),
    MUST_NOT("must_not");

    private final String jsonName;

    BoolClause(String jsonName) {
        this.jsonName = jsonName;
    }

    public String getJsonName() {
        return jsonName;
    }
}
