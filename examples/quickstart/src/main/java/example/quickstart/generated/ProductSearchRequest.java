package example.quickstart.generated;

import io.github.indexsmith.api.PageQuery;

/** Representative request object generated for configured query fields. */
public final class ProductSearchRequest extends PageQuery {
    private String name;
    private String category;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
