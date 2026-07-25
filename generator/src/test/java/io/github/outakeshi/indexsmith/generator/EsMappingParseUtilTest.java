package io.github.outakeshi.indexsmith.generator;

import io.github.outakeshi.indexsmith.generator.model.EsFieldNode;
import io.github.outakeshi.indexsmith.generator.util.EsMappingParseUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EsMappingParseUtilTest {
    @Test
    public void parsesScalarObjectNestedAndArrayFields() throws Exception {
        String mapping = "{\"properties\":{" +
                "\"status\":{\"type\":\"keyword\"}," +
                "\"tags\":{\"type\":\"keyword\"}," +
                "\"items\":{\"type\":\"nested\",\"properties\":{" +
                "\"skuId\":{\"type\":\"long\"}}}}}";

        EsFieldNode root = EsMappingParseUtil.parse2Node(mapping,
                new HashSet<>(Arrays.asList("tags", "items")));

        assertEquals("keyword", root.getFieldMap().get("status").getEsType());
        assertFalse(root.getFieldMap().get("status").isArrFiled());
        assertTrue(root.getFieldMap().get("tags").isArrFiled());
        assertEquals("nested", root.getNodeMap().get("items").getEsType());
        assertTrue(root.getNodeMap().get("items").isArrFiled());
        assertEquals("long", root.getNodeMap().get("items").getFieldMap().get("skuId").getEsType());
    }

    @Test
    public void handlesNullArrayConfigurationAndReportsMalformedMappings() throws Exception {
        EsFieldNode root = EsMappingParseUtil.parse2Node(
                "{\"properties\":{\"status\":{\"type\":\"keyword\"}}}", null);
        assertFalse(root.getFieldMap().get("status").isArrFiled());

        try {
            EsMappingParseUtil.parse2Node("{\"dynamic\":true}", null);
            fail("Expected missing properties to be rejected");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("properties"));
        }
    }
}
