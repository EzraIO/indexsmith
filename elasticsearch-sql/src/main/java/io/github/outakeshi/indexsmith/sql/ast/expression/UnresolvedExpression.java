package io.github.outakeshi.indexsmith.sql.ast.expression;

import io.github.outakeshi.indexsmith.sql.ast.Node;

import java.util.List;



/**
 */
public abstract class UnresolvedExpression implements Node {


    @Override
    public abstract List<UnresolvedExpression> child();

}
