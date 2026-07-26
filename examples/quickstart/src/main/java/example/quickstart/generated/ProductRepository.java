package example.quickstart.generated;

import io.github.indexsmith.api.repository.BaseElasticsearchRepository;
import io.github.indexsmith.api.repository.BaseHit;
import io.github.indexsmith.api.result.PageSearchResponse;
import io.github.indexsmith.api.query.SearchQueryBuilder;
import io.github.indexsmith.client.IndexSmithClient;

import java.io.IOException;
import java.util.Objects;

/** Representative typed repository emitted by the IndexSmith generator. */
public final class ProductRepository extends BaseElasticsearchRepository<Product> {
    public ProductRepository(IndexSmithClient client) {
        super(client, Product.class);
    }

    @Override
    public String getIndex() {
        return "indexsmith-demo-products";
    }

    public String buildQuery(ProductSearchRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        return SearchQueryBuilder.create()
                .page(request.getPageNo(), request.getPageSize())
                .sort(request.getSortFieldName(), request.isDescending())
                .mustMatchPhrase("name", request.getName())
                .mustTerm("category", request.getCategory())
                .conditions(request.getConditions())
                .sorts(request.getSorts())
                .searchAfter(request.getSearchAfter())
                .minimumShouldMatch(request.getMinimumShouldMatch())
                .sourceFilter(request.getSourceFilter())
                .highlight(request.getHighlight())
                .aggregations(request.getAggregations())
                .build();
    }

    public PageSearchResponse<BaseHit<Product>> query(ProductSearchRequest request) throws IOException {
        return queryCondition(buildQuery(request), request.getAggregations());
    }
}
