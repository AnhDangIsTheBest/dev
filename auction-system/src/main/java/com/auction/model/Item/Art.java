package com.auction.model.Item;

public class Art extends Item{
    private String artist;
    private int yearCreated;
    private String medium; // Oil, WaterColor, Digital

    public Art(String id,String name, double startingPrice, double currentPrice, String status,String artist, int yearCreated, String medium){
        super(id,name,startingPrice, currentPrice,status);
        this.artist = artist;
        this.yearCreated = yearCreated;
        this.medium = medium;
    }

    @Override 
    public String getType(){ return "Art";}
    
    public String getArtist(){ return artist;}
    public int getYearCreated(){ return yearCreated;}
    public String getMedium(){ return medium;}


    @Override
    public String display(){
        return super.display() + String.format(" | Nghệ sĩ: %s | Năm sản xuất: %d | Chất liệu: %s ",artist,yearCreated,medium);
        
    }

}