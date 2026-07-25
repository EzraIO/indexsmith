package io.github.outakeshi.indexsmith.sql.ast.plan;


import io.github.outakeshi.indexsmith.sql.ast.expression.UnresolvedExpression;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Table extends UnresolvedPlan {

    private UnresolvedExpression tableExpression;

}
