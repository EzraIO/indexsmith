package io.github.indexsmith.api;

import io.github.indexsmith.api.aggregation.*;
import io.github.indexsmith.api.query.*;
import io.github.indexsmith.api.repository.*;
import io.github.indexsmith.api.result.*;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ApiModelsTest {
    @Test
    public void storesPaginationInput() {
        PageQuery query = new PageQuery();
        assertEquals(1, query.getPageNo());
        assertEquals(20, query.getPageSize());
        query.setPageNo(2);
        query.setPageSize(50);
        query.setSortFieldName("createdAt");
        query.setDescending(true);

        assertEquals(2, query.getPageNo());
        assertEquals(50, query.getPageSize());
        assertEquals("createdAt", query.getSortFieldName());
        assertTrue(query.isDescending());
    }

    @Test
    public void storesHitMetadataAndPageResult() {
        BaseHit<String> hit = new BaseHit<>();
        hit.setId("42");
        hit.setIndex("orders");
        hit.setData("payload");

        PageSearchResponse<BaseHit<String>> response = new PageSearchResponse<>();
        response.setHits(Collections.singletonList(hit));
        response.setTotal(1);
        response.setTotalHits(5);
        response.setSucceed(true);
        response.setTimedOut(false);
        response.setScrollId("scroll-1");

        assertSame(hit, response.getHits().get(0));
        assertEquals("orders", hit.getIndex());
        assertEquals(1, response.getTotal());
        assertEquals(5, response.getTotalHits());
        assertEquals("scroll-1", response.getScrollId());
        assertTrue(response.isSucceed());
    }
}
