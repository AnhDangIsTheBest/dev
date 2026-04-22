package com.auction.service;

public class AuctionManager {
    public static volatile AuctionManager instance;

    private AuctionManager(){}

    public static AuctionManager getInstance(){
        if (instance == null){
            synchronized (AuctionManager.class){
                if (instance == null){
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }
}
