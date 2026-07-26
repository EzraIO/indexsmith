package io.github.indexsmith.util.ast;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JavaAstBuilderTest {
    @Test
    public void buildsConstructedVariable() {
        String source = JavaAstBuilder.variableDeclaratorWithConstruct(
                NodeList.nodeList(Modifier.finalModifier()), StringBuilder.class, "builder").toString();

        assertTrue(source.contains("final StringBuilder builder = new StringBuilder()"));
    }

    @Test
    public void buildsStringArrayLiteral() {
        String source = JavaAstBuilder.arrStrVariableDeclaratorWithInit(
                "indexes", Arrays.asList("orders", "products")).toString();

        assertTrue(source.contains("String[] indexes"));
        assertTrue(source.contains("{ \"orders\", \"products\" }"));
    }

    @Test
    public void buildsVariableInitializedByMethodCall() {
        MethodCallExpr call = new MethodCallExpr("create");
        String source = JavaAstBuilder.variableDeclaratorWithCallMethod(
                NodeList.nodeList(), String.class, "value", call).toString();

        assertEquals("String value = create();", source.trim());
    }
}
