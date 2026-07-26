package io.github.indexsmith.util.ast;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import java.util.Objects;

/**
 */
public class MethodChainCallExpr {

    private final NameExpr nameExpr;

    private MethodCallExpr methodCallExpr = null;

    public MethodChainCallExpr(NameExpr nameExpr) {
        this.nameExpr = nameExpr;
    }

    public MethodChainCallExpr(String nameExpr) {
        this.nameExpr = new NameExpr(nameExpr);
    }

    public MethodChainCallExpr callMethod(String methodName, Expression... arg) {
        if (Objects.isNull(this.methodCallExpr)) {
            this.methodCallExpr = new MethodCallExpr(this.nameExpr, methodName, NodeList.nodeList(arg));
            return this;
        }
        this.methodCallExpr = new MethodCallExpr(this.methodCallExpr, methodName, NodeList.nodeList(arg));
        return this;
    }

    public MethodChainCallExpr callMethod(String methodName) {
        if (Objects.isNull(this.methodCallExpr)) {
            this.methodCallExpr = new MethodCallExpr(this.nameExpr, methodName);
            return this;
        }
        this.methodCallExpr = new MethodCallExpr(this.methodCallExpr, methodName);
        return this;
    }

    public MethodCallExpr getMethodCallExpr() {
        return methodCallExpr;
    }
}
