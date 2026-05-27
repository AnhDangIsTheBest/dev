package com.auction.shared.model;

import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.Item.Electronics;
import com.auction.shared.model.Item.Item;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionModelTest {
    @Test
    void constructorInitializesAuctionFromItem() {
        Auction auction = auction("A1", false);

        assertEquals("A1", auction.getAuctionId());
        assertEquals(100.0, auction.getCurrentPrice());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertTrue(auction.getBidHistory().isEmpty());
        assertTrue(auction.getAutoBidConfigs().isEmpty());
        assertTrue(auction.display().contains("A1"));
        assertEquals(auction.display(), auction.toString());
    }

    @Test
    void applyBidUpdatesLeaderPriceAndHistory() {
        Auction auction = auction("A2", false);
        BidTransaction bid = bid("B1", "A2", "U1", "An", 150.0);

        auction.applyBid(bid);

        assertEquals(150.0, auction.getCurrentPrice());
        assertEquals("U1", auction.getLeadBidderId());
        assertEquals("An", auction.getLeadBidderName());
        assertEquals(List.of(bid), auction.getBidHistory());
    }

    @Test
    void applyBidExtendsEndTimeWhenAntiSnipingWindowIsActive() {
        Auction auction = auction("A3", true);
        LocalDateTime originalEndTime = LocalDateTime.now().plusSeconds(20);
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setEndTime(originalEndTime);

        auction.applyBid(bid("B1", "A3", "U1", "An", 150.0));

        assertEquals(originalEndTime.plusSeconds(60), auction.getEndTime());
    }

    @Test
    void secondRemainingIsOnlyAvailableForRunningAuctions() {
        Auction auction = auction("A4", false);
        auction.setEndTime(LocalDateTime.now().plusMinutes(1));

        assertEquals(0, auction.getSecondRemaining());

        auction.setStatus(AuctionStatus.RUNNING);

        assertTrue(auction.getSecondRemaining() > 0);
    }

    @Test
    void settersReplaceMutableState() {
        Auction auction = auction("A5", false);
        BidTransaction first = bid("B1", "A5", "U1", "An", 150.0);
        BidTransaction second = bid("B2", "A5", "U2", "Binh", 175.0);

        auction.setCurrentPrice(125.0);
        auction.setLeadBidder("U1", "An");
        auction.setBidHistory(List.of(first, second));
        auction.setBidHistory(null);
        auction.setLeadBidder("U2", " ");

        assertEquals(125.0, auction.getCurrentPrice());
        assertEquals("U2", auction.getLeadBidderId());
        assertNotEquals(" ", auction.getLeadBidderName());
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void autoBidConfigMapCanAddCheckAndRemoveBidderConfig() {
        Auction auction = auction("A6", false);
        AutoBidConfig config = new AutoBidConfig("U1", "An", 200.0, 10.0, 1L);

        auction.addAutoBidConfig(config);

        assertTrue(auction.hasAutoBid("U1"));
        assertEquals(config, auction.getAutoBidConfigs().get("U1"));

        auction.removeAutoBidConfig("U1");

        assertFalse(auction.hasAutoBid("U1"));
    }

    private static Auction auction(String id, boolean antiSniping) {
        return new Auction(
                id,
                item(),
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10),
                null,
                null,
                antiSniping,
                30,
                60
        );
    }

    private static Item item() {
        Electronics item = new Electronics("ITEM-1", "Laptop", "Gaming laptop", 100.0, "NEW", 100.0,
                "Dell", 24, "G15");
        item.setSellerId("SELLER");
        return item;
    }

    private static BidTransaction bid(String id, String auctionId, String bidderId, String bidderName, double amount) {
        return new BidTransaction(id, auctionId, bidderId, bidderName, amount, LocalDateTime.of(2026, 5, 18, 10, 0),
                false);
    }
}
