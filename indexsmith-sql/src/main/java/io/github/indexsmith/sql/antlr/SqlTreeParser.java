package io.github.indexsmith.sql.antlr;

import io.github.indexsmith.sql.antlr.parser.BaseSQLLexer;
import io.github.indexsmith.sql.antlr.parser.BaseSQLParser;
import io.github.indexsmith.sql.exception.SyntaxAnalysisErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 *
 */

public class SqlTreeParser {

    /**
     * @param sql sql
     * @return
     */
    public static BaseSQLParser.RootContext parse(String sql) {
        BaseSQLLexer lexer = new BaseSQLLexer(new UppercaseCharStream(sql));
        BaseSQLParser parser = new BaseSQLParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new SyntaxAnalysisErrorListener());
        return parser.root();
    }
}
