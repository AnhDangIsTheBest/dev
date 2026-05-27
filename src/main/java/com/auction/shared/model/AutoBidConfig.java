package com.auction.shared.model;

import java.io.Serializable;

public class AutoBidConfig implements Serializable, Comparable<AutoBidConfig> {
    private String bidderId;
    private String bidderName;
    private double maxBid;
    private double increment;
    private long registeredAt; //timeStamp đăng ký( ưu tiên người đki trước)

    public AutoBidConfig(String bidderId, String bidderName, double maxBid,
                         double increment, long registeredAt) {

        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = registeredAt;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public double getIncrement() {
        return increment;
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    // So sánh độ ưu tiên
    @Override
    public int compareTo(AutoBidConfig other) {
        return Long.compare(this.registeredAt, other.registeredAt);
    }

    @Override
    public String toString() {
        return String.format("AutoBid[%s(%s): max = %.0f |  inc = %.0f]", bidderName, bidderId, maxBid, increment);

    }


}
