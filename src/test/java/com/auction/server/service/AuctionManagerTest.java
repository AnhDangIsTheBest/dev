package com.auction.server.service;

import com.auction.server.exception.AuctionNotFoundException;
import com.auction.shared.model.Auction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionManagerTest {
    private AuctionManager manager;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();
        manager.clearAll();
    }

    @Test
    void singletonStoresReadsAndRemovesAuctions() throws AuctionNotFoundException {
        Auction auction = TestData.auction("A1", "SELLER");

        manager.addAuction(auction);

        assertSame(manager, AuctionManager.getInstance());
        assertTrue(manager.contains("A1"));
        assertSame(auction, manager.getAuction("A1"));
        assertEquals(1, manager.getAllAuctions().size());
        assertEquals(1, manager.getSize());

        manager.removeAuction("A1");

        assertFalse(manager.contains("A1"));
        assertEquals(0, manager.getSize());
    }

    @Test
    void invalidAuctionOperationsThrowCheckedException() {
        assertThrows(AuctionNotFoundException.class, () -> manager.addAuction(null));
        assertThrows(AuctionNotFoundException.class, () -> manager.getAuction("MISSING"));
        assertThrows(AuctionNotFoundException.class, () -> manager.removeAuction("MISSING"));
    }
}
