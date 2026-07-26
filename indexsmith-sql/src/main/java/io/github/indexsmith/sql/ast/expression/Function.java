package io.github.indexsmith.sql.ast.expression;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class Function extends UnresolvedExpression {
    private final String name;
    private final List<UnresolvedExpression> arguments;

    public Function(String name, List<UnresolvedExpression> arguments) {
        this.name = name;
        this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
    }

    @Override
    public List<UnresolvedExpression> child() {
        return arguments;
    }
}
