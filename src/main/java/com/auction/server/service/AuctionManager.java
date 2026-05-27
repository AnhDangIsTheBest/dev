package com.auction.server.service;

import com.auction.shared.model.Auction;
import com.auction.server.exception.AuctionNotFoundException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class AuctionManager {
    private static volatile AuctionManager instance;
    private final Map<String, Auction> auctions;

    private AuctionManager() {
        this.auctions = new ConcurrentHashMap<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    public void addAuction(Auction auction) throws AuctionNotFoundException {
        if (auction == null) {
            throw new AuctionNotFoundException("Lỗi: Auction Trống");
        }
        auctions.put(auction.getAuctionId(), auction);
    }

    public Auction getAuction(String auctionId) throws AuctionNotFoundException {
        if (!auctions.containsKey(auctionId)) {
            throw new AuctionNotFoundException("Phiên đấu giá không tồn tại");
        }
        return auctions.get(auctionId);
    }

    public boolean contains(String AuctionId) {
        if (auctions.containsKey(AuctionId)) {
            return true;
        }
        return false;
    }

    public void removeAuction(String auctionId) throws AuctionNotFoundException {
        if (!auctions.containsKey(auctionId)) {
            throw new AuctionNotFoundException("Phiên đấu giá không tồn tại");
        }
        auctions.remove(auctionId);
    }

    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }

    public void clearAll() {
        auctions.clear();
    }

    public int getSize() {
        return auctions.size();
    }

}
