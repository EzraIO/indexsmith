package io.github.indexsmith.generator.model;

/** Common Elasticsearch type metadata for mapping leaves and object nodes. */
public interface EsFieldDefinition {

    String getEsType();

    boolean isArrayField();
}
