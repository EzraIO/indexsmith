package io.github.outakeshi.indexsmith.api;

/** Field metadata implemented by generated field enums. */
public interface QueryField {
    String getEsFieldPathName();

    String getEsFieldType();

    String getNestedPath();
}
