package io.github.indexsmith.sql;

import io.github.indexsmith.sql.antlr.SqlTreeParser;
import io.github.indexsmith.sql.antlr.parser.BaseSQLParser;

/**
 */
public class SqlTransformService {

    public SqlMethodResult transform(SqlMethodParam sql) {
        if (sql == null || sql.getSql() == null || sql.getSql().trim().isEmpty()) {
            throw new IllegalArgumentException("sql must not be blank");
        }
        BaseSQLParser.RootContext parse = SqlTreeParser.parse(sql.getSql());
        return SqlMethodResult.builder()
                .queryPlan(new io.github.indexsmith.sql.ast.builder.AstParser().visit(parse))
                .build();
    }
}
