package io.github.outakeshi.indexsmith.sql.ast;

import java.util.List;

/**
 */
public interface Node {

    /**
     * 获取子节点
     * @return
     */
    List<? extends Node> child();
}
