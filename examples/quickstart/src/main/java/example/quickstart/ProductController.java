package example.quickstart;

import example.quickstart.generated.Product;
import example.quickstart.generated.ProductRepository;
import example.quickstart.generated.ProductSearchRequest;
import io.github.indexsmith.api.repository.BaseHit;
import io.github.indexsmith.api.json.JsonUtil;
import io.github.indexsmith.api.result.PageSearchResponse;
import io.github.indexsmith.client.IndexSmithClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public final class ProductController {
    private final IndexSmithClient client;
    private final ProductRepository products;

    public ProductController(IndexSmithClient client, ProductRepository products) {
        this.client = client;
        this.products = products;
    }

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("project", "IndexSmith for Elasticsearch");
        response.put("adapter", client.getAdapterId());
        response.put("serverVersion", client.getServerVersion().toString());
        response.put("try", Arrays.asList("/products", "/products?name=city%20bike", "/products?category=bikes"));
        return response;
    }

    @GetMapping("/products")
    public Map<String, Object> search(@RequestParam(defaultValue = "") String name,
                                      @RequestParam(defaultValue = "") String category,
                                      @RequestParam(defaultValue = "10") int size) throws IOException {
        if (size < 1 || size > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "size must be between 1 and 50");
        }
        ProductSearchRequest request = new ProductSearchRequest();
        request.setName(name);
        request.setCategory(category);
        request.setPageSize(size);

        String query = products.buildQuery(request);
        PageSearchResponse<BaseHit<Product>> result = products.query(request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("adapter", client.getAdapterId());
        response.put("serverVersion", client.getServerVersion().toString());
        response.put("generatedQuery", JsonUtil.readTree(query));
        response.put("totalHits", result.getTotalHits());
        response.put("hits", result.getHits());
        return response;
    }
}
