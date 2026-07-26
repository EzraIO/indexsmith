package io.github.indexsmith.sql.util;

import io.github.indexsmith.util.ast.JavaAstBuilder;
import io.github.indexsmith.util.ast.MethodChainCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Legacy ES7-specific Java source helper. The experimental SQL module only guarantees parsing and AST output.
 * @deprecated use the version-neutral client and query APIs instead of generated ES7 QueryBuilders code
 */
@Deprecated
public class ElasticsearchAstFactory {

    public static ExpressionStmt buildBoolQueryBuilderStmt(String variableName){
        return JavaAstBuilder.variableDeclaratorWithCallMethod(null, BoolQueryBuilder.class, variableName
                , new MethodChainCallExpr(QueryBuilders.class.getSimpleName())
                        .callMethod("boolQuery")
                        .getMethodCallExpr());
    }
}
