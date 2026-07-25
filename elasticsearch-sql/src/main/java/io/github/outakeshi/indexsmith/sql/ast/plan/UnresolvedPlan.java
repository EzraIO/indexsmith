package io.github.outakeshi.indexsmith.sql.ast.plan;

import io.github.outakeshi.indexsmith.sql.ast.Node;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 */
public abstract class UnresolvedPlan implements Node {



    @Override
    public List<? extends UnresolvedPlan> child() {
        return ImmutableList.of();
    }

    public void addChild(UnresolvedPlan plan){
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not accept child plans");
    }

}
