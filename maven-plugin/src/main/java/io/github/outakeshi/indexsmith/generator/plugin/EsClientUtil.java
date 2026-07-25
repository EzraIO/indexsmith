package io.github.outakeshi.indexsmith.generator.plugin;

import io.github.outakeshi.indexsmith.client.ClientConfiguration;
import io.github.outakeshi.indexsmith.client.IndexSmithClient;
import io.github.outakeshi.indexsmith.client.IndexSmithClientFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public final class EsClientUtil {
    private EsClientUtil() { }

    public static IndexSmithClient getClient(Cluster cluster) throws IOException {
        return new IndexSmithClientFactory().create(buildConfiguration(cluster.getConnection()));
    }

    static ClientConfiguration buildConfiguration(Connection connection) {
        if (connection == null) throw new IllegalArgumentException("cluster connection must be configured");
        if (StringUtils.isBlank(connection.getHostPorts())) {
            throw new IllegalArgumentException("connection.hostPorts must not be blank");
        }
        ClientConfiguration.Builder builder = ClientConfiguration.builder()
                .connectTimeoutMillis(connection.getConnectTimeOut())
                .socketTimeoutMillis(connection.getSocketTimeout())
                .connectionRequestTimeoutMillis(connection.getConnectionRequestTimeout());
        for (String host : StringUtils.split(connection.getHostPorts(), ",")) {
            builder.endpoint(host.trim());
        }
        boolean apiKeyConfigured = StringUtils.isNotBlank(connection.getApiKeyId())
                || StringUtils.isNotBlank(connection.getApiKeySecret());
        boolean basicConfigured = StringUtils.isNotBlank(connection.getUserName())
                || StringUtils.isNotBlank(connection.getPassWord());
        if (apiKeyConfigured && basicConfigured) {
            throw new IllegalArgumentException("Basic authentication and API key authentication are mutually exclusive");
        }
        if (apiKeyConfigured) {
            if (StringUtils.isBlank(connection.getApiKeyId())
                    || StringUtils.isBlank(connection.getApiKeySecret())) {
                throw new IllegalArgumentException("Both apiKeyId and apiKeySecret must be configured");
            }
            builder.apiKey(connection.getApiKeyId(), connection.getApiKeySecret());
        } else if (basicConfigured) {
            if (StringUtils.isBlank(connection.getUserName())) {
                throw new IllegalArgumentException("userName must be configured when passWord is set");
            }
            builder.credentials(connection.getUserName(), connection.getPassWord());
        }
        return builder.build();
    }
}
