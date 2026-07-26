package example;

import example.generated.ProductClient;
import example.generated.ProductReq;
import io.github.indexsmith.api.policy.RestrictedQueryPolicy;
import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.client.IndexSmithClientFactory;
import io.github.indexsmith.client.policy.QueryPolicyViolationException;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import javax.net.ssl.SSLContext;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean(destroyMethod = "close")
    IndexSmithClient elasticsearchClient(
            @Value("${indexsmith.endpoint:http://localhost:9200}") String endpoint,
            @Value("${indexsmith.api-key-id:}") String apiKeyId,
            @Value("${indexsmith.api-key-secret:}") String apiKeySecret) throws Exception {
        ClientConfiguration.Builder builder = ClientConfiguration.builder()
                .endpoint(endpoint)
                .queryPolicy(RestrictedQueryPolicy.builder()
                        .allowIndex("products")
                        .allowMethod("GET")
                        .allowMethod("POST")
                        .allowMethod("PUT")
                        .allowMethod("DELETE")
                        .maxResultWindow(1000)
                        .build())
                .requestListener(event -> System.out.println("elasticsearch " + event.getPhase()
                        + " " + event.getMethod() + " " + event.getPath()));
        if (endpoint.startsWith("https://")) builder.sslContext(SSLContext.getDefault());
        if (!apiKeyId.trim().isEmpty()) {
            if (apiKeySecret.trim().isEmpty()) {
                throw new IllegalArgumentException("indexsmith.api-key-secret must not be blank");
            }
            builder.apiKey(apiKeyId, apiKeySecret);
        }
        return new IndexSmithClientFactory().create(builder.build());
    }

    @Bean(destroyMethod = "")
    ProductClient productClient(IndexSmithClient client) {
        return new ProductClient(client);
    }

    @Bean
    ApplicationRunner optionalSearch(ProductClient products,
                                     @Value("${indexsmith.run-example:false}") boolean runExample) {
        return arguments -> {
            if (!runExample) return;
            try {
                ProductReq request = new ProductReq();
                request.setName("bike");
                request.setPageSize(10);
                products.query(request);
            } catch (QueryPolicyViolationException exception) {
                System.err.println("Query rejected by local policy: " + exception.getMessage());
            } catch (IOException exception) {
                System.err.println("Elasticsearch request failed: " + exception.getMessage());
            }
        };
    }

}
