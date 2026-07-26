package io.github.indexsmith.api;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 */
@Data
public class PageSearchResponse<T extends BaseHit<?>> {
    /**
     * 结果总数
     */
    private long total;

    private long totalHits;

    private List<T> hits;

    private Map<String, AggregationResult> aggregations;

    private String scrollId;

    private Object suggest;

    private boolean timedOut;
    /**
     * 是否查询成功
     */
    private boolean isSucceed;
}
