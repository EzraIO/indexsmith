package io.github.indexsmith.generator.util;

import io.github.indexsmith.api.JsonUtil;
import io.github.indexsmith.generator.model.EsField;
import io.github.indexsmith.generator.model.EsFieldNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 */
public final class EsMappingParseUtil {

    private static final String ES_FIELD_NODE_KEY = "properties";

    private static final String ES_FIELD_NODE_TYPE = "type";

    private EsMappingParseUtil() { }

    private static EsFieldNode buildEsFieldNode(JsonNode properties, String parentPath,
                                                 Set<String> arrayPaths) throws IOException {
        EsFieldNode.EsFieldNodeBuilder builder = EsFieldNode.builder();
        Map<String, EsField> fieldMap = new LinkedHashMap<>();
        Map<String, EsFieldNode> nodeMap = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            JsonNode definition = entry.getValue();
            if (!definition.isObject()) {
                throw new IOException("Invalid mapping definition for field "
                        + getCurrentPath(parentPath, fieldName));
            }
            String fieldPath = getCurrentPath(parentPath, fieldName);
            String fieldType = definition.path(ES_FIELD_NODE_TYPE).asText(null);
            JsonNode childProperties = definition.path(ES_FIELD_NODE_KEY);
            boolean objectNode = childProperties.isObject()
                    || "object".equals(fieldType) || "nested".equals(fieldType);
            if (objectNode) {
                EsFieldNode child = buildEsFieldNode(childProperties.isObject()
                        ? childProperties : JsonUtil.readTree("{}"), fieldPath, arrayPaths);
                child.setEsType(fieldType == null ? "object" : fieldType);
                child.setArrayField(arrayPaths.contains(fieldPath));
                nodeMap.put(fieldName, child);
            } else {
                if (StringUtils.isBlank(fieldType)) {
                    throw new IOException("Mapping field " + fieldPath + " does not define a type");
                }
                EsField esField = EsField.builder()
                        .arrayField(arrayPaths.contains(fieldPath))
                        .esType(fieldType).build();
                fieldMap.put(fieldName, esField);
            }
        }
        builder.nodeMap(nodeMap);
        builder.fieldMap(fieldMap);
        return builder.build();
    }

    private static String getCurrentPath(String parentPath, String o) {
        if (StringUtils.isEmpty(parentPath)) {
            return o;
        }
        return parentPath + "." + o;
    }

    public static EsFieldNode parse2Node(String mappingJson, Set<String> arrPath) throws IOException {
        JsonNode mapping = JsonUtil.readTree(mappingJson);
        JsonNode properties = mapping.path(ES_FIELD_NODE_KEY);
        if (!properties.isObject()) {
            throw new IOException("Elasticsearch mapping does not contain an object 'properties' node");
        }
        return buildEsFieldNode(properties, "", arrPath == null ? Collections.emptySet() : arrPath);
    }
}
