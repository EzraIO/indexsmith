package example.quickstart;

import example.quickstart.generated.ProductRepository;
import io.github.indexsmith.api.policy.RestrictedQueryPolicy;
import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.IndexSmithClient;
import io.github.indexsmith.client.IndexSmithClientFactory;
import io.github.indexsmith.client.RequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QuickstartApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickstartApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(QuickstartApplication.class, args);
    }

    @Bean(destroyMethod = "close")
    IndexSmithClient indexSmithClient(@Value("${indexsmith.endpoint}") String endpoint) throws Exception {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .endpoint(endpoint)
                .connectTimeoutMillis(5000)
                .socketTimeoutMillis(10000)
                .queryPolicy(RestrictedQueryPolicy.builder()
                        .allowIndex("indexsmith-demo-products")
                        .allowMethod("GET")
                        .allowMethod("HEAD")
                        .allowMethod("POST")
                        .allowMethod("PUT")
                        .allowMethod("DELETE")
                        .maxResultWindow(100)
                        .forbidPathPrefix("/_cluster")
                        .build())
                .requestListener(QuickstartApplication::logRequest)
                .build();
        IndexSmithClient client = new IndexSmithClientFactory().create(configuration);
        LOGGER.info("IndexSmith selected adapter={} for Elasticsearch {}",
                client.getAdapterId(), client.getServerVersion());
        return client;
    }

    @Bean(destroyMethod = "")
    ProductRepository productRepository(IndexSmithClient client) {
        return new ProductRepository(client);
    }

    private static void logRequest(RequestEvent event) {
        if (event.getPhase() == RequestEvent.Phase.START) return;
        if (event.getPhase() == RequestEvent.Phase.FAILURE
                && event.getStatusCode() == 404
                && "/indexsmith-demo-products".equals(event.getPath())) return;
        LOGGER.info("IndexSmith request phase={} adapter={} method={} path={} status={} retries={} duration={}ms",
                event.getPhase(), event.getAdapterId(), event.getMethod(), event.getPath(),
                event.getStatusCode(), event.getRetryCount(), event.getDurationMillis());
    }
}
