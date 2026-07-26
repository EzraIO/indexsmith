package io.github.indexsmith.api;

/** Supported version-neutral Elasticsearch query operators. */
public enum QueryOperator {
    TERM("term"),
    MATCH("match"),
    MATCH_PHRASE("match_phrase"),
    TERMS("terms"),
    RANGE("range"),
    EXISTS("exists"),
    PREFIX("prefix"),
    WILDCARD("wildcard");

    private final String jsonName;

    QueryOperator(String jsonName) {
        this.jsonName = jsonName;
    }

    public String getJsonName() {
        return jsonName;
    }
}
