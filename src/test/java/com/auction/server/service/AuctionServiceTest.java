package com.auction.server.service;

import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Item.Item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionServiceTest {
    private FakeAuctionDAO auctionDAO;
    private FakeBidDAO bidDAO;
    private AuctionService service;

    @BeforeEach
    void setUp() {
        auctionDAO = new FakeAuctionDAO();
        bidDAO = new FakeBidDAO();
        service = new AuctionService(auctionDAO, bidDAO);
    }

    @Test
    void createAuctionPersistsValidAuction() {
        Item item = TestData.item("ITEM-1", "SELLER");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);

        String auctionId = service.createAuction(item, start, end, true, 30, 60);

        assertEquals("AUC-CREATED", auctionId);
        assertNotNull(auctionDAO.createdAuction);
        assertSame(item, auctionDAO.createdAuction.getItem());
        assertEquals(100.0, auctionDAO.createdAuction.getCurrentPrice());
        assertTrue(auctionDAO.createdAuction.isAntiSnipingEnabled());
        assertEquals(30, auctionDAO.createdAuction.snipeWindowSeconds());
        assertEquals(60, auctionDAO.createdAuction.snipeExtendSeconds());
    }

    @Test
    void createAuctionRejectsInvalidInput() {
        Item item = TestData.item("ITEM-2", "SELLER");
        LocalDateTime start = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class,
                () -> service.createAuction(null, start, start.plusHours(1), false, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.createAuction(item, null, start.plusHours(1), false, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.createAuction(item, start, start, false, 0, 0));
    }

    @Test
    void startAuctionOnlyStartsOpenAuction() {
        Auction openAuction = TestData.auction("A1", "SELLER");
        Auction runningAuction = TestData.auction("A2", "SELLER");
        runningAuction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.add(openAuction);
        auctionDAO.add(runningAuction);

        assertTrue(service.startAuction("A1"));
        assertEquals(AuctionStatus.RUNNING, openAuction.getStatus());
        assertFalse(service.startAuction("A2"));
        assertFalse(service.startAuction("MISSING"));
    }

    @Test
    void finishAuctionMarksActiveAuctionFinished() {
        Auction auction = TestData.auction("A3", "SELLER");
        auction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.add(auction);

        assertTrue(service.finishAuction("A3"));

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals(AuctionStatus.FINISHED, auctionDAO.lastStatus);
    }

    @Test
    void finishAuctionRejectsMissingPaidAndCanceledAuctions() {
        Auction paidAuction = TestData.auction("A4", "SELLER");
        paidAuction.setStatus(AuctionStatus.PAID);
        Auction canceledAuction = TestData.auction("A5", "SELLER");
        canceledAuction.setStatus(AuctionStatus.CANCELED);
        auctionDAO.add(paidAuction);
        auctionDAO.add(canceledAuction);

        assertFalse(service.finishAuction("MISSING"));
        assertFalse(service.finishAuction("A4"));
        assertFalse(service.finishAuction("A5"));
    }

    @Test
    void markPaidRequiresFinishedAuction() {
        Auction finishedAuction = TestData.auction("A6", "SELLER");
        finishedAuction.setStatus(AuctionStatus.FINISHED);
        Auction runningAuction = TestData.auction("A7", "SELLER");
        runningAuction.setStatus(AuctionStatus.RUNNING);
        auctionDAO.add(finishedAuction);
        auctionDAO.add(runningAuction);

        assertTrue(service.markPaid("A6"));
        assertEquals(AuctionStatus.PAID, finishedAuction.getStatus());
        assertFalse(service.markPaid("A7"));
        assertFalse(service.markPaid("MISSING"));
    }

    @Test
    void cancelAndDeleteAuctionDelegateToDaos() {
        Auction auction = TestData.auction("A8", "SELLER");
        bidDAO.placeBid(new BidTransaction(
                "B1", "A8", "BIDDER", "An", 150.0, LocalDateTime.now(), false));
        auctionDAO.add(auction);

        assertTrue(service.cancelAuction("A8"));
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertTrue(service.deleteAuction("A8"));
        assertTrue(bidDAO.deletedAuctionIds.contains("A8"));
        assertTrue(auctionDAO.deletedAuctionIds.contains("A8"));
    }

    @Test
    void readMethodsReturnDaoDataAndValidateSellerId() {
        Auction auction = TestData.auction("A9", "SELLER");
        auctionDAO.add(auction);

        assertSame(auction, service.getAuctionById("A9"));
        assertEquals(1, service.getAllAuctions().size());
        assertEquals(1, service.getAuctionsBySeller("SELLER").size());
        assertEquals(1, service.getAuctionsByStatus(AuctionStatus.OPEN).size());
        assertThrows(IllegalArgumentException.class, () -> service.getAuctionsBySeller(" "));
    }
}
