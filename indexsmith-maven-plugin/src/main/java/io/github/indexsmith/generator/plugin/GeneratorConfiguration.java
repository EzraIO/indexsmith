package io.github.indexsmith.generator.plugin;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

import java.util.List;

/**
 */
@Data
@XStreamAlias("generatorConfiguration")
public class GeneratorConfiguration {
    private String sourceDir;
    private List<Cluster> clusters;
    private TargetPackage targetPackage;
    private String writeMode;
}
