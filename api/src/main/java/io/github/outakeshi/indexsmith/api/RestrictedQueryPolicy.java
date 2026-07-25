package io.github.outakeshi.indexsmith.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.outakeshi.indexsmith.client.ClientOperation;
import io.github.outakeshi.indexsmith.client.QueryPolicy;
import io.github.outakeshi.indexsmith.client.QueryPolicyViolationException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Optional allow-list policy for applications that accept externally supplied queries. */
public final class RestrictedQueryPolicy implements QueryPolicy {
    private final Set<String> allowedIndices;
    private final Set<String> allowedMethods;
    private final Set<String> forbiddenPathPrefixes;
    private final Integer maxResultWindow;

    private RestrictedQueryPolicy(Builder builder) {
        this.allowedIndices = immutableCopy(builder.allowedIndices);
        this.allowedMethods = immutableCopy(builder.allowedMethods);
        this.forbiddenPathPrefixes = immutableCopy(builder.forbiddenPathPrefixes);
        this.maxResultWindow = builder.maxResultWindow;
    }

    public static Builder builder() { return new Builder(); }

    @Override
    public void validate(ClientOperation operation) throws QueryPolicyViolationException {
        if (!allowedMethods.isEmpty() && !allowedMethods.contains(operation.getMethod())) {
            throw new QueryPolicyViolationException("HTTP method is not allowed: " + operation.getMethod());
        }
        String path = withoutQuery(operation.getPath());
        for (String prefix : forbiddenPathPrefixes) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                throw new QueryPolicyViolationException("Elasticsearch path is forbidden: " + path);
            }
        }
        validateIndex(operation, path);
        validateResultWindow(operation, path);
    }

    private void validateIndex(ClientOperation operation, String path) throws QueryPolicyViolationException {
        if (allowedIndices.isEmpty()) return;
        if (bodyMayOverridePathIndex(path)) {
            throw new QueryPolicyViolationException(
                    "Request body may override the path index and cannot be verified: " + path);
        }
        String expression = operation.getIndex();
        if (expression == null) expression = firstPathSegment(path);
        if (expression == null) {
            if (mayTargetIndicesWithoutPathIndex(path)) {
                throw new QueryPolicyViolationException(
                        "Global request cannot be verified by an index allow-list: " + path);
            }
            return;
        }
        for (String index : expression.split(",")) {
            if (!allowedIndices.contains(index)) {
                throw new QueryPolicyViolationException("Elasticsearch index is not allowed: " + index);
            }
        }
    }

    private void validateResultWindow(ClientOperation operation, String path)
            throws QueryPolicyViolationException {
        if (maxResultWindow == null) return;
        if (hasEndpoint(path, "_msearch")) {
            throw new QueryPolicyViolationException(
                    "Multi-search pagination cannot be verified by maxResultWindow: " + path);
        }
        if (!(operation.getType() == ClientOperation.Type.SEARCH
                || path.endsWith("/_search") || path.endsWith("/_async_search"))) return;
        try {
            JsonNode body = JsonUtil.readTree(operation.getBody() == null ? "{}" : operation.getBody());
            if (body == null || !body.isObject()) {
                throw new QueryPolicyViolationException("Search body must be a JSON object");
            }
            long from = integerValue(body, "from", 0);
            long size = integerValue(body, "size", 10);
            Long queryFrom = queryInteger(operation.getPath(), "from");
            Long querySize = queryInteger(operation.getPath(), "size");
            validateWindow(queryFrom == null ? from : queryFrom,
                    querySize == null ? size : querySize);
        } catch (QueryPolicyViolationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new QueryPolicyViolationException("Search body is not valid JSON", exception);
        }
    }

    private void validateWindow(long from, long size) throws QueryPolicyViolationException {
        if (from < 0 || size < 0 || from > maxResultWindow
                || size > maxResultWindow || from > maxResultWindow - size) {
            throw new QueryPolicyViolationException("Search result window exceeds " + maxResultWindow);
        }
    }

    private static long integerValue(JsonNode object, String field, long defaultValue)
            throws QueryPolicyViolationException {
        JsonNode value = object.get(field);
        if (value == null) return defaultValue;
        if (!value.canConvertToLong() || !value.isIntegralNumber()) {
            throw new QueryPolicyViolationException("Search " + field + " must be an integer");
        }
        return value.longValue();
    }

    private static String firstPathSegment(String path) {
        String value = path.startsWith("/") ? path.substring(1) : path;
        int separator = value.indexOf('/');
        String segment = separator < 0 ? value : value.substring(0, separator);
        return segment.isEmpty() || segment.startsWith("_") ? null : segment;
    }

    private static boolean mayTargetIndicesWithoutPathIndex(String path) {
        return "/_bulk".equals(path) || "/_msearch".equals(path)
                || "/_mget".equals(path) || "/_mtermvectors".equals(path)
                || "/_search".equals(path) || path.startsWith("/_search/")
                || "/_async_search".equals(path) || path.startsWith("/_async_search/")
                || "/_field_caps".equals(path) || "/_count".equals(path)
                || "/_search_shards".equals(path) || "/_terms_enum".equals(path)
                || "/_knn_search".equals(path) || "/_rank_eval".equals(path)
                || "/_validate/query".equals(path) || path.startsWith("/_eql/search")
                || "/_sql".equals(path) || path.startsWith("/_sql/")
                || "/_delete_by_query".equals(path) || "/_update_by_query".equals(path)
                || "/_reindex".equals(path) || "/_aliases".equals(path)
                || "/_all".equals(path) || path.startsWith("/_all/");
    }

    private static boolean bodyMayOverridePathIndex(String path) {
        return hasEndpoint(path, "_bulk") || hasEndpoint(path, "_msearch")
                || hasEndpoint(path, "_mget") || hasEndpoint(path, "_mtermvectors");
    }

    private static boolean hasEndpoint(String path, String endpoint) {
        String marker = "/" + endpoint;
        int position = path.indexOf(marker);
        if (position < 0) return false;
        int end = position + marker.length();
        return end == path.length() || path.charAt(end) == '/';
    }

    private static Long queryInteger(String path, String name) throws QueryPolicyViolationException {
        int question = path.indexOf('?');
        if (question < 0) return null;
        int fragment = path.indexOf('#', question + 1);
        String query = path.substring(question + 1, fragment < 0 ? path.length() : fragment);
        Long result = null;
        for (String parameter : query.split("&")) {
            int separator = parameter.indexOf('=');
            String rawName = separator < 0 ? parameter : parameter.substring(0, separator);
            if (!name.equals(decode(rawName))) continue;
            if (result != null) {
                throw new QueryPolicyViolationException("Duplicate search query parameter: " + name);
            }
            String rawValue = separator < 0 ? "" : parameter.substring(separator + 1);
            try {
                result = Long.valueOf(decode(rawValue));
            } catch (NumberFormatException exception) {
                throw new QueryPolicyViolationException(
                        "Search query parameter " + name + " must be an integer", exception);
            }
        }
        return result;
    }

    private static String decode(String value) throws QueryPolicyViolationException {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (IllegalArgumentException | UnsupportedEncodingException exception) {
            throw new QueryPolicyViolationException("Search query parameters are not valid URL encoding", exception);
        }
    }

    private static String withoutQuery(String path) {
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        int end = query < 0 ? path.length() : query;
        if (fragment >= 0 && fragment < end) end = fragment;
        return path.substring(0, end);
    }

    private static Set<String> immutableCopy(Collection<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    public static final class Builder {
        private final Set<String> allowedIndices = new LinkedHashSet<>();
        private final Set<String> allowedMethods = new LinkedHashSet<>();
        private final Set<String> forbiddenPathPrefixes = new LinkedHashSet<>();
        private Integer maxResultWindow;

        public Builder allowIndex(String index) {
            allowedIndices.add(requireValue(index, "index"));
            return this;
        }

        public Builder allowMethod(String method) {
            allowedMethods.add(requireValue(method, "method").toUpperCase(Locale.ROOT));
            return this;
        }

        public Builder forbidPathPrefix(String path) {
            String value = requireValue(path, "path");
            forbiddenPathPrefixes.add(value.startsWith("/") ? value : "/" + value);
            return this;
        }

        public Builder maxResultWindow(int value) {
            if (value <= 0) throw new IllegalArgumentException("maxResultWindow must be positive");
            this.maxResultWindow = value;
            return this;
        }

        public RestrictedQueryPolicy build() { return new RestrictedQueryPolicy(this); }

        private static String requireValue(String value, String name) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value.trim();
        }
    }
}
