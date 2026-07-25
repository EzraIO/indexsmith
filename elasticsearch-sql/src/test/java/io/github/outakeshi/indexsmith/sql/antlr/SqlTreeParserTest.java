package io.github.outakeshi.indexsmith.sql.antlr;

import io.github.outakeshi.indexsmith.sql.exception.SyntaxCheckException;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.misc.Interval;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SqlTreeParserTest {
    @Test
    public void parsesKeywordsCaseInsensitively() {
        assertNotNull(SqlTreeParser.parse("select id from orders where status = 'paid';")
                .sqlStatement().selectStatement());
    }

    @Test(expected = SyntaxCheckException.class)
    public void reportsInvalidSyntax() {
        SqlTreeParser.parse("SELECT FROM orders");
    }

    @Test
    public void uppercasesLookaheadButPreservesOriginalText() {
        UpperCharChatStream stream = new UpperCharChatStream("select");
        assertEquals('S', stream.LA(1));
        assertEquals("select", stream.getText(Interval.of(0, 5)));
        stream.consume();
        assertEquals(1, stream.index());
        stream.seek(0);
        assertEquals(0, stream.index());
        assertEquals(IntStream.UNKNOWN_SOURCE_NAME, stream.getSourceName());
    }
}
