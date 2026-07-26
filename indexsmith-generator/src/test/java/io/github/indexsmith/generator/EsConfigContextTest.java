package io.github.indexsmith.generator;

import io.github.indexsmith.generator.model.EsConfigContext;
import io.github.indexsmith.generator.model.EsIndexInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EsConfigContextTest {
    @Test
    public void filtersConfiguredQueryFieldsByFullPath() {
        EsIndexInfo info = new EsIndexInfo();
        info.setIndexNames("orders");
        info.setQueryConditionFields(new HashSet<>(Arrays.asList("status", "items.skuId")));
        EsConfigContext context = new EsConfigContext();
        context.setEsIndexInfoMap(Collections.singletonMap("orders", info));

        assertTrue(context.isCondition("orders", Collections.emptyList(), "status"));
        assertTrue(context.isCondition("orders", Collections.singletonList("items"), "skuId"));
        assertFalse(context.isCondition("orders", Collections.singletonList("items"), "price"));
        assertFalse(context.isCondition("missing", Collections.emptyList(), "status"));
    }

    @Test
    public void emptyConditionSetAllowsEveryField() {
        EsIndexInfo info = new EsIndexInfo();
        info.setIndexNames("orders");
        info.setQueryConditionFields(Collections.emptySet());
        EsConfigContext context = new EsConfigContext();
        context.setEsIndexInfoMap(Collections.singletonMap("orders", info));

        assertTrue(context.isCondition("orders", Collections.emptyList(), "anything"));
    }
}
