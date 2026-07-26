package example.quickstart.generated;

/** Representative JavaBean generated from the products mapping. */
public final class Product {
    private String name;
    private String category;
    private Double price;
    private Boolean inStock;

    public Product() { }

    public Product(String name, String category, Double price, Boolean inStock) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.inStock = inStock;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }
}
