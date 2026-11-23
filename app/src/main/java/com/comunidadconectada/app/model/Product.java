package com.comunidadconectada.app.model;

public class Product {
    public static final String AVAILABLE = "available";
    public static final String SOLD = "sold";

    private final String id, name, category, description, sellerUid, sellerName, unit, status;
    private final double price;
    private final long timestamp;

    public Product(String id, String name, double price, String category, String description,
                   String sellerUid, String sellerName, String unit, String status, long timestamp) {
        this.id = id; this.name = name; this.price = price; this.category = category;
        this.description = description; this.sellerUid = sellerUid; this.sellerName = sellerName;
        this.unit = unit; this.status = status; this.timestamp = timestamp;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getSellerUid() { return sellerUid; }
    public String getSellerName() { return sellerName; }
    public String getUnit() { return unit; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public boolean isSold() { return SOLD.equals(status); }
}
