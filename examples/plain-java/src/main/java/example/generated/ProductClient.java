package example.generated;

import io.github.indexsmith.api.BaseElasticsearchRepository;
import io.github.indexsmith.api.BaseHit;
import io.github.indexsmith.api.PageSearchResponse;
import io.github.indexsmith.api.SearchQueryBuilder;
import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.IndexSmithClient;

import java.io.IOException;
import java.util.Objects;

/** Representative generated repository; production output is created by the Maven plugin. */
public class ProductClient extends BaseElasticsearchRepository<ProductModel> {
    public ProductClient(ClientConfiguration configuration) throws IOException {
        super(configuration, ProductModel.class);
    }

    public ProductClient(IndexSmithClient client) {
        super(client, ProductModel.class);
    }

    @Override
    public String getIndex() { return "products"; }

    public String buildQuery(ProductReq request) throws IOException {
        Objects.requireNonNull(request, "request");
        return SearchQueryBuilder.create()
                .page(request.getPageNo(), request.getPageSize())
                .sort(request.getSortFieldName(), request.isDescending())
                .mustMatchPhrase("name", request.getName())
                .conditions(request.getConditions())
                .sorts(request.getSorts())
                .searchAfter(request.getSearchAfter())
                .minimumShouldMatch(request.getMinimumShouldMatch())
                .sourceFilter(request.getSourceFilter())
                .highlight(request.getHighlight())
                .aggregations(request.getAggregations())
                .build();
    }

    public PageSearchResponse<BaseHit<ProductModel>> query(ProductReq request) throws IOException {
        return queryCondition(buildQuery(request), request.getAggregations());
    }
}
