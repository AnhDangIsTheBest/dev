package com.auction.server.service;

import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.AutoBidConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BidServiceEdgeCaseTest {
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
    void registerAutoBidDoesNotRunBattleWhenSaveFails() {
        Auction auction = TestData.auction("A1", "SELLER");
        auction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.add(auction);
        autoBidDAO.saveResult = false;

        assertFalse(service.registerAutoBid("A1", "BIDDER-1", "An", 160.0, 10.0));
        assertTrue(bidDAO.placedBids.isEmpty());
        assertEquals(0, auctionDAO.updateAfterBidCalls);
    }

    @Test
    void autoBidBattleIgnoresSellerManualLeaderAndExpiredConfigs() {
        Auction auction = TestData.auction("A2", "SELLER");
        auction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.add(auction);
        autoBidDAO.configs.put("A2", List.of(
                new AutoBidConfig("SELLER", "Seller", 300.0, 10.0, 1L),
                new AutoBidConfig("BIDDER-1", "An", 300.0, 10.0, 2L),
                new AutoBidConfig("LOW", "Low", 100.0, 10.0, 3L),
                new AutoBidConfig("BIDDER-2", "Binh", 180.0, 20.0, 4L)
        ));

        service.placeManualBid("A2", "BIDDER-1", "An", 150.0);

        assertEquals(170.0, auction.getCurrentPrice());
        assertEquals("BIDDER-2", auction.getLeadBidderId());
        assertEquals(2, bidDAO.placedBids.size());
        assertEquals(1, userDAO.totalBidIncrements.get("BIDDER-1"));
        assertEquals(1, userDAO.totalBidIncrements.get("BIDDER-2"));
    }

    @Test
    void registerAutoBidReturnsSavedResultWhenAuctionDisappearsBeforeBattle() {
        Auction auction = TestData.auction("A3", "SELLER");
        auction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.add(auction);
        autoBidDAO = new RemovingAuctionAutoBidDAO(auctionDAO);
        service = new BidService(auctionDAO, bidDAO, autoBidDAO, userDAO);

        assertTrue(service.registerAutoBid("A3", "BIDDER-2", "Binh", 200.0, 10.0));
        assertTrue(bidDAO.placedBids.isEmpty());
        assertEquals(0, auctionDAO.updateAfterBidCalls);
    }

    private static final class RemovingAuctionAutoBidDAO extends FakeAutoBidDAO {
        private final FakeAuctionDAO auctionDAO;

        private RemovingAuctionAutoBidDAO(FakeAuctionDAO auctionDAO) {
            this.auctionDAO = auctionDAO;
        }

        @Override
        public boolean save(String auctionId, AutoBidConfig config) {
            boolean saved = super.save(auctionId, config);
            auctionDAO.auctions.remove(auctionId);
            return saved;
        }
    }
}
