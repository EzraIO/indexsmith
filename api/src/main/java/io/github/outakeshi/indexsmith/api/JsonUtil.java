package io.github.outakeshi.indexsmith.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/** Shared, immutable Jackson configuration for version-neutral request and response JSON. */
public final class JsonUtil {
    private static final ObjectMapper MAPPER;

    static {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        MAPPER = mapper;
    }

    private JsonUtil() { }

    public static String toJson(Object object) throws IOException {
        return MAPPER.writeValueAsString(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return MAPPER.readValue(json, clazz);
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) throws IOException {
        return MAPPER.readValue(json, typeReference);
    }

    public static JsonNode readTree(String json) throws IOException {
        return MAPPER.readTree(json);
    }

    public static <T> T treeToValue(JsonNode node, Class<T> clazz) throws IOException {
        return MAPPER.treeToValue(node, clazz);
    }

}
