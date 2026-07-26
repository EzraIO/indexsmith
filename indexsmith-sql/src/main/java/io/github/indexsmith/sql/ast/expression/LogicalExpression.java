package io.github.indexsmith.sql.ast.expression;

import io.github.indexsmith.sql.common.LogicalOperatorEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 */
@AllArgsConstructor
@Getter
public abstract class LogicalExpression extends UnresolvedExpression {


    private final LogicalOperatorEnum logicalOperator;


}
