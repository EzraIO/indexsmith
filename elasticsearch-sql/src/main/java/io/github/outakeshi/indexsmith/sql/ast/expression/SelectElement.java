package io.github.outakeshi.indexsmith.sql.ast.expression;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SelectElement extends UnresolvedExpression {

    private UnresolvedExpression expression;

    private String alias;

    @Override
    public List<UnresolvedExpression> child() {
        return Lists.newArrayList(expression);
    }
}
