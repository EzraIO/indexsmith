package io.github.indexsmith.sql.ast.expression;

import java.util.Collections;
import java.util.List;

/**
 */
public class Name extends TailUnresolvedExpression {
    private final List<String> parts;

    public Name(String name) {
        this.parts = Collections.singletonList(name);
    }

    public Name(List<String> name) {
        this.parts = Collections.unmodifiableList(name);
    }

    public List<String> getParts() {
        return parts;
    }

    @Override
    public String toString() {
        return String.join(".", parts);
    }
}
