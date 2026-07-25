package io.github.outakeshi.indexsmith.sql;

import io.github.outakeshi.indexsmith.sql.ast.builder.Not;
import io.github.outakeshi.indexsmith.sql.ast.expression.Compare;
import io.github.outakeshi.indexsmith.sql.ast.expression.Function;
import io.github.outakeshi.indexsmith.sql.ast.expression.Literal;
import io.github.outakeshi.indexsmith.sql.ast.expression.SelectElement;
import io.github.outakeshi.indexsmith.sql.ast.expression.UnresolvedExpression;
import io.github.outakeshi.indexsmith.sql.ast.plan.Project;
import io.github.outakeshi.indexsmith.sql.common.CompareEnum;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SqlExpressionTransformTest {
    private final SqlTransformService service = new SqlTransformService();

    @Test
    public void buildsLikeAndNullPredicatesAsFunctions() {
        Function like = (Function) where("SELECT id FROM orders WHERE name NOT LIKE 'test%'");
        assertEquals("NOT_LIKE", like.getName());
        assertEquals("name", like.getArguments().get(0).toString());
        assertEquals("test%", ((Literal) like.getArguments().get(1)).getValue());

        Function isNull = (Function) where("SELECT id FROM orders WHERE deletedAt IS NULL");
        assertEquals("IS_NULL", isNull.getName());
        assertEquals(1, isNull.child().size());
    }

    @Test
    public void buildsNotBooleanAndSignedDecimalExpressions() {
        Not not = (Not) where("SELECT id FROM orders WHERE NOT active = true");
        Compare booleanComparison = (Compare) not.child().get(0);
        assertEquals(Boolean.TRUE, ((Literal) booleanComparison.getRight()).getValue());

        Compare decimalComparison = (Compare) where("SELECT id FROM orders WHERE price >= -1.5");
        assertEquals(CompareEnum.GTE, decimalComparison.getCompareEnum());
        assertEquals(new BigDecimal("-1.5"), ((Literal) decimalComparison.getRight()).getValue());
    }

    @Test
    public void buildsDateAndNullLiteralsWithAlias() {
        Project project = project("SELECT DATE '2026-07-21' AS businessDay, NULL FROM orders");
        SelectElement date = (SelectElement) project.getSelectElements().getSelectFields().get(0);
        SelectElement nullValue = (SelectElement) project.getSelectElements().getSelectFields().get(1);

        assertEquals("businessDay", date.getAlias());
        assertEquals("2026-07-21", ((Literal) date.getExpression()).getValue());
        assertNull(((Literal) nullValue.getExpression()).getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullRequestObject() {
        service.transform(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void explicitlyRejectsDeleteUntilImplemented() {
        project("DELETE FROM orders WHERE id = 1");
    }

    private UnresolvedExpression where(String sql) {
        return project(sql).getFrom().getWhere().getExpression();
    }

    private Project project(String sql) {
        Project project = service.transform(SqlMethodParam.builder().sql(sql).build()).getQueryPlan();
        assertTrue(project != null);
        return project;
    }
}
