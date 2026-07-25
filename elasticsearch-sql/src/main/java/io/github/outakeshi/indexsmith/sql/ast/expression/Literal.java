package io.github.outakeshi.indexsmith.sql.ast.expression;

import lombok.Getter;

@Getter
public class Literal extends TailUnresolvedExpression {
    private final Object value;

    public Literal(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
