package com.auction.model.Item;

import com.auction.model.Entity;

public abstract class Item extends Entity{
    protected String desription;
    protected String name;
    protected double startingPrice;
    protected double currentPrice;
    protected String status;
    
    public Item(String id,String name, double startingPrice, double currentPrice, String status){
        super(id);
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice =  currentPrice;
        this.status = status;
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
    public String getDescription(){ return desription;}
    public void setDescription(String description){ this.desription = description;}
    public double getStartingPrice(){ return startingPrice;}
    public void setStartingPrice(double price){ this.startingPrice = price;}
    public String getStatus(){ return status;}
    public void setStatus(String status){
        this.status = status;
    }

    @Override
    public String display(){
        return String.format("[ %s ] %s - %s | Giá khời điểm: %.1f | Tình trạng: %s",getType(),desription,startingPrice,status);
        
    }

}
