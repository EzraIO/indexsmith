package io.github.indexsmith.sql.ast.plan;

import io.github.indexsmith.sql.ast.expression.UnresolvedExpression;
import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 */
public class SelectElements extends UnresolvedPlan {

    @Getter
    private final List<UnresolvedExpression> selectFields = Lists.newArrayList();


    public void addField(UnresolvedExpression field) {
        if (Objects.isNull(field)) {
            return;
        }
        selectFields.add(field);
    }

}
