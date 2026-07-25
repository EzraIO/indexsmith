package io.github.outakeshi.indexsmith.sql.ast.expression;

import com.google.common.collect.Lists;

import java.util.List;

/**
 */
public interface  BinaryExpression  {

    abstract UnresolvedExpression getLeft();

    abstract UnresolvedExpression getRight();


    public  default List<UnresolvedExpression> childOfBinary() {
        return Lists.newArrayList(getLeft(), getRight());
    }
}
