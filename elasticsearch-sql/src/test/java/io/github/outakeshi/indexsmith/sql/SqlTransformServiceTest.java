package io.github.outakeshi.indexsmith.sql;

import io.github.outakeshi.indexsmith.sql.ast.expression.Compare;
import io.github.outakeshi.indexsmith.sql.ast.expression.Name;
import io.github.outakeshi.indexsmith.sql.ast.plan.Project;
import io.github.outakeshi.indexsmith.sql.ast.plan.SelectElements;
import io.github.outakeshi.indexsmith.sql.common.CompareEnum;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SqlTransformServiceTest {
    private final SqlTransformService service = new SqlTransformService();

    @Test
    public void transformsSelectIntoStructuredPlan() {
        SqlMethodResult result = service.transform(SqlMethodParam.builder().sql(
                "SELECT id, name AS displayName FROM orders "
                        + "WHERE status = 'paid' GROUP BY id ORDER BY name DESC LIMIT 5, 10;").build());

        Project project = result.getQueryPlan();
        assertNotNull(project);
        assertEquals(2, project.getSelectElements().getSelectFields().size());
        assertEquals("orders", ((Name) project.getFrom().getTable().getTableExpression()).toString());
        Compare condition = (Compare) project.getFrom().getWhere().getExpression();
        assertEquals(CompareEnum.EQ, condition.getCompareEnum());
        assertEquals(1, project.getFrom().getAggregation().getGroupByElements().size());
        assertEquals(1, project.getFrom().getOrder().getOrderByElements().size());
        assertEquals(new BigDecimal("5"),
                ((io.github.outakeshi.indexsmith.sql.ast.expression.Literal) project.getLimit().getOffset()).getValue());
        assertEquals(new BigDecimal("10"),
                ((io.github.outakeshi.indexsmith.sql.ast.expression.Literal) project.getLimit().getLimit()).getValue());
    }

    @Test
    public void supportsStarAndOptionalClauses() {
        Project project = service.transform(SqlMethodParam.builder().sql("SELECT * FROM orders").build())
                .getQueryPlan();

        SelectElements fields = project.getSelectElements();
        assertEquals("*", fields.getSelectFields().get(0).toString());
        assertNull(project.getLimit());
        assertNull(project.getFrom().getWhere());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankSql() {
        service.transform(SqlMethodParam.builder().sql("  ").build());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void explicitlyRejectsUpdateUntilImplemented() {
        service.transform(SqlMethodParam.builder().sql("UPDATE orders status = 'paid' WHERE id = 1").build());
    }
}
