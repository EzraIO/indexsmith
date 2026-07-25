package io.github.outakeshi.indexsmith.sql.ast.plan;

import io.github.outakeshi.indexsmith.sql.ast.expression.UnresolvedExpression;
import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 */
public class Aggregation extends UnresolvedPlan {

    @Getter
    private final List<UnresolvedExpression> groupByElements = Lists.newArrayList();


    public void addGroupByElement(UnresolvedExpression groupByElement) {
        if (Objects.isNull(groupByElement)) {
            return;
        }
        groupByElements.add(groupByElement);
    }

    @Override
    public List<? extends UnresolvedPlan> child() {
        return java.util.Collections.emptyList();
    }

}
