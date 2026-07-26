package io.github.indexsmith.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpServerVersionDetector implements ServerVersionDetector {
    private static final Pattern NUMBER = Pattern.compile("\\\"number\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    @Override
    public EsVersion detect(ClientConfiguration configuration) throws IOException {
        Objects.requireNonNull(configuration, "configuration");
        IOException failure = null;
        for (String endpoint : configuration.getEndpoints()) {
            try {
                return detect(endpoint, configuration);
            } catch (IOException exception) {
                if (failure != null) exception.addSuppressed(failure);
                failure = exception;
            }
        }
        throw failure == null ? new IOException("No Elasticsearch endpoint is configured") : failure;
    }

    private static EsVersion detect(String endpoint, ClientConfiguration configuration) throws IOException {
        URL url = new URL(endpoint + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            JdkHttpTransport.configureTls(connection, configuration);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(configuration.getConnectTimeoutMillis());
            connection.setReadTimeout(configuration.getSocketTimeoutMillis());
            if (configuration.getApiKey() != null) {
                connection.setRequestProperty("Authorization", "ApiKey " + configuration.getApiKey());
            } else if (configuration.getUsername() != null) {
                String token = configuration.getUsername() + ":" + configuration.getPassword();
                connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder()
                        .encodeToString(token.getBytes(StandardCharsets.UTF_8)));
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String body = read(stream);
            if (status < 200 || status >= 300) {
                throw new IOException("Elasticsearch version request to " + endpoint
                        + " failed with HTTP " + status + ": " + body);
            }
            Matcher matcher = NUMBER.matcher(body);
            if (!matcher.find()) {
                throw new IOException("Elasticsearch response from " + endpoint
                        + " does not contain version.number");
            }
            return EsVersion.parse(matcher.group(1));
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.toString();
    }
}
