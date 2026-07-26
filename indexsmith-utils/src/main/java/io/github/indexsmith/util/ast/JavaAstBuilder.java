package io.github.indexsmith.util.ast;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 */
public class JavaAstBuilder {

    /**
     * 声明变量 并且使用无参构造方法初始化
     *
     * @param type
     * @param name
     * @param <T>
     * @return
     */
    public static <T> ExpressionStmt variableDeclaratorWithConstruct(NodeList<Modifier> modifiers, Class<T> type
            , String name, NodeList<Expression> arguments) {
        VariableDeclarator credentialsProviderDeclarator = new VariableDeclarator()
                .setName(name)
                .setType(type);

        credentialsProviderDeclarator.setInitializer(new ObjectCreationExpr()
                .setType(type).setArguments(arguments));
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr();
        variableDeclarationExpr.setVariables(NodeList.nodeList(credentialsProviderDeclarator));
        variableDeclarationExpr.setModifiers(NodeList.nodeList(modifiers));
        return new ExpressionStmt(variableDeclarationExpr);
    }

    public static <T> ExpressionStmt variableDeclaratorWithConstruct(NodeList<Modifier> modifiers, Class<T> type
            , String name) {
        return variableDeclaratorWithConstruct(modifiers, type, name, NodeList.nodeList());
    }

    public static ExpressionStmt arrStrVariableDeclaratorWithInit(NodeList<Modifier> modifiers
            , String name, Collection<String> values) {
        StringLiteralExpr[] literalExprs = values.stream()
                .map(StringLiteralExpr::new)
                .toArray(StringLiteralExpr[]::new);
        ArrayCreationExpr arrayCreationExpr = new ArrayCreationExpr();
        arrayCreationExpr.setElementType(String.class)
                .setInitializer(new ArrayInitializerExpr()
                        .setValues(NodeList.nodeList(literalExprs)));

        VariableDeclarator credentialsProviderDeclarator = new VariableDeclarator()
                .setName(name)
                .setType(String[].class);

        credentialsProviderDeclarator.setInitializer(arrayCreationExpr);
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr();
        variableDeclarationExpr.setVariables(NodeList.nodeList(credentialsProviderDeclarator));
        variableDeclarationExpr.setModifiers(NodeList.nodeList(modifiers));
        return new ExpressionStmt(variableDeclarationExpr);
    }

    public static ExpressionStmt arrStrVariableDeclaratorWithInit(String name, Collection<String> values) {
        return arrStrVariableDeclaratorWithInit(NodeList.nodeList(), name, values);
    }

    /**
     * 声明变量 并且使用无参构造方法初始化
     *
     * @param type
     * @param name
     * @param <T>
     * @return
     */
    public static <T> ExpressionStmt variableDeclaratorWithConstruct(NodeList<Modifier> modifiers, Class<T> type, String name, Object[] args) {
        VariableDeclarator credentialsProviderDeclarator = new VariableDeclarator()
                .setName(name)
                .setType(type);

        credentialsProviderDeclarator.setInitializer(new ObjectCreationExpr()
                .setType(type));
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr();
        variableDeclarationExpr.setVariables(NodeList.nodeList(credentialsProviderDeclarator));
        variableDeclarationExpr.setModifiers(NodeList.nodeList(modifiers));
        return new ExpressionStmt(variableDeclarationExpr);
    }


    public static <T> ExpressionStmt variableDeclaratorWithCallMethod(NodeList<Modifier> modifiers, Class<T> type, String variableName
            , MethodCallExpr methodCallExpr) {
        VariableDeclarator credentialsProviderDeclarator = new VariableDeclarator()
                .setName(variableName)
                .setType(type);

        credentialsProviderDeclarator.setInitializer(methodCallExpr);
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr();
        variableDeclarationExpr.setVariables(NodeList.nodeList(credentialsProviderDeclarator));
        if (modifiers != null && !modifiers.isEmpty()) {
            variableDeclarationExpr.setModifiers(NodeList.nodeList(modifiers));
        }
        return new ExpressionStmt(variableDeclarationExpr);

    }
}
