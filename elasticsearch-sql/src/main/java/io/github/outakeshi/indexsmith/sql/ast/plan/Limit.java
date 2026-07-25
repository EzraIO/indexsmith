package io.github.outakeshi.indexsmith.sql.ast.plan;

import io.github.outakeshi.indexsmith.sql.ast.expression.UnresolvedExpression;
import lombok.Getter;

/**
 */
@Getter
public class Limit extends UnresolvedPlan {

    private final UnresolvedExpression offset;
    private final UnresolvedExpression limit;

    public Limit(UnresolvedExpression offset,UnresolvedExpression limit) {

        this.limit = limit;
        this.offset = offset;
    }

}
