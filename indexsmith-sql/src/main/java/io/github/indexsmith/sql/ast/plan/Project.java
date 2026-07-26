package io.github.indexsmith.sql.ast.plan;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

/**
 */

public class Project extends UnresolvedPlan {

    @Getter
    private final PlanType planType;

    @Getter
    @Setter
    private SelectElements selectElements;

    @Getter
    @Setter
    private From from;

    @Getter
    @Setter
    private Limit limit;

    public Project(PlanType planType) {
        this.planType = planType;
    }

    @Override
    public List<? extends UnresolvedPlan> child() {
        ImmutableList.Builder<UnresolvedPlan> builder = ImmutableList.builder();
        if (Objects.nonNull(selectElements)) {
            builder.add(selectElements);
        }
        if (Objects.nonNull(from)) {
            builder.add(from);
        }
        if (Objects.nonNull(limit)){
            builder.add(limit);
        }
        return builder.build();
    }

    @Override
    public void addChild(UnresolvedPlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (plan instanceof SelectElements) {
            setSelectElements((SelectElements) plan);
            return;
        }
        if (plan instanceof From) {
            setFrom((From) plan);
            return;
        }
        if (plan instanceof Limit) {
            setLimit((Limit) plan);
            return;
        }

        throw new IllegalArgumentException("Project does not support child plan: " + plan.getClass().getName());
    }
}
