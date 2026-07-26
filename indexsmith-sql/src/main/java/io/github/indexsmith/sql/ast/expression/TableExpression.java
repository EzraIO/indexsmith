package io.github.indexsmith.sql.ast.expression;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TableExpression extends UnresolvedExpression {


    private Name tableName;

    @Override
    public List<UnresolvedExpression> child() {
        return Lists.newArrayList(tableName);
    }
}
