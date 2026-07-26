package io.github.indexsmith.sql.ast.expression;

import io.github.indexsmith.sql.common.LogicalOperatorEnum;
import lombok.Getter;

import java.util.List;

/**
 */
@Getter
public class BinaryLogicalExpression extends LogicalExpression implements BinaryExpression {

    private final UnresolvedExpression left;
    private final UnresolvedExpression right;


    public BinaryLogicalExpression(UnresolvedExpression left, UnresolvedExpression right, LogicalOperatorEnum logicalOperator) {
        super(logicalOperator);
        this.left = left;
        this.right = right;
    }


    @Override
    public List<UnresolvedExpression> child() {
        return childOfBinary();
    }
}
