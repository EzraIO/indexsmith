package example.generated.request;

import io.github.indexsmith.api.PageQuery;

/** Representative output from the query request generator. */
public class OrdersReq extends PageQuery {
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
