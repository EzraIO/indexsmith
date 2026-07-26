package io.github.indexsmith.generator.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** Describes an object or nested node from an Elasticsearch mapping. */
@Builder
@Getter
public class EsFieldNode implements EsFieldDefinition {
    private Map<String, EsField> fieldMap;
    private Map<String, EsFieldNode> nodeMap;
    /** Elasticsearch mapping type: {@code nested} or {@code object}. */
    private String esType;

    private boolean arrayField;

    public Map<String, EsField> getFieldMap() {
        return fieldMap;
    }

    public void setFieldMap(Map<String, EsField> fieldMap) {
        this.fieldMap = fieldMap;
    }

    public Map<String, EsFieldNode> getNodeMap() {
        return nodeMap;
    }

    public void setNodeMap(Map<String, EsFieldNode> nodeMap) {
        this.nodeMap = nodeMap;
    }
    public void setEsType(String esType) {
        this.esType = esType;
    }

    public void setArrayField(boolean arrayField) {
        this.arrayField = arrayField;
    }
}
