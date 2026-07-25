package io.github.outakeshi.indexsmith.generator.model;

import lombok.Data;

import java.util.Set;

/**
 */
@Data
public class EsIndexInfo {
    private String indexNames;
    private Set<String> arrFieldPath;
    private Set<String> queryConditionFields;
}
