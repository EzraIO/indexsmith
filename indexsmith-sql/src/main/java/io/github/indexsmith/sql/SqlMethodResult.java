package io.github.indexsmith.sql;

import com.github.javaparser.ast.stmt.BlockStmt;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import io.github.indexsmith.sql.ast.plan.Project;

/**
 */
@Builder
@Getter
public class SqlMethodResult {

    private Project queryPlan;

    private List<Object> paramsJavaAstList;
    private BlockStmt methodJavaAst;
    private List<Object> resultJavaAstList;

}
