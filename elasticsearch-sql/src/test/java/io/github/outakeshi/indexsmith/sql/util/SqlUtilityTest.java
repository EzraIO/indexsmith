package io.github.outakeshi.indexsmith.sql.util;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SqlUtilityTest {
    @Test
    public void unquotesTextAndIdentifiers() {
        assertEquals("field", StringUtils.unquoteIdentifier("`field`"));
        assertEquals("value", StringUtils.unquoteText("'value'"));
        assertEquals("plain", StringUtils.unquoteText("plain"));
        assertEquals("x=2", StringUtils.format("x=%d", 2));
    }

    @Test
    public void matchesSqlLikeAndRegularExpressions() {
        assertTrue(OperatorUtils.matches("order-42", "order-%"));
        assertTrue(OperatorUtils.matches("A1", "A_"));
        assertTrue(OperatorUtils.matches("100%", "100\\%"));
        assertFalse(OperatorUtils.matches("AB", "A\\_"));
        assertEquals(1, OperatorUtils.matchesRegexp("abc42", "abc\\d+"));
        assertEquals(0, OperatorUtils.matchesRegexp("abc", "\\d+"));
    }

    @Test
    public void formatsExpressionLists() {
        assertEquals("a,2,true", ExpressionUtils.format(Arrays.asList("a", 2, true)));
    }

    @Test
    public void identifiesAndBuildsSystemTables() {
        assertFalse(SystemIndexUtils.isSystemIndex(null));
        assertTrue(SystemIndexUtils.isSystemIndex("_ODFE_SYS_TABLE_META.ALL"));
        assertEquals("_ODFE_SYS_TABLE_MAPPINGS.orders", SystemIndexUtils.mappingTable("orders"));

        SystemIndexUtils.SystemTable meta = SystemIndexUtils.systemTable("_ODFE_SYS_TABLE_META.ALL");
        assertTrue(meta.isSystemInfoTable());
        assertEquals("ALL", meta.getTableName());

        SystemIndexUtils.SystemTable mapping = SystemIndexUtils.systemTable("_ODFE_SYS_TABLE_MAPPINGS.order%");
        assertTrue(mapping.isMetaInfoTable());
        assertEquals("order*", mapping.getTableName());
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsInvalidSystemTable() {
        SystemIndexUtils.systemTable("orders");
    }
}
