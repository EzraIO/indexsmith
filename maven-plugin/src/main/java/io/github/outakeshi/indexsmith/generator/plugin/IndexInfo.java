package io.github.outakeshi.indexsmith.generator.plugin;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

/**
 */
@Data
@XStreamAlias("index")
public class IndexInfo {
    private String name;
    private String queryConditionFields;
    private String arrFields;
}
