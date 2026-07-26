package io.github.indexsmith.sql.ast.plan;


import io.github.indexsmith.sql.ast.expression.UnresolvedExpression;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Table extends UnresolvedPlan {

    private UnresolvedExpression tableExpression;

}
