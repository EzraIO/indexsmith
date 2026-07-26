package io.github.indexsmith.client.transport;

import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.request.ClientRequest;
import io.github.indexsmith.client.response.ClientResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.net.ssl.HttpsURLConnection;

/** Java 8 HTTP transport used where a version-specific binary client is unnecessary. */
public final class JdkHttpTransport {
    private final ClientConfiguration configuration;

    public JdkHttpTransport(ClientConfiguration configuration) {
        this.configuration = configuration;
    }

    public ClientResponse perform(ClientRequest request) throws IOException {
        IOException lastFailure = null;
        ClientResponse lastRetryableResponse = null;
        int attempt = 0;
        for (String endpoint : configuration.getEndpoints()) {
            try {
                ClientResponse response = perform(endpoint, request);
                ClientResponse observed = new ClientResponse(response.getStatusCode(), response.getBody(), attempt);
                if (!request.isRetryable() || !isRetryable(response.getStatusCode())) return observed;
                lastRetryableResponse = observed;
            } catch (IOException exception) {
                if (!request.isRetryable()) throw exception;
                if (lastFailure != null) exception.addSuppressed(lastFailure);
                lastFailure = exception;
            }
            attempt++;
        }
        if (lastRetryableResponse != null) {
            return new ClientResponse(lastRetryableResponse.getStatusCode(), lastRetryableResponse.getBody(),
                    Math.max(0, attempt - 1));
        }
        throw lastFailure == null ? new IOException("No Elasticsearch endpoint is configured") : lastFailure;
    }

    private static boolean isRetryable(int statusCode) {
        return statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private ClientResponse perform(String endpoint, ClientRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + request.getPath()).openConnection();
        try {
            configureTls(connection, configuration);
            connection.setRequestMethod(request.getMethod());
            connection.setConnectTimeout(configuration.getConnectTimeoutMillis());
            connection.setReadTimeout(configuration.getSocketTimeoutMillis());
            connection.setRequestProperty("Accept", ClientRequest.JSON);
            connection.setRequestProperty("Content-Type", request.getContentType());
            if (configuration.getApiKey() != null) {
                connection.setRequestProperty("Authorization", "ApiKey " + configuration.getApiKey());
            } else if (configuration.getUsername() != null) {
                String token = configuration.getUsername() + ":"
                        + (configuration.getPassword() == null ? "" : configuration.getPassword());
                connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder()
                        .encodeToString(token.getBytes(StandardCharsets.UTF_8)));
            }
            if (request.getBody() != null) {
                connection.setDoOutput(true);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(request.getBody().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new ClientResponse(status, read(stream));
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream input) throws IOException {
        if (input == null) return "";
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public static void configureTls(HttpURLConnection connection, ClientConfiguration configuration) {
        if (connection instanceof HttpsURLConnection && configuration.getSslContext() != null) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(
                    configuration.getSslContext().getSocketFactory());
        }
    }
}
