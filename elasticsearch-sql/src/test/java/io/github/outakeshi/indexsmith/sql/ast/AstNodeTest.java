package io.github.outakeshi.indexsmith.sql.ast;

import io.github.outakeshi.indexsmith.sql.ast.expression.BinaryLogicalExpression;
import io.github.outakeshi.indexsmith.sql.ast.expression.Name;
import io.github.outakeshi.indexsmith.sql.ast.plan.From;
import io.github.outakeshi.indexsmith.sql.ast.plan.Limit;
import io.github.outakeshi.indexsmith.sql.ast.plan.Project;
import io.github.outakeshi.indexsmith.sql.ast.plan.SelectElements;
import io.github.outakeshi.indexsmith.sql.ast.plan.Table;
import io.github.outakeshi.indexsmith.sql.common.CompareEnum;
import io.github.outakeshi.indexsmith.sql.common.LogicalOperatorEnum;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AstNodeTest {
    @Test
    public void exposesBinaryChildrenInOrder() {
        Name left = new Name("left");
        Name right = new Name("right");
        BinaryLogicalExpression expression = new BinaryLogicalExpression(left, right, LogicalOperatorEnum.AND);

        assertEquals(left, expression.child().get(0));
        assertEquals(right, expression.child().get(1));
        assertEquals(CompareEnum.GTE, CompareEnum.textOfEnum(">="));
    }

    @Test
    public void projectAndFromKeepSupportedChildren() {
        Project project = new Project(io.github.outakeshi.indexsmith.sql.ast.plan.PlanType.SELECT);
        SelectElements select = new SelectElements();
        From from = new From();
        Table table = new Table();
        table.setTableExpression(new Name("orders"));
        from.addChild(table);
        Limit limit = new Limit(null, new Name("10"));
        project.addChild(select);
        project.addChild(from);
        project.addChild(limit);

        assertEquals(3, project.child().size());
        assertEquals(table, from.child().get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownComparisonOperator() {
        CompareEnum.textOfEnum("<=>");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void leafPlansRejectUnexpectedChildren() {
        new Limit(null, new Name("10")).addChild(new Table());
    }
}
