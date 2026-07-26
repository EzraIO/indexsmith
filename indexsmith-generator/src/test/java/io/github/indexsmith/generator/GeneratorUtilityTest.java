package io.github.indexsmith.generator;

import io.github.indexsmith.generator.builder.BlockCommentBuilder;
import io.github.indexsmith.generator.builder.ClassOrInterfaceTypeBuilder;
import io.github.indexsmith.generator.exception.UnsupportedDataTypeException;
import io.github.indexsmith.generator.model.FieldTypeMappingEnum;
import io.github.indexsmith.generator.model.IntegerCounter;
import io.github.indexsmith.generator.util.JavaBeansUtil;
import io.github.indexsmith.generator.util.JavaNameUtil;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeneratorUtilityTest {
    @Test
    public void convertsIndexAndFieldNames() {
        assertEquals("OrderItemsModel", JavaNameUtil.getEsModelName("order_items"));
        assertEquals("OrderItemsClient", JavaNameUtil.getEsClientName("order-items"));
        assertEquals("ORDER_VIEW_ID", JavaNameUtil.convert("orderViewId"));
        assertEquals("URL", JavaBeansUtil.getValidPropertyName("URL"));
        assertEquals("userName", JavaBeansUtil.getCamelCaseString("USER_NAME", false));
        assertEquals("setURL", JavaBeansUtil.getSetterMethodName("URL"));
    }

    @Test
    public void mapsSupportedElasticsearchTypes() {
        assertEquals(String.class, FieldTypeMappingEnum.getType("keyword"));
        assertEquals(Long.class, FieldTypeMappingEnum.getType("long"));
        assertEquals(String.class, FieldTypeMappingEnum.getType("date"));
        assertEquals(FieldTypeMappingEnum.BOOLEAN, FieldTypeMappingEnum.getEnum("boolean"));
    }

    @Test(expected = UnsupportedDataTypeException.class)
    public void rejectsUnsupportedElasticsearchType() {
        FieldTypeMappingEnum.getType("wildcard");
    }

    @Test
    public void buildsGenericTypesAndComments() {
        assertEquals("Map<String, Long>", ClassOrInterfaceTypeBuilder.builder("Map")
                .addTypeArgument("String").addTypeArgument("Long").build());
        String comment = BlockCommentBuilder.builder(Arrays.asList("first", "second")).build().toString();
        assertTrue(comment.contains("* first"));
        assertTrue(comment.contains("* second"));
    }

    @Test
    public void incrementsCounterInBothModes() {
        IntegerCounter counter = new IntegerCounter();
        assertEquals(0, counter.getAndAdd());
        assertEquals(2, counter.addAndGet());
        assertEquals(2, counter.getCount());
    }
}
