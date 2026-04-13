package com.auction.model;

import com.auction.model.Item.Item;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;


public class Auction implements Serializable{
    private static final long serialVersionUID = 1L;

    public enum AuctionStatus{
        OPEN, RUNNING, FINISHED, PAID, CANCELLED
    }

    private String auctionId;
    private Item item;
    private double currentPrice;
    private String leadBidderId;
    private String leadBidderName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private List<BidTransaction> bidHistory;

    // Gia han phien dau
    private boolean antiSnipingEnabled;
    private int snipeWindowSeconds; // x giay cuoi
    private int snipeExtendSeconds; // gia han them y giay

    // tu dong dau gia
    private Map<String, AutoBidConfig> autoBidConfigs;

    public Auction(String auctionId, Item item, LocalDateTime startTime, LocalDateTime endTime,
                   boolean antiSnipingEnabled, int snipeWindowSeconds, int snipeExtendSeconds) {
        this.auctionId = auctionId;
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        this.leadBidderId = null;
        this.leadBidderName = "Chưa có";
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.antiSnipingEnabled = antiSnipingEnabled;
        this.snipeWindowSeconds = snipeWindowSeconds;
        this.snipeExtendSeconds = snipeExtendSeconds;
        this.autoBidConfigs = new LinkedHashMap<>();
    }

    public String getAuctionId() { return auctionId; }
    public Item getItem() { return item; }
    public double getCurrentPrice() { return currentPrice; }
    public String getLeadBidderId() { return leadBidderId; }
    public String getLeadBidderName() { return leadBidderName; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public List<BidTransaction> getBidHistory() { return Collections.unmodifiableList(bidHistory); }
    public boolean isAntiSnipingEnabled() { return antiSnipingEnabled; }
    public int getSnipeWindowSeconds() { return snipeWindowSeconds; }
    public int getSnipeExtendSeconds() { return snipeExtendSeconds; }
    public Map<String, AutoBidConfig> getAutoBidConfigs() { return autoBidConfigs; }

    public void setStatus(AuctionStatus status) { this.status = status; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    // Tinh gioi gian con lai
    public long getSecondsRemaining() {
        if (status != AuctionStatus.RUNNING) return 0;
        long remaining = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        return Math.max(0, remaining);
    }

    /**
     * Cap nhat bid moi
     */
    public void appllyBid(BidTransaction tx){
        this.currentPrice = tx.getAmount();
        this.leadBidderId = tx.getBidderId();
        this.leadBidderName = tx.getBidderName();
        this.bidHistory.add(tx);

        if (antiSnipingEnabled && getSecondsRemaining() <= snipeWindowSeconds){
            this.endTime = this.endTime.plusSeconds(snipeExtendSeconds);
        }
    }

    public void addAutoBidConfig(AutoBidConfig config) {
        autoBidConfigs.put(config.getBidderId(), config);
    }

    public void removeAutoBidConfig(String bidderId) {
        autoBidConfigs.remove(bidderId);
    }

    public boolean hasAutoBid(String bidderId) {
        return autoBidConfigs.containsKey(bidderId);
    }

    public String getSummary() {
        return String.format("Phiên: %s | %s | Giá: %.0f | Dẫn đầu: %s | Trạng thái: %s",
                auctionId, item.getName(), currentPrice, leadBidderName, status.name());
    }

    @Override
    public String toString() {
        return getSummary();
    }
}