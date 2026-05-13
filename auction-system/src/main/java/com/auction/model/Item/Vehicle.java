package com.auction.model.Item;

public class Vehicle extends Item {
    private String brand; // thương hiệu
    private String vehicleModel; // máy chủ( số khung)
    private int year; // năm sản xuất
    private  int mileage; // Số km đi được
    private String vehicleType; // Loại phương tiện

    public Vehicle(String id, String name, double  startingPrice, double currentPrice, String status, String description,
        String brand, String vehicleModel, int year, int mileage, String vehicleType){
            super(id,name,startingPrice,currentPrice,status,description);
            this.brand = brand;
            this.vehicleModel = vehicleModel;
            this.year = year;
            this.mileage = mileage;
            this.vehicleType = vehicleType;
    }

    @Override
    public String getType(){ return "Vehicle";}
    public String getBrand(){ return brand;}
    public String getVehicleModel(){ return vehicleModel;}
    public int getYear(){ return year;}
    public int getMileage(){ return mileage;}
    public String getVehicleType() { return vehicleType;}

    @Override
    public String display(){
        return super.display() + String.format(" | Hãng: %s (%s) - %s| Km: %d | Year: %d",brand,vehicleModel,vehicleType,mileage,year);
    }


    
}
