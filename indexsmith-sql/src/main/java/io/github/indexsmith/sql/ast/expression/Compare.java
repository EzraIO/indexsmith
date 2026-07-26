package io.github.indexsmith.sql.ast.expression;

import io.github.indexsmith.sql.common.CompareEnum;
import lombok.Getter;

import java.util.List;

/**
 */
@Getter
public class Compare extends UnresolvedExpression implements BinaryExpression {

    private final CompareEnum compareEnum;
    private final UnresolvedExpression left;
    private final UnresolvedExpression right;

    public Compare(CompareEnum compareEnum, UnresolvedExpression left, UnresolvedExpression right) {
        this.compareEnum = compareEnum;
        this.left = left;
        this.right = right;
    }

    @Override
    public List<UnresolvedExpression> child() {
        return childOfBinary();
    }
}
