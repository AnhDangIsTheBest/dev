package com.auction.model.Item;

public class Electronics extends Item{
    private String  brand;
    private String model;
    private int warranty;

    public Electronics(String id, String name, String description, double startingPrice,String status,double currentPrice, String brand, int warranty, String model){
        super(id,name,startingPrice,currentPrice,status);
        this.brand = brand ;
        this.model = model;
        this.warranty = warranty;
    }
    @Override
    public String getType(){
        return "Electronics";
    }
    public String getBrand(){ return brand;}
    public String getModel(){ return model;}
    public int getWarranty(){return warranty;}
    @Override
    public String display(){
        return super.display() + String.format(" | Hãng: %s %s | Bảo hành: %d tháng |",brand, model,warranty);
        
    }
}
