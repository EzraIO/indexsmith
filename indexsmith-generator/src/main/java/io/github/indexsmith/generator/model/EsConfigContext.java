package io.github.indexsmith.generator.model;

import com.github.javaparser.ast.expr.Name;
import io.github.indexsmith.api.json.JsonUtil;
import io.github.indexsmith.client.ClientRequest;
import io.github.indexsmith.client.ClientResponse;
import io.github.indexsmith.client.IndexSmithClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import io.github.indexsmith.generator.util.EsMappingParseUtil;

import java.io.IOException;
import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 */
@Slf4j
@Setter
@NoArgsConstructor
public class EsConfigContext implements Closeable {

    private IndexSmithClient esClient;

    @Getter
    private Name modelTargetPackage;

    @Getter
    private  Map<String, EsIndexInfo> esIndexInfoMap;

    @Getter
    private Name clientTargetPackage;


    private String targetProject;

    @Getter
    private Name fieldEnumTargetPackage;

    @Getter
    private Name reqTargetPackage;

    @Getter
    private Map<String, EsFieldNode> esFieldNodeMap;

    @Getter
    private WriteMode writeMode = WriteMode.FAIL;


    public EsConfigContext(List<EsIndexInfo> indexInfos, IndexSmithClient esClient) throws IOException {
        if (indexInfos == null || indexInfos.isEmpty()) {
            throw new IllegalArgumentException("At least one index must be configured");
        }
        this.esClient = Objects.requireNonNull(esClient, "esClient");
        this.esIndexInfoMap = new LinkedHashMap<>();
        for (EsIndexInfo indexInfo : indexInfos) {
            if (indexInfo == null || StringUtils.isBlank(indexInfo.getIndexNames())) {
                throw new IllegalArgumentException("Configured index name must not be blank");
            }
            String indexName = indexInfo.getIndexNames().trim();
            if (esIndexInfoMap.put(indexName, indexInfo) != null) {
                throw new IllegalArgumentException("Duplicate configured index: " + indexName);
            }
        }

        String indexes = String.join(",", esIndexInfoMap.keySet());
        ClientResponse response = esClient.perform(ClientRequest.get("/" + indexes));
        if (!response.isSuccessful()) {
            throw new IOException("Unable to read Elasticsearch mappings, HTTP " + response.getStatusCode()
                    + ": " + response.getBody());
        }
        JsonNode responseBody = JsonUtil.readTree(response.getBody());
        this.esFieldNodeMap = new LinkedHashMap<>();
        for (String indexName : esIndexInfoMap.keySet()) {
            JsonNode mappingNode = responseBody.path(indexName).path("mappings");
            if (mappingNode.isMissingNode()) {
                throw new IllegalStateException("Mapping response does not contain index " + indexName);
            }
            String mapping = mappingNode.toString();
            log.info("index:{},mapping:{}", indexName, mapping);
            esFieldNodeMap.put(indexName,
                    EsMappingParseUtil.parse2Node(mapping, esIndexInfoMap.get(indexName).getArrFieldPath()));
        }
    }

    public boolean isCondition(String indexName, List<String> path, String currentPath) {
        EsIndexInfo esIndexInfo = esIndexInfoMap.get(indexName);
        if (Objects.isNull(esIndexInfo)){
            return false;
        }
        Set<String> queryConditionFields = esIndexInfo.getQueryConditionFields();
        if (queryConditionFields == null || queryConditionFields.isEmpty()) {
            return true;
        }
        String pathStr = path.stream().reduce((o1, o2) -> o1 + "." + o2).orElse(StringUtils.EMPTY);
        if (StringUtils.isEmpty(pathStr)) {
            return queryConditionFields.contains(currentPath);
        }
        return queryConditionFields.contains(pathStr + "." + currentPath);
    }

    public EsFieldNode getFieldNode(String indexName) {
        return esFieldNodeMap.get(indexName);
    }

    public IndexSmithClient getEsClient() {
        return esClient;
    }

    @Override
    public void close() throws IOException {
        if (esClient != null) {
            esClient.close();
        }
    }


    public void setReqTargetPackage(String reqTargetPackage) {
        this.reqTargetPackage = new Name(reqTargetPackage);
    }

    public String getTargetProject() {
        return targetProject;
    }


    public void setFieldEnumTargetPackage(String fieldEnumTargetPackage) {
        this.fieldEnumTargetPackage = new Name(fieldEnumTargetPackage);
    }

    public void setClientTargetPackage(String clientTargetPackage) {
        this.clientTargetPackage = new Name(clientTargetPackage);
    }

    public void setModelTargetPackage(String modelTargetPackage) {
        this.modelTargetPackage = new Name(modelTargetPackage);
    }

    public void setWriteMode(WriteMode writeMode) {
        this.writeMode = writeMode == null ? WriteMode.FAIL : writeMode;
    }
}
