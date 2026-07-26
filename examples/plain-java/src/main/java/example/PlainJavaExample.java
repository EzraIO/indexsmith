package example;

import example.generated.ProductClient;
import example.generated.ProductReq;
import io.github.indexsmith.api.RestrictedQueryPolicy;
import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.QueryPolicyViolationException;

import javax.net.ssl.SSLContext;
import java.io.IOException;

/** Run after setting ES_URL and, when required, ES_API_KEY_ID/ES_API_KEY_SECRET. */
public final class PlainJavaExample {
    private PlainJavaExample() { }

    public static void main(String[] args) {
        String endpoint = environment("ES_URL", "http://localhost:9200");
        ClientConfiguration.Builder configuration = ClientConfiguration.builder()
                .endpoint(endpoint)
                .queryPolicy(RestrictedQueryPolicy.builder()
                        .allowIndex("products")
                        .allowMethod("POST")
                        .maxResultWindow(1000)
                        .build())
                .requestListener(event -> System.out.println(event.getPhase() + " "
                        + event.getMethod() + " " + event.getPath() + " " + event.getDurationMillis() + "ms"));
        try {
            if (endpoint.startsWith("https://")) {
                // Uses the JVM trust store and keeps standard hostname verification enabled.
                configuration.sslContext(SSLContext.getDefault());
            }
            String apiKeyId = System.getenv("ES_API_KEY_ID");
            if (apiKeyId != null && !apiKeyId.trim().isEmpty()) {
                configuration.apiKey(apiKeyId, requiredEnvironment("ES_API_KEY_SECRET"));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to configure Elasticsearch TLS/authentication", exception);
        }

        try (ProductClient products = new ProductClient(configuration.build())) {
            ProductReq request = new ProductReq();
            request.setName("bike");
            request.setPageSize(10);
            System.out.println(products.query(request).getHits());
        } catch (QueryPolicyViolationException exception) {
            System.err.println("Query rejected by local policy: " + exception.getMessage());
        } catch (IOException exception) {
            System.err.println("Elasticsearch request failed: " + exception.getMessage());
        }
    }

    private static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }
}
