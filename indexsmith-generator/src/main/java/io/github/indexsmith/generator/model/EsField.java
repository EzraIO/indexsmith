package io.github.indexsmith.generator.model;

import lombok.Builder;
import lombok.Getter;

/** Describes a leaf field from an Elasticsearch mapping. */
@Getter
@Builder
public class EsField implements EsFieldDefinition {
    private final String esType;

    private final boolean arrayField;
}
