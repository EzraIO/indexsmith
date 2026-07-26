package io.github.indexsmith.sql.ast.builder;

import io.github.indexsmith.sql.antlr.parser.BaseSQLParser;
import io.github.indexsmith.sql.antlr.parser.BaseSQLParserBaseVisitor;
import io.github.indexsmith.sql.ast.expression.BinaryLogicalExpression;
import io.github.indexsmith.sql.ast.expression.Compare;
import io.github.indexsmith.sql.ast.expression.Function;
import io.github.indexsmith.sql.ast.expression.Literal;
import io.github.indexsmith.sql.ast.expression.Name;
import io.github.indexsmith.sql.ast.expression.SelectElement;
import io.github.indexsmith.sql.ast.expression.UnresolvedExpression;
import io.github.indexsmith.sql.common.CompareEnum;
import io.github.indexsmith.sql.common.LogicalOperatorEnum;
import io.github.indexsmith.sql.util.StringUtils;
import org.antlr.v4.runtime.RuleContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 */
public class AstExpressionBuilder extends BaseSQLParserBaseVisitor<UnresolvedExpression> {


    @Override
    public UnresolvedExpression visitSelectElement(BaseSQLParser.SelectElementContext ctx) {
        SelectElement selectElement = new SelectElement();
        BaseSQLParser.AliasContext alias = ctx.alias();
        if (Objects.nonNull(alias)) {
            String aliasName = StringUtils.unquoteIdentifier(ctx.alias().getText());
            selectElement.setAlias(aliasName);
        }
        selectElement.setExpression(this.visit(ctx.expression()));
        return selectElement;
    }

    @Override
    public UnresolvedExpression visitTableName(BaseSQLParser.TableNameContext ctx) {
        return visit(ctx.qualifiedName());
    }

    /********************* parse logic expression    ***********************/
    @Override
    public UnresolvedExpression visitAndExpression(BaseSQLParser.AndExpressionContext ctx) {
        return new BinaryLogicalExpression(visit(ctx.left), visit(ctx.right), LogicalOperatorEnum.AND);
    }

    @Override
    public UnresolvedExpression visitOrExpression(BaseSQLParser.OrExpressionContext ctx) {
        return new BinaryLogicalExpression(visit(ctx.left), visit(ctx.right), LogicalOperatorEnum.OR);
    }

    @Override
    public UnresolvedExpression visitNotExpression(BaseSQLParser.NotExpressionContext ctx) {
        return new Not(visit(ctx.expression()));
    }


    @Override
    public UnresolvedExpression visitBinaryComparisonPredicate(BaseSQLParser.BinaryComparisonPredicateContext ctx) {
        String functionName = ctx.comparisonOperator().getText();
        return new Compare(CompareEnum.textOfEnum(functionName)
                , visit(ctx.left), visit(ctx.right));
    }

    @Override
    public UnresolvedExpression visitIsNullPredicate(BaseSQLParser.IsNullPredicateContext ctx) {
        return new Function(
                ctx.nullNotnull().NOT() == null ? "IS_NULL" : "IS_NOT_NULL",
                Arrays.asList(visit(ctx.predicate())));
    }

    @Override
    public UnresolvedExpression visitLikePredicate(BaseSQLParser.LikePredicateContext ctx) {
        return new Function(
                ctx.NOT() == null ? "LIKE" : "NOT_LIKE",
                Arrays.asList(visit(ctx.left), visit(ctx.right)));
    }


    /********************* parse expressionAtom of constant    ***********************/
    @Override
    public UnresolvedExpression visitString(BaseSQLParser.StringContext ctx) {
        return visit(ctx.stringLiteral());
    }

    @Override
    public UnresolvedExpression visitSignedDecimal(BaseSQLParser.SignedDecimalContext ctx) {
        return new Literal(new BigDecimal(ctx.getText()));
    }

    @Override
    public UnresolvedExpression visitDecimalLiteral(BaseSQLParser.DecimalLiteralContext ctx) {
        return new Literal(new BigDecimal(ctx.getText()));
    }

    @Override
    public UnresolvedExpression visitSignedReal(BaseSQLParser.SignedRealContext ctx) {
        return new Literal(new BigDecimal(ctx.getText()));
    }

    @Override
    public UnresolvedExpression visitBoolean(BaseSQLParser.BooleanContext ctx) {
        return new Literal(Boolean.valueOf(ctx.getText().toLowerCase()));
    }

    @Override
    public UnresolvedExpression visitStringLiteral(BaseSQLParser.StringLiteralContext ctx) {
        return new Literal(StringUtils.unquoteText(ctx.getText()));
    }

    /********************* parse expressionAtom of columnName    ***********************/
    @Override
    public UnresolvedExpression visitColumnName(BaseSQLParser.ColumnNameContext ctx) {
        return visit(ctx.qualifiedName());
    }

    @Override
    public UnresolvedExpression visitOrderByElement(BaseSQLParser.OrderByElementContext ctx) {
        return visit(ctx.expression());
    }


    @Override
    public UnresolvedExpression visitNullLiteral(BaseSQLParser.NullLiteralContext ctx) {
        return new Literal(null);
    }

    @Override
    public UnresolvedExpression visitDateLiteral(BaseSQLParser.DateLiteralContext ctx) {
        return new Literal(StringUtils.unquoteText(ctx.date.getText()));
    }

    @Override
    public UnresolvedExpression visitTimeLiteral(BaseSQLParser.TimeLiteralContext ctx) {
        return new Literal(StringUtils.unquoteText(ctx.time.getText()));
    }

    @Override
    public UnresolvedExpression visitTimestampLiteral(BaseSQLParser.TimestampLiteralContext ctx) {
        return new Literal(StringUtils.unquoteText(ctx.timestamp.getText()));
    }

    @Override
    public UnresolvedExpression visitIntervalLiteral(BaseSQLParser.IntervalLiteralContext ctx) {
        return new Literal(ctx.getText());
    }

    @Override
    public UnresolvedExpression visitParamExpression(BaseSQLParser.ParamExpressionContext ctx) {
        return visit(ctx.paramName().javaVariableName().qualifiedName());
    }

    @Override
    public UnresolvedExpression visitIdent(BaseSQLParser.IdentContext ctx) {
        return visitIdentifiers(Collections.singletonList(ctx));
    }

    @Override
    public UnresolvedExpression visitQualifiedName(BaseSQLParser.QualifiedNameContext ctx) {
        return visitIdentifiers(ctx.ident());
    }

    private Name visitIdentifiers(List<BaseSQLParser.IdentContext> identifiers) {
        return new Name(
                identifiers.stream()
                        .map(RuleContext::getText)
                        .map(StringUtils::unquoteIdentifier)
                        .collect(Collectors.toList())
        );
    }


}
