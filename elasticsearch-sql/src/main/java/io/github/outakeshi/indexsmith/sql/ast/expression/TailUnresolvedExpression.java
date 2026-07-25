package io.github.outakeshi.indexsmith.sql.ast.expression;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 */
public class TailUnresolvedExpression extends UnresolvedExpression {

    @Override
    public List<UnresolvedExpression> child() {
        return ImmutableList.of();
    }
}
