package io.github.indexsmith.generator.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves explicit environment placeholders without exposing secret values. */
final class ConfigurationInterpolator {
    private static final Pattern ENVIRONMENT = Pattern.compile("\\$\\{env\\.([A-Za-z_][A-Za-z0-9_]*)}");

    private ConfigurationInterpolator() { }

    static void interpolate(GeneratorConfiguration configuration) {
        interpolate(configuration, System.getenv());
    }

    static void interpolate(GeneratorConfiguration configuration, Map<String, String> environment) {
        if (configuration == null) return;
        Map<String, String> values = environment == null ? Collections.emptyMap() : environment;
        configuration.setSourceDir(resolve(configuration.getSourceDir(), values));
        configuration.setWriteMode(resolve(configuration.getWriteMode(), values));
        TargetPackage targetPackage = configuration.getTargetPackage();
        if (targetPackage != null) {
            targetPackage.setModel(resolve(targetPackage.getModel(), values));
            targetPackage.setFieldEnum(resolve(targetPackage.getFieldEnum(), values));
            targetPackage.setEsQueryModel(resolve(targetPackage.getEsQueryModel(), values));
            targetPackage.setClient(resolve(targetPackage.getClient(), values));
        }
        if (configuration.getClusters() == null) return;
        for (Cluster cluster : configuration.getClusters()) {
            if (cluster == null) continue;
            Connection connection = cluster.getConnection();
            if (connection != null) {
                connection.setHostPorts(resolve(connection.getHostPorts(), values));
                connection.setUserName(resolve(connection.getUserName(), values));
                connection.setPassWord(resolve(connection.getPassWord(), values));
                connection.setApiKeyId(resolve(connection.getApiKeyId(), values));
                connection.setApiKeySecret(resolve(connection.getApiKeySecret(), values));
            }
            if (cluster.getIndexes() == null) continue;
            for (IndexInfo index : cluster.getIndexes()) {
                if (index == null) continue;
                index.setName(resolve(index.getName(), values));
                index.setArrFields(resolve(index.getArrFields(), values));
                index.setQueryConditionFields(resolve(index.getQueryConditionFields(), values));
            }
        }
    }

    static String resolve(String input, Map<String, String> environment) {
        if (input == null) return null;
        Matcher matcher = ENVIRONMENT.matcher(input);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = environment.get(name);
            if (value == null) {
                throw new IllegalArgumentException("Environment variable is not defined: " + name);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        if (result.indexOf("${env.") >= 0) {
            throw new IllegalArgumentException("Invalid environment placeholder syntax");
        }
        return result.toString();
    }
}
