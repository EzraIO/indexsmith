package io.github.outakeshi.indexsmith.generator.model;

import lombok.Builder;
import lombok.Getter;

/**
 */
@Getter
@Builder
public class EsField implements BaseNodeInterface {


    private final String esType;

    private final boolean arrFiled;

}
