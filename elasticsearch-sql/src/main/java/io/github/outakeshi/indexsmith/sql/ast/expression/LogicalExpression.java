package io.github.outakeshi.indexsmith.sql.ast.expression;

import io.github.outakeshi.indexsmith.sql.common.LogicalOperatorEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 */
@AllArgsConstructor
@Getter
public abstract class LogicalExpression extends UnresolvedExpression {


    private final LogicalOperatorEnum logicalOperator;


}
