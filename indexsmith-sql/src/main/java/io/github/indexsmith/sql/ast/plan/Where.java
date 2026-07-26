package io.github.indexsmith.sql.ast.plan;

import io.github.indexsmith.sql.ast.expression.UnresolvedExpression;

/**
 */
public class Where extends UnresolvedPlan {

    private UnresolvedExpression expression;

    public UnresolvedExpression getExpression() {
        return expression;
    }

    public void setExpression(UnresolvedExpression expression) {
        this.expression = expression;
    }


}
