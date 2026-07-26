package io.github.indexsmith.api;

import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.ClientRequest;
import io.github.indexsmith.client.ClientResponse;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.client.IndexSmithClientFactory;
import io.github.indexsmith.client.SearchRequest;
import io.github.indexsmith.client.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Closeable;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Version-neutral repository backed by the adapter selected for the server version.
 */
public abstract class BaseElasticsearchRepository<T> implements Closeable {
    private final IndexSmithClient esClient;
    private final Class<T> modelClass;

    protected BaseElasticsearchRepository(ClientConfiguration configuration, Class<T> modelClass)
            throws IOException {
        this(new IndexSmithClientFactory().create(configuration), modelClass);
    }

    protected BaseElasticsearchRepository(IndexSmithClient esClient, Class<T> modelClass) {
        this.esClient = Objects.requireNonNull(esClient, "esClient");
        this.modelClass = Objects.requireNonNull(modelClass, "modelClass");
    }

    protected abstract String getIndex();

    public IndexSmithClient getEsClient() {
        return esClient;
    }

    public BulkWriteResult batchInsert(List<BaseHit<T>> models) throws IOException {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("models must not be empty");
        }
        List<String> indices = new ArrayList<>();
        for (BaseHit<T> model : models) {
            validateHit(model, "bulk item");
            indices.add(requiredIndex(insertIndex(model)));
        }
        try {
            StringBuilder body = new StringBuilder();
            for (int index = 0; index < models.size(); index++) {
                BaseHit<T> model = models.get(index);
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("_index", indices.get(index));
                if (model.getId() != null && !model.getId().trim().isEmpty()) {
                    metadata.put("_id", model.getId());
                }
                body.append(JsonUtil.toJson(Collections.singletonMap("index", metadata))).append('\n');
                body.append(JsonUtil.toJson(model.getData())).append('\n');
            }
            ClientResponse response = esClient.perform(ClientRequest.ndjson("POST", "/_bulk", body.toString()));
            return mapBulkWriteResponse(response, models.size());
        } catch (Exception exception) {
            throw operationFailure("bulk index", exception);
        }
    }

    public WriteResult insert(BaseHit<T> model) throws IOException {
        validateHit(model, "model");
        String rawIndex = requiredIndex(insertIndex(model));
        try {
            String index = encodePathSegment(rawIndex);
            boolean hasId = model.getId() != null && !model.getId().trim().isEmpty();
            String path = "/" + index + "/_doc" + (hasId ? "/" + encodePathSegment(model.getId()) : "");
            ClientResponse response = esClient.perform(ClientRequest.json(
                    hasId ? "PUT" : "POST", path, JsonUtil.toJson(model.getData())));
            return mapWriteResponse(response, WriteOperation.INDEX, rawIndex, model.getId());
        } catch (Exception exception) {
            throw operationFailure("index", exception);
        }
    }

    public WriteResult deleteByPk(String id) throws IOException {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        String index = requiredIndex(getIndex());
        try {
            ClientResponse response = esClient.perform(ClientRequest.delete(
                    "/" + encodePathSegment(index) + "/_doc/" + encodePathSegment(id)));
            return mapWriteResponse(response, WriteOperation.DELETE, index, id);
        } catch (Exception exception) {
            throw operationFailure("delete", exception);
        }
    }

    public PageSearchResponse<BaseHit<T>> queryCondition(String queryBody) throws IOException {
        SearchResult response = esClient.search(new SearchRequest(getIndex(), queryBody));
        return mapSearchResponse(response, Collections.<AggregationSpec>emptyList());
    }

    public PageSearchResponse<BaseHit<T>> queryCondition(String queryBody,
                                                          List<AggregationSpec> aggregationSpecs) throws IOException {
        SearchResult response = esClient.search(new SearchRequest(getIndex(), queryBody));
        return mapSearchResponse(response, aggregationSpecs);
    }

    public PageSearchResponse<BaseHit<T>> mapSearchResponse(SearchResult response) throws IOException {
        return mapSearchResponse(response, Collections.<AggregationSpec>emptyList());
    }

    public PageSearchResponse<BaseHit<T>> mapSearchResponse(SearchResult response,
                                                             List<AggregationSpec> aggregationSpecs) throws IOException {
        if (response == null || response.getResponseBody() == null) {
            throw new IOException("Elasticsearch returned an empty search response");
        }
        JsonNode root = JsonUtil.readTree(response.getResponseBody());
        if (root.has("error")) {
            throw new IOException("Elasticsearch search failed: " + root.get("error"));
        }

        PageSearchResponse<BaseHit<T>> result = new PageSearchResponse<>();
        JsonNode hitContainer = root.path("hits");
        JsonNode hits = hitContainer.path("hits");
        List<BaseHit<T>> mappedHits = new ArrayList<>();
        if (hits.isArray()) {
            for (JsonNode hit : hits) {
                BaseHit<T> mapped = new BaseHit<>();
                mapped.setId(hit.path("_id").asText(null));
                mapped.setIndex(hit.path("_index").asText(null));
                mapped.setData(JsonUtil.treeToValue(hit.path("_source"), modelClass));
                mapped.setHighlight(parseHighlight(hit.path("highlight")));
                mapped.setSortValues(parseNeutralList(hit.path("sort")));
                mappedHits.add(mapped);
            }
        }

        JsonNode total = hitContainer.path("total");
        result.setTotal(mappedHits.size());
        result.setTotalHits(total.isObject() ? total.path("value").asLong() : total.asLong());
        result.setHits(mappedHits);
        result.setScrollId(root.path("_scroll_id").asText(null));
        result.setTimedOut(root.path("timed_out").asBoolean(false));
        result.setAggregations(parseAggregations(root.get("aggregations"), aggregationSpecs));
        result.setSuggest(asNeutralValue(root.get("suggest")));
        result.setSucceed(true);
        return result;
    }

    public T parse(String data) throws IOException {
        return JsonUtil.fromJson(data, modelClass);
    }

    @Override
    public void close() throws IOException {
        esClient.close();
    }

    private String insertIndex(BaseHit<T> model) {
        return model.getIndex() == null || model.getIndex().trim().isEmpty() ? getIndex() : model.getIndex();
    }

    private BulkWriteResult mapBulkWriteResponse(ClientResponse response, int expectedItems) throws IOException {
        JsonNode root = readResponse(response, "bulk index");
        if (root == null) {
            return new BulkWriteResult(response.getStatusCode(), 0, Collections.<WriteResult>emptyList(),
                    true, new ElasticsearchError("http_error", response.getBody()), response.getBody());
        }
        List<WriteResult> items = new ArrayList<>();
        JsonNode itemNodes = root.path("items");
        if (itemNodes.isArray()) {
            for (JsonNode wrapper : itemNodes) {
                java.util.Iterator<Map.Entry<String, JsonNode>> fields = wrapper.fields();
                if (!fields.hasNext()) continue;
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode item = entry.getValue();
                WriteOperation operation = "delete".equals(entry.getKey()) ? WriteOperation.DELETE : WriteOperation.INDEX;
                items.add(new WriteResult(operation,
                        item.path("_index").asText(null), item.path("_id").asText(null),
                        item.path("status").asInt(response.getStatusCode()), item.path("result").asText(null),
                        parseError(item.get("error")), item.toString()));
            }
        }
        ElasticsearchError topLevelError = parseError(root.get("error"));
        boolean errors = root.path("errors").asBoolean(false);
        if (topLevelError != null) errors = true;
        for (WriteResult item : items) {
            if (!item.isSuccessful()) errors = true;
        }
        if (response.isSuccessful() && items.size() != expectedItems && topLevelError == null) {
            topLevelError = new ElasticsearchError("incomplete_bulk_response",
                    "Expected " + expectedItems + " bulk items but received " + items.size());
            errors = true;
        }
        return new BulkWriteResult(response.getStatusCode(), root.path("took").asLong(0), items,
                errors, topLevelError, response.getBody());
    }

    private WriteResult mapWriteResponse(ClientResponse response, WriteOperation operation,
                                         String fallbackIndex, String fallbackId) throws IOException {
        JsonNode root = readResponse(response, operation.name().toLowerCase());
        if (root == null) {
            return new WriteResult(operation, fallbackIndex, fallbackId, response.getStatusCode(), null,
                    new ElasticsearchError("http_error", response.getBody()), response.getBody());
        }
        return new WriteResult(operation,
                root.path("_index").asText(fallbackIndex), root.path("_id").asText(fallbackId),
                response.getStatusCode(), root.path("result").asText(null),
                parseError(root.get("error")), response.getBody());
    }

    /** Returns null only for a non-success response whose body is not JSON. */
    private JsonNode readResponse(ClientResponse response, String operation) throws IOException {
        String body = response.getBody();
        if (body == null || body.trim().isEmpty()) {
            if (!response.isSuccessful()) return null;
            throw new ElasticsearchOperationException(operation,
                    "Elasticsearch returned an empty successful response", null);
        }
        try {
            return JsonUtil.readTree(body);
        } catch (IOException exception) {
            if (!response.isSuccessful()) return null;
            throw new ElasticsearchOperationException(operation,
                    "Unable to decode successful Elasticsearch response", exception);
        }
    }

    private static ElasticsearchError parseError(JsonNode error) {
        if (error == null || error.isMissingNode() || error.isNull()) return null;
        if (error.isTextual()) return new ElasticsearchError("elasticsearch_error", error.asText());
        String type = error.path("type").asText(null);
        String reason = error.path("reason").asText(null);
        JsonNode rootCause = error.path("root_cause");
        if ((type == null || reason == null) && rootCause.isArray() && rootCause.size() > 0) {
            if (type == null) type = rootCause.get(0).path("type").asText(null);
            if (reason == null) reason = rootCause.get(0).path("reason").asText(null);
        }
        return new ElasticsearchError(type, reason == null ? error.toString() : reason);
    }

    private static Map<String, List<String>> parseHighlight(JsonNode highlight) {
        if (!highlight.isObject()) return Collections.emptyMap();
        Map<String, List<String>> result = new LinkedHashMap<>();
        java.util.Iterator<Map.Entry<String, JsonNode>> fields = highlight.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            List<String> fragments = new ArrayList<>();
            if (entry.getValue().isArray()) {
                for (JsonNode value : entry.getValue()) fragments.add(value.asText());
            }
            result.put(entry.getKey(), Collections.unmodifiableList(fragments));
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<Object> parseNeutralList(JsonNode values) throws IOException {
        if (!values.isArray()) return Collections.emptyList();
        List<Object> result = new ArrayList<>();
        for (JsonNode value : values) result.add(asNeutralValue(value));
        return Collections.unmodifiableList(result);
    }

    private static Map<String, AggregationResult> parseAggregations(JsonNode values,
                                                                    List<AggregationSpec> specs) throws IOException {
        if (values == null || !values.isObject()) return Collections.emptyMap();
        Map<String, AggregationSpec> byName = new HashMap<>();
        if (specs != null) {
            for (AggregationSpec spec : specs) {
                AggregationSpec previous = byName.put(spec.getName(), spec);
                if (previous != null) throw new IOException("Duplicate aggregation spec: " + spec.getName());
            }
        }
        Map<String, AggregationResult> result = new LinkedHashMap<>();
        java.util.Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String name = aggregationName(entry.getKey());
            if (result.containsKey(name)) {
                throw new IOException("Duplicate aggregation name after typed-key normalization: " + name);
            }
            result.put(name, parseAggregation(entry.getValue(), byName.get(name)));
        }
        return Collections.unmodifiableMap(result);
    }

    private static AggregationResult parseAggregation(JsonNode value, AggregationSpec spec) throws IOException {
        AggregationType type = spec == null ? AggregationType.UNKNOWN : spec.getType();
        Number metric = value.path("value").isNumber() ? value.path("value").numberValue() : null;
        Long docCount = value.path("doc_count").isNumber() ? value.path("doc_count").asLong() : null;
        List<AggregationBucket> buckets = new ArrayList<>();
        JsonNode bucketValues = value.path("buckets");
        if (bucketValues.isArray()) {
            for (JsonNode bucket : bucketValues) buckets.add(parseBucket(bucket, spec));
        } else if (bucketValues.isObject()) {
            java.util.Iterator<Map.Entry<String, JsonNode>> keyed = bucketValues.fields();
            while (keyed.hasNext()) {
                Map.Entry<String, JsonNode> entry = keyed.next();
                buckets.add(parseBucket(entry.getValue(), spec, entry.getKey()));
            }
        }
        Map<String, Object> raw = JsonUtil.fromJson(value.toString(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { });
        return new AggregationResult(type, metric, docCount, buckets, raw);
    }

    private static AggregationBucket parseBucket(JsonNode bucket, AggregationSpec parentSpec) throws IOException {
        return parseBucket(bucket, parentSpec, null);
    }

    private static AggregationBucket parseBucket(JsonNode bucket, AggregationSpec parentSpec,
                                                   String fallbackKey) throws IOException {
        Object key = bucket.has("key") ? asNeutralValue(bucket.get("key")) : fallbackKey;
        Map<String, AggregationSpec> subSpecs = new HashMap<>();
        if (parentSpec != null) {
            for (AggregationSpec sub : parentSpec.getSubAggregations()) subSpecs.put(sub.getName(), sub);
        }
        Map<String, AggregationResult> subResults = new LinkedHashMap<>();
        java.util.Iterator<Map.Entry<String, JsonNode>> fields = bucket.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (isBucketMetadata(entry.getKey())) continue;
            if (entry.getValue().isObject()) {
                String name = aggregationName(entry.getKey());
                if (subResults.containsKey(name)) {
                    throw new IOException("Duplicate sub-aggregation name after typed-key normalization: " + name);
                }
                subResults.put(name, parseAggregation(entry.getValue(), subSpecs.get(name)));
            }
        }
        return new AggregationBucket(key, bucket.path("key_as_string").asText(null),
                asNeutralValue(bucket.get("from")), bucket.path("from_as_string").asText(null),
                asNeutralValue(bucket.get("to")), bucket.path("to_as_string").asText(null),
                bucket.path("doc_count").asLong(0), subResults);
    }

    private static boolean isBucketMetadata(String field) {
        return "key".equals(field) || "key_as_string".equals(field) || "doc_count".equals(field)
                || "from".equals(field) || "from_as_string".equals(field)
                || "to".equals(field) || "to_as_string".equals(field);
    }

    private static String aggregationName(String field) {
        int separator = field.indexOf('#');
        return separator < 0 ? field : field.substring(separator + 1);
    }

    private static ElasticsearchOperationException operationFailure(String operation, Exception exception) {
        if (exception instanceof ElasticsearchOperationException) {
            return (ElasticsearchOperationException) exception;
        }
        return new ElasticsearchOperationException(operation,
                "Elasticsearch " + operation + " failed: " + exception.getMessage(), exception);
    }

    private static void validateHit(BaseHit<?> hit, String name) {
        if (hit == null) throw new IllegalArgumentException(name + " must not be null");
        if (hit.getData() == null) throw new IllegalArgumentException(name + " data must not be null");
    }

    private static String requiredIndex(String index) {
        if (index == null || index.trim().isEmpty()) {
            throw new IllegalArgumentException("index must not be blank");
        }
        return index.trim();
    }

    private static Object asNeutralValue(JsonNode value) throws IOException {
        return value == null || value.isMissingNode() ? null : JsonUtil.fromJson(value.toString(), Object.class);
    }

    private static String encodePathSegment(String value) throws IOException {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }
}
