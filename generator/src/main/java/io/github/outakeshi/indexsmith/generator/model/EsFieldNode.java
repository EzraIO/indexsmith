package io.github.outakeshi.indexsmith.generator.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 */
@Builder
@Getter
public class EsFieldNode implements BaseNodeInterface {
    private Map<String, EsField> fieldMap;
    private Map<String, EsFieldNode> nodeMap;
    /**
     * nest or object
     */
    private String esType;

    private boolean arrFiled;

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

    public void setArrFiled(boolean arrFiled) {
        this.arrFiled = arrFiled;
    }
}
