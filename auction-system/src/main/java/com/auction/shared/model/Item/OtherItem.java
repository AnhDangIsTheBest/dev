package com.auction.shared.model.Item;


public class OtherItem extends Item {
    private String category;

    public OtherItem(String id, String name, double startingPrice, double currentPrice, String status, String description,
                     String category) {
        super(id, name, startingPrice, currentPrice, status, description);
        this.category = category;
    }

    @Override
    public String getType() {
        return "Other";
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String display() {
        return super.display() + String.format(" | Danh mục: %s |", category);
    }
}
