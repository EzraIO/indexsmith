package io.github.indexsmith.sql.ast.plan;

import com.google.common.collect.ImmutableList;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class From extends UnresolvedPlan {

    private Table table;

    private Where where;

    private Aggregation aggregation;

    private Order order;

    @Override
    public List<? extends UnresolvedPlan> child() {
        ImmutableList.Builder<UnresolvedPlan> builder = ImmutableList.builder();
        if (Objects.nonNull(table)) {
            builder.add(table);
        }
        if (Objects.nonNull(where)) {
            builder.add(where);
        }
        if (Objects.nonNull(aggregation)) {
            builder.add(aggregation);
        }
        if (Objects.nonNull(order)) {
            builder.add(order);
        }
        return builder.build();
    }

    @Override
    public void addChild(UnresolvedPlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (plan instanceof Table) {
            setTable((Table) plan);
            return;
        }
        if (plan instanceof Where) {
            setWhere((Where) plan);
            return;
        }

        if (plan instanceof Aggregation) {
            setAggregation((Aggregation) plan);
            return;
        }

        if (plan instanceof Order) {
            setOrder((Order) plan);
            return;
        }
        throw new IllegalArgumentException("From does not support child plan: " + plan.getClass().getName());
    }
}
