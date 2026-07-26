package io.github.indexsmith.sql.ast.plan;

import io.github.indexsmith.sql.ast.expression.UnresolvedExpression;
import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 */
public class Order extends UnresolvedPlan {

    @Getter
    private final List<UnresolvedExpression> orderByElements = Lists.newArrayList();

    public void addOrderByElement(UnresolvedExpression orderByElement) {
        if (Objects.isNull(orderByElement)) {
            return;
        }
        orderByElements.add(orderByElement);
    }

}
