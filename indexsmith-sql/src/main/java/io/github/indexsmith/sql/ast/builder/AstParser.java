package io.github.indexsmith.sql.ast.builder;

import io.github.indexsmith.sql.antlr.parser.BaseSQLParser;
import io.github.indexsmith.sql.antlr.parser.BaseSQLParserBaseVisitor;
import io.github.indexsmith.sql.ast.plan.Project;

/**
 */
public class AstParser extends BaseSQLParserBaseVisitor<Project> {

    private SelectAstBuilder selectAstBuilder = new SelectAstBuilder();

    @Override
    public Project visitRoot(BaseSQLParser.RootContext ctx) {
        if (ctx.sqlStatement() == null) {
            throw new IllegalArgumentException("SQL statement must not be empty");
        }
        return visit(ctx.sqlStatement());
    }

    @Override
    public Project visitSelectStatement(BaseSQLParser.SelectStatementContext ctx) {

        return (Project) selectAstBuilder.visit(ctx);

    }

    @Override
    public Project visitUpdateStatement(BaseSQLParser.UpdateStatementContext ctx) {
        throw new UnsupportedOperationException("UPDATE SQL is not supported yet");
    }


    @Override
    public Project visitDeleteStatement(BaseSQLParser.DeleteStatementContext ctx) {
        throw new UnsupportedOperationException("DELETE SQL is not supported yet");
    }

}
