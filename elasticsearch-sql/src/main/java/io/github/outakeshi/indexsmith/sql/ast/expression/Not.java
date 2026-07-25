package io.github.outakeshi.indexsmith.sql.ast.expression;

import io.github.outakeshi.indexsmith.sql.common.LogicalOperatorEnum;
import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;

/**
 */
@Getter
public class Not extends LogicalExpression {
    private final UnresolvedExpression right;


    public Not(UnresolvedExpression right) {
        super(LogicalOperatorEnum.NOT);
        this.right = right;
    }

    @Override
    public List<UnresolvedExpression> child() {
        return Lists.newArrayList(right);
    }
}