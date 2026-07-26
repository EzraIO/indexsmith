package io.github.indexsmith.sql.ast.builder;

import io.github.indexsmith.sql.ast.expression.LogicalExpression;
import io.github.indexsmith.sql.ast.expression.UnresolvedExpression;
import io.github.indexsmith.sql.common.LogicalOperatorEnum;

import java.util.Collections;
import java.util.List;

/**
 */
public class Not extends LogicalExpression {
    private final UnresolvedExpression expression;

    public Not(UnresolvedExpression expression) {
        super(LogicalOperatorEnum.NOT);
        this.expression = expression;
    }

    @Override
    public List<UnresolvedExpression> child() {
        return Collections.singletonList(expression);
    }

}
