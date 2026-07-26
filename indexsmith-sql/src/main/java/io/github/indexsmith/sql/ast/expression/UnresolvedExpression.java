package io.github.indexsmith.sql.ast.expression;

import io.github.indexsmith.sql.ast.Node;

import java.util.List;



/**
 */
public abstract class UnresolvedExpression implements Node {


    @Override
    public abstract List<UnresolvedExpression> child();

}
