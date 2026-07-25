package io.github.outakeshi.indexsmith.generator.plugin;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

/**
 */
@Data
@XStreamAlias("targetPackage")
public class TargetPackage {
    private String model;

    private String fieldEnum;

    private String esQueryModel;

    private String client;
}