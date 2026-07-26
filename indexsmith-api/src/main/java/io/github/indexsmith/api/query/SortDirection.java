package io.github.indexsmith.api.query;

public enum SortDirection {
    ASC("asc"), DESC("desc");

    private final String jsonName;

    SortDirection(String jsonName) { this.jsonName = jsonName; }

    public String getJsonName() { return jsonName; }
}
