package io.github.indexsmith.sql;

import io.github.indexsmith.generator.model.EsIndexInfo;
import lombok.Builder;
import lombok.Getter;

/**
 */
@Builder
@Getter
public class SqlMethodParam {
    /**
     * need transform sql
     */
    private String sql;


    /**
     * es mappings
     */
    private EsIndexInfo esConfigContext;
}
