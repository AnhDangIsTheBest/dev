package com.auction.shared.model.Item;

import com.auction.shared.model.Entity;

public abstract class Item extends Entity{
    protected String description;
    protected String name;
    protected double startingPrice;
    protected double currentPrice;
    protected String status;
    protected byte[] imageData;
    protected String sellerId;

    public Item(String id,String name, double startingPrice, double currentPrice, String status,String description){
        super(id);
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice =  currentPrice;
        this.status = status;
        this.description = description;
    }
    public String getName(){
        return name;
    }
    public double getCurrentPrice() {return currentPrice;}

    public void  setCurrentPrice( double price){
        this.currentPrice = price;
    }

    public abstract String getType();
    public void setName(String name){this.name = name; }
    public String getDescription(){ return description;}
    public void setDescription(String description){ this.description = description;}
    public double getStartingPrice(){ return startingPrice;}
    public void setStartingPrice(double price){ this.startingPrice = price;}
    public String getStatus(){ return status;}
    public byte[] getImageData(){ return imageData; }
    public void setImageData(byte[] imageData){ this.imageData = imageData; }
    public String getSellerId(){ return sellerId; }
    public void setSellerId(String sellerId){ this.sellerId = sellerId; }
    public void setStatus(String status){
        this.status = status;
    }

    @Override
    public String display(){
        return String.format("[ %s ] %s | Giá khởi điểm: %.1f | Tình trạng: %s",
        getType(), description, startingPrice, status);

    }

}
