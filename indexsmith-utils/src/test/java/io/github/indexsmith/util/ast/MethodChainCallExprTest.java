package io.github.indexsmith.util.ast;

import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MethodChainCallExprTest {
    @Test
    public void buildsFluentMethodChain() {
        MethodChainCallExpr chain = new MethodChainCallExpr("client")
                .callMethod("index", new StringLiteralExpr("orders"))
                .callMethod("refresh");

        assertEquals("client.index(\"orders\").refresh()", chain.getMethodCallExpr().toString());
    }

    @Test
    public void hasNoCallBeforeFirstMethod() {
        assertNull(new MethodChainCallExpr("client").getMethodCallExpr());
    }
}
