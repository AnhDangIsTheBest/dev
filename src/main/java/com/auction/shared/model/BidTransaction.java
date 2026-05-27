package com.auction.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidTransaction implements Serializable {
    private String transactionId; // id phiên đấu giá
    private String auctionId; // ID cuộc đấu giá( tồn tại lâu dài đến theo sản phẩm)
    private String bidderId; // ID người đấu giá
    private String bidderName; // Tên
    private double amount; // SỐ  tiền đấu giá
    private LocalDateTime timeStamp;
    private boolean isAutoBid;

    public BidTransaction(String transactionId, String auctionId, String bidderId, String bidderName, double amount,
                          LocalDateTime timeStamp, boolean isAutoBid) {

        this.transactionId = transactionId;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timeStamp = timeStamp;
        this.isAutoBid = isAutoBid;

    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getLocalDateTime() {
        return timeStamp;
    }

    public boolean isAutoBid() {
        return isAutoBid;
    }

    public String getFormattedTimeHour() { // lấy ngày tháng theo giờ;
        return timeStamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getFormattedTimeDay() {
        return timeStamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s đặt %.0f%s", getFormattedTimeHour(), bidderName, amount, isAutoBid ? " (Auto)" : "");
    }
}
