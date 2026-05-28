package com.auction.server.service;

import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.AutoBidConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BidServiceTest {
    private FakeAuctionDAO auctionDAO;
    private FakeBidDAO bidDAO;
    private FakeAutoBidDAO autoBidDAO;
    private FakeUserDAO userDAO;
    private BidService service;

    @BeforeEach
    void setUp() {
        auctionDAO = new FakeAuctionDAO();
        bidDAO = new FakeBidDAO();
        autoBidDAO = new FakeAutoBidDAO();
        userDAO = new FakeUserDAO();
        service = new BidService(auctionDAO, bidDAO, autoBidDAO, userDAO);
    }

    @Test
    void placeManualBidAcceptsHigherBidAndStartsOpenAuction() {
        Auction auction = TestData.auction("A1", "SELLER");
        auctionDAO.add(auction);

        String bidId = service.placeManualBid("A1", "BIDDER-1", "An", 150.0);

        assertNotNull(bidId);
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        assertEquals(150.0, auction.getCurrentPrice());
        assertEquals("BIDDER-1", auction.getLeadBidderId());
        assertEquals("An", auction.getLeadBidderName());
        assertEquals(1, auction.getBidHistory().size());
        assertEquals(1, bidDAO.placedBids.size());
        assertEquals(1, auctionDAO.updateAfterBidCalls);
        assertEquals(AuctionStatus.RUNNING, auctionDAO.lastStatus);
        assertEquals(1, userDAO.totalBidIncrements.get("BIDDER-1"));
    }

    @Test
    void placeManualBidRejectsAmountNotHigherThanCurrentPrice() {
        Auction auction = TestData.auction("A2", "SELLER");
        auction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.add(auction);

        assertThrows(IllegalArgumentException.class,
                () -> service.placeManualBid("A2", "BIDDER-1", "An", 100.0));

        assertEquals(100.0, auction.getCurrentPrice());
        assertTrue(bidDAO.placedBids.isEmpty());
        assertEquals(0, auctionDAO.updateAfterBidCalls);
    }

    @Test
    void placeManualBidRejectsSellerBiddingOnOwnAuction() {
        Auction auction = TestData.auction("A3", "SELLER");
        auctionDAO.add(auction);

        assertThrows(IllegalStateException.class,
                () -> service.placeManualBid("A3", "SELLER", "Owner", 150.0));

        assertTrue(bidDAO.placedBids.isEmpty());
    }

    @Test
    void placeManualBidRejectsMissingAuction() {
        assertThrows(IllegalArgumentException.class,
                () -> service.placeManualBid("MISSING", "BIDDER-1", "An", 150.0));

        assertTrue(bidDAO.placedBids.isEmpty());
    }

    @Test
    void placeManualBidRejectsClosedAuction() {
        Auction auction = TestData.auction("A4", "SELLER");
        auction.setStatus(AuctionStatus.FINISHED);
        auctionDAO.add(auction);

        assertThrows(IllegalStateException.class,
                () -> service.placeManualBid("A4", "BIDDER-1", "An", 150.0));

        assertTrue(bidDAO.placedBids.isEmpty());
    }

    @Test
    void placeManualBidFinishesExpiredAuctionBeforeRejectingBid() {
        Auction auction = TestData.auction("A5", "SELLER");
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setEndTime(LocalDateTime.now().minusSeconds(1));
        auctionDAO.add(auction);

        assertThrows(IllegalStateException.class,
                () -> service.placeManualBid("A5", "BIDDER-1", "An", 150.0));

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals(AuctionStatus.FINISHED, auctionDAO.lastStatus);
        assertTrue(bidDAO.placedBids.isEmpty());
    }

    @Test
    void placeManualBidReturnsNullWhenBidDaoFails() {
        Auction auction = TestData.auction("A6", "SELLER");
        auctionDAO.add(auction);
        bidDAO.placeBidSucceeds = false;

        String bidId = service.placeManualBid("A6", "BIDDER-1", "An", 150.0);

        assertNull(bidId);
        assertEquals(100.0, auction.getCurrentPrice());
        assertEquals(0, auctionDAO.updateAfterBidCalls);
        assertFalse(userDAO.totalBidIncrements.containsKey("BIDDER-1"));
    }

    @Test
    void autoBidBattlePlacesAutomaticCounterBidAfterManualBid() {
        Auction auction = TestData.auction("A7", "SELLER");
        auction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.add(auction);
        autoBidDAO.configs.put("A7", java.util.List.of(
                new AutoBidConfig("BIDDER-2", "Binh", 200.0, 25.0, 1L)
        ));

        service.placeManualBid("A7", "BIDDER-1", "An", 150.0);

        assertEquals(175.0, auction.getCurrentPrice());
        assertEquals("BIDDER-2", auction.getLeadBidderId());
        assertEquals(2, bidDAO.placedBids.size());
        assertTrue(bidDAO.placedBids.get(1).isAutoBid());
        assertEquals(1, userDAO.totalBidIncrements.get("BIDDER-1"));
        assertEquals(1, userDAO.totalBidIncrements.get("BIDDER-2"));
    }

    @Test
    void registerAutoBidRejectsInvalidMaxBidAndIncrement() {
        Auction auction = TestData.auction("A8", "SELLER");
        auction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.add(auction);

        assertThrows(IllegalArgumentException.class,
                () -> service.registerAutoBid("A8", "BIDDER-1", "An", 100.0, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> service.registerAutoBid("A8", "BIDDER-1", "An", 150.0, 0.0));

        assertTrue(autoBidDAO.getByAuction("A8").isEmpty());
    }

    @Test
    void registerAndCancelAutoBidUseDao() {
        Auction auction = TestData.auction("A9", "SELLER");
        auction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.add(auction);

        assertTrue(service.registerAutoBid("A9", "BIDDER-1", "An", 160.0, 10.0));
        assertEquals(1, autoBidDAO.getByAuction("A9").size());
        assertTrue(service.cancelAutoBid("A9", "BIDDER-1"));
        assertTrue(autoBidDAO.getByAuction("A9").isEmpty());
    }
}
