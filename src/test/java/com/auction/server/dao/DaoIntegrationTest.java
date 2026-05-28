package com.auction.server.dao;

import com.auction.server.config.DBConnection;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.AutoBidConfig;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Item.Art;
import com.auction.shared.model.Item.Electronics;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.Item.OtherItem;
import com.auction.shared.model.Item.Vehicle;
import com.auction.shared.model.User.Admin;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.model.User.Seller;
import com.auction.shared.model.User.User;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaoIntegrationTest {
    private final UserDAO userDAO = new UserDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO bidDAO = new BidDAO();
    private final AutoBidDAO autoBidDAO = new AutoBidDAO();

    @BeforeAll
    static void configureDatabase() throws Exception {
        DBConnection.configure(
                "jdbc:h2:mem:auction_dao;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
                "sa",
                "");
    }

    @BeforeEach
    void resetSchema() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS auto_bid_configs");
            st.execute("DROP TABLE IF EXISTS bid_transactions");
            st.execute("DROP TABLE IF EXISTS auctions");
            st.execute("DROP TABLE IF EXISTS item_electronics");
            st.execute("DROP TABLE IF EXISTS item_vehicles");
            st.execute("DROP TABLE IF EXISTS item_arts");
            st.execute("DROP TABLE IF EXISTS item_others");
            st.execute("DROP TABLE IF EXISTS items");
            st.execute("DROP TABLE IF EXISTS users");

            st.execute("""
                    CREATE TABLE users (
                        id VARCHAR(36) PRIMARY KEY,
                        username VARCHAR(50) NOT NULL UNIQUE,
                        email VARCHAR(100) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL,
                        fullname VARCHAR(100) NOT NULL,
                        role VARCHAR(20) NOT NULL,
                        balance DECIMAL(15,2) DEFAULT 0,
                        total_bids INT DEFAULT 0,
                        won_auctions INT DEFAULT 0,
                        total_items_listed INT DEFAULT 0,
                        total_revenue DECIMAL(15,2) DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            st.execute("""
                    CREATE TABLE items (
                        id VARCHAR(36) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        description VARCHAR(1024),
                        type VARCHAR(20) NOT NULL,
                        starting_price DECIMAL(15,2) NOT NULL,
                        current_price DECIMAL(15,2) NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        image_data BLOB,
                        seller_id VARCHAR(36) NOT NULL,
                        FOREIGN KEY (seller_id) REFERENCES users(id)
                    )
                    """);
            st.execute("CREATE TABLE item_electronics (item_id VARCHAR(36) PRIMARY KEY, brand VARCHAR(100), model VARCHAR(100), warranty INT, FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE item_vehicles (item_id VARCHAR(36) PRIMARY KEY, brand VARCHAR(100), vehicle_model VARCHAR(100), year INT, mileage INT, vehicle_type VARCHAR(100), FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE item_arts (item_id VARCHAR(36) PRIMARY KEY, artist VARCHAR(100), year_created INT, material VARCHAR(100), FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE item_others (item_id VARCHAR(36) PRIMARY KEY, category VARCHAR(100), FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE)");
            st.execute("""
                    CREATE TABLE auctions (
                        id VARCHAR(36) PRIMARY KEY,
                        item_id VARCHAR(36) NOT NULL,
                        current_price DECIMAL(15,2) NOT NULL,
                        lead_bidder_id VARCHAR(36),
                        lead_bidder_name VARCHAR(100),
                        start_time TIMESTAMP NOT NULL,
                        end_time TIMESTAMP NOT NULL,
                        status VARCHAR(20) DEFAULT 'OPEN',
                        anti_sniping_enabled BOOLEAN DEFAULT FALSE,
                        snipe_window_seconds INT DEFAULT 30,
                        snipe_extend_seconds INT DEFAULT 60,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
                        FOREIGN KEY (lead_bidder_id) REFERENCES users(id)
                    )
                    """);
            st.execute("""
                    CREATE TABLE bid_transactions (
                        id VARCHAR(36) PRIMARY KEY,
                        auction_id VARCHAR(36) NOT NULL,
                        bidder_id VARCHAR(36) NOT NULL,
                        bidder_name VARCHAR(100) NOT NULL,
                        amount DECIMAL(15,2) NOT NULL,
                        bid_time TIMESTAMP NOT NULL,
                        is_auto_bid BOOLEAN DEFAULT FALSE,
                        FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                        FOREIGN KEY (bidder_id) REFERENCES users(id)
                    )
                    """);
            st.execute("""
                    CREATE TABLE auto_bid_configs (
                        auction_id VARCHAR(36) NOT NULL,
                        bidder_id VARCHAR(36) NOT NULL,
                        bidder_name VARCHAR(100) NOT NULL,
                        max_bid DECIMAL(15,2) NOT NULL,
                        increment DECIMAL(15,2) NOT NULL,
                        registered_at BIGINT NOT NULL,
                        PRIMARY KEY (auction_id, bidder_id),
                        FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                        FOREIGN KEY (bidder_id) REFERENCES users(id)
                    )
                    """);
        }
    }

    @Test
    void userDaoCoversCrudRolesCountersAndErrorPaths() {
        User bidder = new Bidder("BIDDER", "ann", "ann@example.test", "Ann", "secret", 10.0, 1, 0);
        User seller = new Seller("SELLER", "seller", "seller@example.test", "secret", "Seller", 2, 50.0);
        User admin = new Admin("ADMIN", "root", "root@example.test", "secret", "Root");

        assertTrue(userDAO.insert(bidder));
        assertTrue(userDAO.insert(seller));
        assertTrue(userDAO.insert(admin));
        assertFalse(userDAO.insert(new Bidder("BIDDER", "dup", "dup@example.test", "Dup", "pw", 0, 0, 0)));

        assertTrue(userDAO.existsByUsername("ann"));
        assertFalse(userDAO.existsByUsername("missing"));
        assertTrue(userDAO.existsByEmail("seller@example.test"));
        assertFalse(userDAO.existsByEmail("missing@example.test"));

        assertInstanceOf(Bidder.class, userDAO.login("ann", "secret"));
        assertNull(userDAO.login("ann", "bad"));
        assertInstanceOf(Seller.class, userDAO.findById("SELLER"));
        assertInstanceOf(Admin.class, userDAO.findById("ADMIN"));
        assertNull(userDAO.findById("MISSING"));
        assertEquals(3, userDAO.getAll().size());

        bidder.setFullName("Ann Updated");
        assertTrue(userDAO.update(bidder));
        assertTrue(userDAO.deposit("BIDDER", 5.0));
        assertTrue(userDAO.incrementTotalBids("BIDDER"));
        assertTrue(userDAO.incrementTotalItemsListed("SELLER"));

        Bidder updated = (Bidder) userDAO.findById("BIDDER");
        assertEquals("Ann Updated", updated.getFullname());
        assertEquals(15.0, updated.getBalance());
        assertEquals(2, updated.getTotalBids());

        assertTrue(userDAO.delete("ADMIN"));
        assertFalse(userDAO.delete("MISSING"));
    }

    @Test
    void itemAuctionBidAndAutoBidDaosWorkTogetherAgainstSqlSchema() {
        seedUsers();
        Electronics electronics = new Electronics("ITEM-E", "Laptop", "Desc", 100.0, "NEW", 100.0,
                "Dell", 24, "G15");
        electronics.setImageData(new byte[] {1, 2});
        Vehicle vehicle = new Vehicle("ITEM-V", "Car", 200.0, 200.0, "USED", "Desc",
                "Toyota", "Camry", 2020, 50000, "CAR");
        Art art = new Art("ITEM-A", "Painting", 300.0, 300.0, "NEW", "Desc",
                "Artist", 2024, "Oil");
        OtherItem other = new OtherItem("ITEM-O", "Book", 10.0, 10.0, "NEW", "Desc", "Collectible");

        assertTrue(itemDAO.insert(electronics, "SELLER"));
        assertTrue(itemDAO.insert(vehicle, "SELLER"));
        assertTrue(itemDAO.insert(art, "SELLER"));
        assertTrue(itemDAO.insert(other, "SELLER"));
        assertFalse(itemDAO.insert(electronics, "SELLER"));

        assertInstanceOf(Electronics.class, itemDAO.findById("ITEM-E"));
        assertInstanceOf(Vehicle.class, itemDAO.findById("ITEM-V"));
        assertInstanceOf(Art.class, itemDAO.findById("ITEM-A"));
        assertInstanceOf(OtherItem.class, itemDAO.findById("ITEM-O"));
        assertNull(itemDAO.findById("MISSING"));
        assertEquals(4, itemDAO.getAllItems().size());
        assertEquals(4, itemDAO.getItemsBySeller("SELLER").size());

        electronics.setName("Updated Laptop");
        assertTrue(itemDAO.update(electronics));
        assertEquals("Updated Laptop", itemDAO.findById("ITEM-E").getName());

        Auction auction = new Auction("AUC-1", electronics,
                LocalDateTime.of(2026, 5, 28, 9, 0),
                LocalDateTime.of(2026, 5, 28, 10, 0),
                null, null, true, 30, 60);
        assertEquals("AUC-1", auctionDAO.createAuction(auction));
        assertNull(auctionDAO.createAuction(auction));

        Auction loaded = auctionDAO.getAuctionById("AUC-1");
        assertNotNull(loaded);
        assertEquals("ITEM-E", loaded.getItem().getId());
        assertTrue(loaded.isAntiSnipingEnabled());
        List<Auction> summaryAuctions = auctionDAO.getAllAuctions();
        assertEquals(1, summaryAuctions.size());
        assertArrayEquals(new byte[] {1, 2}, summaryAuctions.get(0).getItem().getImageData());
        assertEquals(1, auctionDAO.getAuctionsBySeller("SELLER").size());
        assertEquals(1, auctionDAO.getAuctionsByStatus(AuctionStatus.OPEN).size());
        assertEquals(loaded.getAuctionId(), auctionDAO.getAuctionByItemId("ITEM-E").getAuctionId());
        assertTrue(auctionDAO.getAuctionsByBidder("BIDDER").isEmpty());

        BidTransaction first = new BidTransaction("BID-1", "AUC-1", "BIDDER", "Ann",
                150.0, LocalDateTime.of(2026, 5, 28, 9, 10), false);
        BidTransaction second = new BidTransaction("BID-2", "AUC-1", "BIDDER", "Ann",
                175.0, LocalDateTime.of(2026, 5, 28, 9, 20), true);
        assertEquals("BID-1", bidDAO.placeBid(first));
        assertEquals("BID-2", bidDAO.placeBid(second));
        assertNull(bidDAO.placeBid(second));
        assertEquals(2, bidDAO.getBidsByAuction("AUC-1").size());
        assertEquals(2, bidDAO.getBidsByBidder("BIDDER").size());
        assertEquals(2, bidDAO.countBids("AUC-1"));
        assertTrue(bidDAO.hasBidded("AUC-1", "BIDDER"));
        assertFalse(bidDAO.hasBidded("AUC-1", "OTHER"));
        assertEquals("BID-2", bidDAO.getLeadBid("AUC-1").getTransactionId());
        Map<String, Double> bestBids = bidDAO.getMyBestBids("BIDDER");
        assertEquals(175.0, bestBids.get("AUC-1"));
        assertEquals(1, auctionDAO.getAuctionsByBidder("BIDDER").size());

        assertTrue(auctionDAO.updateAfterBid("AUC-1", second, LocalDateTime.of(2026, 5, 28, 10, 5)));
        assertTrue(auctionDAO.updatePriceAndEndTime("AUC-1", 180.0, LocalDateTime.of(2026, 5, 28, 10, 10)));
        assertTrue(auctionDAO.updateStatus("AUC-1", AuctionStatus.RUNNING));
        assertEquals(AuctionStatus.RUNNING, auctionDAO.getAuctionById("AUC-1").getStatus());

        AutoBidConfig config = new AutoBidConfig("BIDDER", "Ann", 250.0, 10.0, 1L);
        AutoBidConfig updatedConfig = new AutoBidConfig("BIDDER", "Ann Updated", 300.0, 15.0, 2L);
        assertTrue(autoBidDAO.save("AUC-1", config));
        assertTrue(autoBidDAO.save("AUC-1", updatedConfig));
        assertEquals(300.0, autoBidDAO.findByAuctionAndBidder("AUC-1", "BIDDER").getMaxBid());
        assertNull(autoBidDAO.findByAuctionAndBidder("AUC-1", "OTHER"));
        assertEquals(1, autoBidDAO.getByAuction("AUC-1").size());
        assertTrue(autoBidDAO.delete("AUC-1", "BIDDER"));
        assertFalse(autoBidDAO.delete("AUC-1", "BIDDER"));
        assertTrue(autoBidDAO.save("AUC-1", config));
        assertTrue(autoBidDAO.deleteByAuction("AUC-1"));
        assertTrue(autoBidDAO.deleteByAuction("MISSING"));

        assertTrue(bidDAO.deleteBidsByAuction("AUC-1"));
        assertFalse(bidDAO.deleteBidsByAuction("AUC-1"));
        assertTrue(auctionDAO.deleteAuction("AUC-1"));
        assertFalse(auctionDAO.deleteAuction("AUC-1"));
        assertTrue(itemDAO.delete("ITEM-O"));
        assertFalse(itemDAO.delete("MISSING"));
    }

    private void seedUsers() {
        assertTrue(userDAO.insert(new Seller("SELLER", "seller", "seller@example.test",
                "secret", "Seller", 0, 0)));
        assertTrue(userDAO.insert(new Bidder("BIDDER", "ann", "ann@example.test",
                "Ann", "secret", 1000.0, 0, 0)));
    }
}
