package com.auction.model;
import com.auction.model.Item.Item;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction implements Serializable{
    public enum AuctionStatus{
        OPEN, RUNNING, FINISHED, PAID, CANCELED;
    }
    private String auctionId;
    private Item item;
    private double currentPrice;
    private String leadBidderName;
    private String leadBidderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private List<BidTransaction> bidHistory; // Lịch sử người đấu giá

   
   
    // chống những người chờ đến cuối mới bid
    private boolean antiSnipingEnable;
    private int snipeWindowSeconds; // chống bid trong x giây cuối
    private int snipeExtendSeconds; // gia  hạn thêm y giây để bid



    // AutoBid congigs ( sắp xếp theo thời gian đăng ký chế độ auto)
    private Map<String,AutoBidConfig> autoBidConfigs;
    public Auction(String auctionId,  Item item, LocalDateTime startTime,
                    LocalDateTime endTime, String leadBidderName, String leadBidderId,
                    boolean antiSnipingEnable, int snipeWindowSeconds, int snipeExtendSeconds){
        
        this.auctionId = auctionId;
        this.currentPrice = item.getStartingPrice();
        this.item = item;
        this.leadBidderId = null;
        this.leadBidderName = "Hiện Trống ❌";
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new CopyOnWriteArrayList<>();
        this.antiSnipingEnable =  antiSnipingEnable;
        this.snipeWindowSeconds = snipeWindowSeconds;
        this.snipeExtendSeconds = snipeExtendSeconds;
        this.autoBidConfigs = new LinkedHashMap<>();
    }

    public String getAuctionId(){ return auctionId;}
    public Item getItem(){ return item;}
    public String getLeadBidderId(){ return leadBidderId;}
    public String getLeadBidderName() { return leadBidderName;}
    public LocalDateTime getStartTime(){ return startTime;}
    public LocalDateTime getEndTime(){ return endTime;}
    public AuctionStatus getStatus() { return status;}
    public List<BidTransaction> getBidHistory(){ return bidHistory;}
    public boolean isAntiSnipingEnabled(){ return antiSnipingEnable;}
    public int snipeWindowSeconds(){ return snipeWindowSeconds;}
    public int snipeExtendSeconds(){ return snipeExtendSeconds;}
    public Map<String,AutoBidConfig> getAutoBidConfigs(){ return autoBidConfigs;}

    public void setStatus(AuctionStatus status){ this.status = status;}
    public void setEndTime(LocalDateTime endTime){ this.endTime =  endTime;}

    public long getSecondRemaining(){ // thời gian còn lại trước khi phiên đấu giá kết thúc
        if (status != AuctionStatus.RUNNING){ return 0;}
        long remaining = java.time.Duration.between(LocalDateTime.now(),endTime).getSeconds();
        return Math.max(0,remaining);
    }

    public void applyBid(BidTransaction lead){ // Update leader
        this.currentPrice = lead.getAmount();
        this.leadBidderId  = lead.getBidderId();
        this.leadBidderName = lead.getBidderName();
        this.bidHistory.add(lead);

        if(antiSnipingEnable && getSecondRemaining() <= snipeWindowSeconds){
            this.endTime =  this.endTime.plusSeconds(snipeExtendSeconds);
        }
    }

    public void addAutoBidConfig(AutoBidConfig newConfig){
        autoBidConfigs.put(newConfig.getBidderId(),newConfig);
    }


    public void removeAutoBidConfig(String bidderId){
        autoBidConfigs.remove(bidderId);
    }


    public boolean hasAutoBid(String bidderId){
        return autoBidConfigs.containsKey(bidderId);
    }

    public String display(){
        return String.format("Phiên: %s | %s | Giá hiện tại: %.0f | Người trả giá: %s | Trạng thái: %s  ",
                            auctionId,item.getName(),currentPrice,leadBidderName,status.name());

    }

    public String toString(){
        return display();
        
    }
}
