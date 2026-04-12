package com.auction.model.Item;

public class Art extends Item {
    private String artist;
    private int yearCreated;
    private String material;

    public Art(String id, String name, double startingPrice, double currentPrice, String status, String description,
               String artist, int yearCreated, String material) {
        super(id, name, startingPrice, currentPrice, status, description);
        this.artist = artist;
        this.yearCreated = yearCreated;
        this.material = material;
    }

    @Override
    public String getType() {
        return "Art";
    }

    public String getArtist() {
        return artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    public String getMaterial() {
        return material;
    }


    @Override
    public String display() {
        return super.display() + String.format(" | Họa sĩ: %s | Năm: %d | Chất liệu: %s |",
                artist, yearCreated, material);
    }
}