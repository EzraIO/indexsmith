package io.github.indexsmith.generator.plugin;

import io.github.indexsmith.generator.model.EsConfigContext;
import io.github.indexsmith.generator.model.EsIndexInfo;
import io.github.indexsmith.generator.model.WriteMode;
import io.github.indexsmith.client.IndexSmithClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/** Converts Maven-plugin configuration into generator runtime models. */
public final class BeanConvertUtil {

    private BeanConvertUtil() { }

    public static EsConfigContext buildEsContext(MavenProject project, GeneratorConfiguration generatorConfiguration,
                                                 Cluster cluster) {
        validateGeneratorConfiguration(project, generatorConfiguration, cluster);
        List<IndexInfo> indexes = cluster.getIndexes();
        List<EsIndexInfo> esIndexInfos = indexes.stream()
                .map(BeanConvertUtil::convert2EsIndexInfo)
                .collect(Collectors.toList());

        IndexSmithClient client = null;
        try {
            client = EsClientUtil.getClient(cluster);
            EsConfigContext esConfigContext = new EsConfigContext(esIndexInfos, client);

            esConfigContext.setTargetProject(resolveOutputDirectory(project, generatorConfiguration.getSourceDir()));
            TargetPackage targetPackage = generatorConfiguration.getTargetPackage();
            esConfigContext.setModelTargetPackage(targetPackage.getModel());
            esConfigContext.setClientTargetPackage(targetPackage.getClient());
            esConfigContext.setFieldEnumTargetPackage(targetPackage.getFieldEnum());
            esConfigContext.setReqTargetPackage(targetPackage.getEsQueryModel());
            esConfigContext.setWriteMode(WriteMode.parse(generatorConfiguration.getWriteMode()));
            return esConfigContext;
        } catch (Exception e) {
            try {
                if (client != null) client.close();
            } catch (Exception closeException) {
                e.addSuppressed(closeException);
            }
            throw new IllegalStateException("Unable to create the generator context", e);
        }


    }

    static String resolveOutputDirectory(MavenProject project, String configuredDirectory) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(project.getBasedir(), "project base directory");
        if (StringUtils.isBlank(configuredDirectory)) {
            throw new IllegalArgumentException("sourceDir must not be blank");
        }
        Path relative = Paths.get(configuredDirectory.trim());
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("sourceDir must be relative to the Maven project");
        }
        Path base = project.getBasedir().toPath().toAbsolutePath().normalize();
        Path output = base.resolve(relative).normalize();
        if (!output.startsWith(base)) {
            throw new IllegalArgumentException("sourceDir must remain inside the Maven project");
        }
        return output.toString();
    }


    public static EsIndexInfo convert2EsIndexInfo(IndexInfo indexInfo) {
        if (indexInfo == null || StringUtils.isBlank(indexInfo.getName())) {
            throw new IllegalArgumentException("index name must not be blank");
        }
        EsIndexInfo esIndexInfo = new EsIndexInfo();
        esIndexInfo.setIndexNames(indexInfo.getName().trim());
        esIndexInfo.setArrFieldPath(split2Set(indexInfo.getArrFields()));
        esIndexInfo.setQueryConditionFields(split2Set(indexInfo.getQueryConditionFields()));
        return esIndexInfo;
    }


    private static Set<String> split2Set(String str) {
        if (StringUtils.isEmpty(str)) {
            return Collections.emptySet();
        }
        return Arrays.stream(StringUtils.split(str, ","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    private static void validateGeneratorConfiguration(MavenProject project,
                                                       GeneratorConfiguration configuration,
                                                       Cluster cluster) {
        Objects.requireNonNull(project, "project");
        if (project.getBasedir() == null) throw new IllegalArgumentException("Maven project base directory is missing");
        Objects.requireNonNull(configuration, "generatorConfiguration");
        if (StringUtils.isBlank(configuration.getSourceDir())) {
            throw new IllegalArgumentException("sourceDir must not be blank");
        }
        TargetPackage packages = Objects.requireNonNull(configuration.getTargetPackage(), "targetPackage");
        if (StringUtils.isAnyBlank(packages.getModel(), packages.getClient(),
                packages.getFieldEnum(), packages.getEsQueryModel())) {
            throw new IllegalArgumentException("All targetPackage values must be configured");
        }
        Objects.requireNonNull(cluster, "cluster");
        if (cluster.getIndexes() == null || cluster.getIndexes().isEmpty()) {
            throw new IllegalArgumentException("At least one index must be configured for each cluster");
        }
    }
}
