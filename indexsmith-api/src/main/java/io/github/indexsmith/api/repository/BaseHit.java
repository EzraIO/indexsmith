package io.github.indexsmith.api.repository;

/**
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BaseHit<T> {

    /**
     * es id
     */
    @JsonProperty("_id")
    private String id;

    /**
     * index name
     */
    @JsonProperty("_index")
    private String index;

    /**
     * data
     */
    @JsonProperty("_source")
    private T data;

    /** Highlight fragments keyed by the Elasticsearch field path. */
    private Map<String, List<String>> highlight;

    /** Sort values returned by Elasticsearch, suitable for search_after. */
    private List<Object> sortValues;
}
