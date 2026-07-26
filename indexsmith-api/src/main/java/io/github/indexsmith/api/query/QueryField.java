package io.github.indexsmith.api.query;

/** Field metadata implemented by generated field enums. */
public interface QueryField {
    String getEsFieldPathName();

    String getEsFieldType();

    String getNestedPath();
}
