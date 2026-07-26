package example.generated.client;

import example.generated.model.OrdersModel;
import example.generated.request.OrdersReq;
import io.github.indexsmith.api.repository.BaseElasticsearchRepository;
import io.github.indexsmith.api.repository.BaseHit;
import io.github.indexsmith.api.result.PageSearchResponse;
import io.github.indexsmith.api.query.SearchQueryBuilder;
import io.github.indexsmith.client.ClientConfiguration;
import io.github.indexsmith.client.IndexSmithClient;

import java.io.IOException;
import java.util.Objects;

/** Representative output from the repository generator. */
public class OrdersClient extends BaseElasticsearchRepository<OrdersModel> {
    public OrdersClient(ClientConfiguration configuration) throws IOException {
        super(configuration, OrdersModel.class);
    }

    public OrdersClient(IndexSmithClient client) {
        super(client, OrdersModel.class);
    }

    @Override
    public String getIndex() {
        return "orders";
    }

    public String buildQuery(OrdersReq request) throws IOException {
        Objects.requireNonNull(request, "request");
        SearchQueryBuilder queryBuilder = SearchQueryBuilder.create();
        queryBuilder.page(request.getPageNo(), request.getPageSize());
        queryBuilder.sort(request.getSortFieldName(), request.isDescending());
        queryBuilder.mustTerm("status", request.getStatus());
        queryBuilder.conditions(request.getConditions());
        queryBuilder.sorts(request.getSorts());
        queryBuilder.searchAfter(request.getSearchAfter());
        queryBuilder.minimumShouldMatch(request.getMinimumShouldMatch());
        queryBuilder.sourceFilter(request.getSourceFilter());
        queryBuilder.highlight(request.getHighlight());
        queryBuilder.aggregations(request.getAggregations());
        return queryBuilder.build();
    }

    public PageSearchResponse<BaseHit<OrdersModel>> query(OrdersReq request) throws IOException {
        return queryCondition(buildQuery(request), request.getAggregations());
    }
}
