package io.github.outakeshi.indexsmith.sql.ast.builder;

import io.github.outakeshi.indexsmith.sql.antlr.parser.BaseSQLParser;
import io.github.outakeshi.indexsmith.sql.antlr.parser.BaseSQLParserBaseVisitor;
import io.github.outakeshi.indexsmith.sql.ast.expression.UnresolvedExpression;
import io.github.outakeshi.indexsmith.sql.ast.plan.Aggregation;
import io.github.outakeshi.indexsmith.sql.ast.plan.From;
import io.github.outakeshi.indexsmith.sql.ast.plan.Limit;
import io.github.outakeshi.indexsmith.sql.ast.plan.Order;
import io.github.outakeshi.indexsmith.sql.ast.plan.PlanType;
import io.github.outakeshi.indexsmith.sql.ast.plan.Project;
import io.github.outakeshi.indexsmith.sql.ast.plan.SelectElements;
import io.github.outakeshi.indexsmith.sql.ast.plan.Table;
import io.github.outakeshi.indexsmith.sql.ast.plan.UnresolvedPlan;
import io.github.outakeshi.indexsmith.sql.ast.plan.Where;

import java.util.List;

/**
 */
public class SelectAstBuilder extends BaseSQLParserBaseVisitor<UnresolvedPlan> {


    private AstExpressionBuilder astExpressionBuilder = new AstExpressionBuilder();

    @Override
    public UnresolvedPlan visitQuerySpecification(BaseSQLParser.QuerySpecificationContext ctx) {
        Project project = new Project(PlanType.SELECT);
        // add selectElement
        project.addChild(visitSelectClause(ctx.selectClause()));

        // add from
        project.addChild(visitFromClause(ctx.fromClause()));

        // add limits
        if (ctx.limitClause() != null) {
            project.addChild(visitLimitClause(ctx.limitClause()));
        }
        return project;
    }


    @Override
    public UnresolvedPlan visitSelectClause(BaseSQLParser.SelectClauseContext ctx) {
        return visitSelectElements(ctx.selectElements());
    }

    @Override
    public UnresolvedPlan visitSelectElements(BaseSQLParser.SelectElementsContext ctx) {
        List<BaseSQLParser.SelectElementContext> selectElementContexts = ctx.selectElement();
        SelectElements selectElements = new SelectElements();
        if (ctx.star != null) {
            selectElements.addField(new io.github.outakeshi.indexsmith.sql.ast.expression.Name("*"));
        }
        selectElementContexts.stream()
                .map(o -> astExpressionBuilder.visit(o))
                .forEach(selectElements::addField);
        return selectElements;

    }

    @Override
    public UnresolvedPlan visitFromClause(BaseSQLParser.FromClauseContext ctx) {
        From from = new From();
        UnresolvedPlan unresolvedPlan = visitTableName(ctx.tableName());
        from.addChild(unresolvedPlan);
        if (ctx.whereClause() != null) from.addChild(visitWhereClause(ctx.whereClause()));
        if (ctx.groupByClause() != null) from.addChild(visitGroupByClause(ctx.groupByClause()));
        if (ctx.orderByClause() != null) from.addChild(visitOrderByClause(ctx.orderByClause()));
        return from;
    }

    @Override
    public UnresolvedPlan visitTableName(BaseSQLParser.TableNameContext ctx) {
        UnresolvedExpression unresolvedExpression = astExpressionBuilder.visitQualifiedName(ctx.qualifiedName());
        Table table = new Table();
        table.setTableExpression(unresolvedExpression);
        return table;
    }

    @Override
    public UnresolvedPlan visitWhereClause(BaseSQLParser.WhereClauseContext ctx) {
        Where where = new Where();
        where.setExpression(astExpressionBuilder.visit(ctx.expression()));
        return where;
    }

    @Override
    public UnresolvedPlan visitGroupByElements(BaseSQLParser.GroupByElementsContext ctx) {
        List<BaseSQLParser.GroupByElementContext> groupByElementContexts = ctx.groupByElement();
        Aggregation aggregation = new Aggregation();
        groupByElementContexts.stream()
                .map(astExpressionBuilder::visit)
                .forEach(aggregation::addGroupByElement);
        return aggregation;
    }

    @Override
    public UnresolvedPlan visitGroupByClause(BaseSQLParser.GroupByClauseContext ctx) {
        return visitGroupByElements(ctx.groupByElements());
    }

    @Override
    public UnresolvedPlan visitOrderByClause(BaseSQLParser.OrderByClauseContext ctx) {
        Order order = new Order();
        List<BaseSQLParser.OrderByElementContext> orderByElementContexts = ctx.orderByElement();
        orderByElementContexts.stream()
                .map(astExpressionBuilder::visit)
                .forEach(order::addOrderByElement);
        return order;
    }

    @Override
    public UnresolvedPlan visitLimitClause(BaseSQLParser.LimitClauseContext ctx) {
        UnresolvedExpression offset = ctx.offset == null ? null : astExpressionBuilder.visit(ctx.offset);
        return new Limit(offset, astExpressionBuilder.visit(ctx.limit));
    }
}
