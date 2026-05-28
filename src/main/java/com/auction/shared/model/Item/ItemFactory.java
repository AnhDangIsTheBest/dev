package com.auction.shared.model.Item;

import java.util.UUID;

public class ItemFactory {

    public enum ItemType{
        ELECTRONICS,ART,VEHICLE,OTHERITEM;
    }

    // Tao san pham

    public static Electronics createElectronics(String name, String description, double startingPrice,String status,double currentPrice, String brand, int warranty, String model){
        String id = UUID.randomUUID().toString().substring(0,8).toUpperCase();
        return new Electronics(id,name,description,startingPrice,status,currentPrice,brand,warranty,model);
    }

    public static Art createArt(String name, double startingPrice, double currentPrice, String status, String description,
                                String artist, int yearCreated, String material){
        String id = UUID.randomUUID().toString().substring(0,8).toUpperCase();
        return new Art(id,name,startingPrice,currentPrice,status,description,artist,yearCreated,material);
    }

    public static Vehicle createVehicle(String name, double  startingPrice, double currentPrice, String status, String description,
                                        String brand, String vehicleModel, int year, int mileage, String vehicleType){
        String id = UUID.randomUUID().toString().substring(0,8).toUpperCase();
        return new Vehicle(id,name,startingPrice,currentPrice,status,description,brand,vehicleModel,year,mileage,vehicleType);
    }

    public static OtherItem createOtherItem(String name, double startingPrice, double currentPrice, String status, String description,
                                            String category){
        String id = UUID.randomUUID().toString().substring(0,8).toUpperCase();
        return new OtherItem(id,name,startingPrice,currentPrice,status,description,category);
    }

    // Tao item theo tung type

    public static Item createItem(ItemType type,
                                  String name,
                                  String description,
                                  double startingPrice,
                                  String status,
                                  double currentPrice,
                                  String... extraParams) {

        switch (type) {

            case ELECTRONICS:
                return createElectronics(
                        name,
                        description,
                        startingPrice,
                        status,
                        currentPrice,
                        extraParams.length > 0 ? extraParams[0] : "Unknown", // brand
                        extraParams.length > 1 ? Integer.parseInt(extraParams[1]) : 12, // warranty
                        extraParams.length > 2 ? extraParams[2] : "Unknown" // model
                );

            case ART:
                return createArt(
                        name,
                        startingPrice,
                        currentPrice,
                        status,
                        description,
                        extraParams.length > 0 ? extraParams[0] : "Unknown", // artist
                        extraParams.length > 1 ? Integer.parseInt(extraParams[1]) : 2024, // year
                        extraParams.length > 2 ? extraParams[2] : "Oil" // material
                );

            case VEHICLE:
                return createVehicle(
                        name,
                        startingPrice,
                        currentPrice,
                        status,
                        description,
                        extraParams.length > 0 ? extraParams[0] : "Unknown", // brand
                        extraParams.length > 1 ? extraParams[1] : "Unknown", // model
                        extraParams.length > 2 ? Integer.parseInt(extraParams[2]) : 2024, // year
                        extraParams.length > 3 ? Integer.parseInt(extraParams[3]) : 0, // mileage
                        extraParams.length > 4 ? extraParams[4] : "CAR" // type
                );

            case OTHERITEM:
                return createOtherItem(
                        name,
                        startingPrice,
                        currentPrice,
                        status,
                        description,
                        extraParams.length > 0 ? extraParams[0] : "General" // category
                );

            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }



}
