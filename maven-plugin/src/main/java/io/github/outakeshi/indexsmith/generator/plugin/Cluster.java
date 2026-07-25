package io.github.outakeshi.indexsmith.generator.plugin;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

import java.util.List;

/**
 */
@Data
@XStreamAlias("cluster")
public class Cluster {
    private Connection connection;
    private List<IndexInfo> indexes;
}
