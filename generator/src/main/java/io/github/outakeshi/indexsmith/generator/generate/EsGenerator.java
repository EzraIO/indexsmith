package io.github.outakeshi.indexsmith.generator.generate;

import lombok.extern.slf4j.Slf4j;
import io.github.outakeshi.indexsmith.generator.model.EsConfigContext;
import io.github.outakeshi.indexsmith.generator.model.EsFieldNode;
import io.github.outakeshi.indexsmith.generator.model.GeneratorJavaFile;
import io.github.outakeshi.indexsmith.generator.util.FileUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Coordinates deterministic generation and writes the complete source set as one batch. */
@Slf4j
public final class EsGenerator {
    private EsGenerator() { }

    public static void generator(EsConfigContext context) {
        if (context == null) throw new IllegalArgumentException("context must not be null");
        try {
            List<GeneratorJavaFile> generatedFiles = new ArrayList<>();
            for (Map.Entry<String, EsFieldNode> entry : new TreeMap<>(context.getEsFieldNodeMap()).entrySet()) {
                generatedFiles.addAll(generateIndex(context, entry.getKey(), entry.getValue()));
            }
            FileUtil.writeFiles(generatedFiles, context.getWriteMode());
        } catch (Exception exception) {
            log.error("source generation failed", exception);
            throw new IllegalStateException("Failed to generate Elasticsearch sources", exception);
        }
    }

    private static List<GeneratorJavaFile> generateIndex(EsConfigContext context, String indexName,
                                                          EsFieldNode esFieldNode) {
        try {
            log.info("start generator class of index [{}]", indexName);
            log.info("start define model class of index [{}]", indexName);
            List<GeneratorJavaFile> models = new EsMappingModelGenerator().buildEsModel(
                    indexName, esFieldNode, context);
            log.info("start define model enum field of index [{}]", indexName);
            GeneratorJavaFile esFieldEnum = new EsFieldEnumGenerator().buildFieldEnum(indexName, esFieldNode, context);
            log.info("start define request example of index [{}]", indexName);
            GeneratorJavaFile request = new EsQueryExampleReqGenerator().buildReqExample(
                    indexName, esFieldNode, context, esFieldEnum);

            log.info("start define client of index [{}]", indexName);
            GeneratorJavaFile client = new EsClientGenerator().buildReqExample(indexName, esFieldNode,
                    models.get(0).getCompilationUnit(), context);


            List<GeneratorJavaFile> result = new ArrayList<>();
            result.add(esFieldEnum);
            result.add(client);
            result.add(request);
            result.addAll(models);
            return Collections.unmodifiableList(result);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate sources for index " + indexName, e);
        }
    }
}
