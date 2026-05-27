package com.auction.shared.model;

import com.auction.shared.model.Item.OtherItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionTest {

    @Test
    void applyBidUpdatesLeaderAndPrice() {
        Auction auction = createAuction(false, 30, 60);
        BidTransaction bid = new BidTransaction(
                "TX001",
                "AUC001",
                "BID001",
                "Alice",
                150_000,
                LocalDateTime.now(),
                false
        );

        auction.applyBid(bid);

        assertEquals(150_000, auction.getCurrentPrice());
        assertEquals("BID001", auction.getLeadBidderId());
        assertEquals("Alice", auction.getLeadBidderName());
        assertEquals(1, auction.getBidHistory().size());
    }

    @Test
    void applyBidExtendsAuctionWhenAntiSnipingIsActive() {
        Auction auction = createAuction(true, 60, 120);
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(30);
        auction.setEndTime(originalEndTime);

        BidTransaction bid = new BidTransaction(
                "TX002",
                "AUC001",
                "BID002",
                "Bob",
                175_000,
                LocalDateTime.now(),
                false
        );

        auction.applyBid(bid);

        assertTrue(auction.getEndTime().isAfter(originalEndTime));
        assertEquals(originalEndTime.plusSeconds(120), auction.getEndTime());
    }

    private Auction createAuction(boolean antiSnipingEnabled, int snipeWindowSeconds, int snipeExtendSeconds) {
        OtherItem item = new OtherItem(
                "ITEM001",
                "Test item",
                100_000,
                100_000,
                "AVAILABLE",
                "Used for CI tests",
                "Other"
        );

        return new Auction(
                "AUC001",
                item,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(5),
                null,
                null,
                antiSnipingEnabled,
                snipeWindowSeconds,
                snipeExtendSeconds
        );
    }
}
