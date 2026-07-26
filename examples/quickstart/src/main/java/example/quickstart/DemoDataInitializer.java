package example.quickstart;

import example.quickstart.generated.Product;
import example.quickstart.generated.ProductRepository;
import io.github.indexsmith.api.BaseHit;
import io.github.indexsmith.api.WriteResult;
import io.github.indexsmith.client.ClientRequest;
import io.github.indexsmith.client.ClientResponse;
import io.github.indexsmith.client.IndexSmithClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public final class DemoDataInitializer implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final IndexSmithClient client;
    private final ProductRepository products;

    public DemoDataInitializer(IndexSmithClient client, ProductRepository products) {
        this.client = client;
        this.products = products;
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        ensureIndex();

        insert("1", new Product("City Bike", "bikes", 699.00, true));
        insert("2", new Product("Mountain Bike", "bikes", 1299.00, true));
        insert("3", new Product("Bike Helmet", "accessories", 79.90, false));
        requireStatus(client.perform(ClientRequest.get("/indexsmith-demo-products/_refresh")),
                "refresh products index", 200);
        LOGGER.info("IndexSmith quickstart loaded 3 products; try GET /products?name=city%20bike");
    }

    private void ensureIndex() throws IOException {
        ClientResponse existing = client.perform(ClientRequest.get("/indexsmith-demo-products"));
        if (existing.getStatusCode() == 200) return;
        if (existing.getStatusCode() != 404) {
            throw new IllegalStateException("check demo products index failed with HTTP "
                    + existing.getStatusCode() + ": " + existing.getBody());
        }
        LOGGER.info("Creating isolated Elasticsearch index indexsmith-demo-products");
        requireStatus(client.perform(ClientRequest.json(
                        "PUT", "/indexsmith-demo-products", readMapping())),
                "create demo products index", 200);
    }

    private void insert(String id, Product product) throws IOException {
        BaseHit<Product> hit = new BaseHit<>();
        hit.setId(id);
        hit.setData(product);
        WriteResult result = products.insert(hit);
        if (!result.isSuccessful()) {
            throw new IllegalStateException("Unable to index product " + id + ": HTTP "
                    + result.getStatusCode());
        }
    }

    private static String readMapping() throws IOException {
        ClassPathResource resource = new ClassPathResource("products-mapping.json");
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private static void requireStatus(ClientResponse response, String operation, int... allowed) {
        for (int status : allowed) {
            if (response.getStatusCode() == status) return;
        }
        throw new IllegalStateException(operation + " failed with HTTP " + response.getStatusCode()
                + ": " + response.getBody());
    }
}
