package com.auction.shared.model;

import com.auction.shared.model.Item.Art;
import com.auction.shared.model.Item.Electronics;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.Item.ItemFactory;
import com.auction.shared.model.Item.OtherItem;
import com.auction.shared.model.Item.Vehicle;
import com.auction.shared.model.User.Admin;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.model.User.Seller;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelUtilityTest {
    @Test
    void autoBidConfigOrdersByRegistrationTime() {
        AutoBidConfig later = new AutoBidConfig("U2", "Binh", 200.0, 10.0, 2L);
        AutoBidConfig earlier = new AutoBidConfig("U1", "An", 200.0, 10.0, 1L);

        List<AutoBidConfig> configs = new java.util.ArrayList<>(List.of(later, earlier));
        configs.sort(null);

        assertEquals(List.of(earlier, later), configs);
        assertTrue(earlier.toString().contains("An"));
    }

    @Test
    void bidTransactionFormatsTimeAndRendersAutoBidMarker() {
        BidTransaction bid = new BidTransaction("B1", "A1", "U1", "An", 150.0,
                LocalDateTime.of(2026, 5, 18, 9, 30, 15), true);

        assertEquals("B1", bid.getTransactionId());
        assertEquals("A1", bid.getAuctionId());
        assertEquals("U1", bid.getBidderId());
        assertEquals("An", bid.getBidderName());
        assertEquals(150.0, bid.getAmount());
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 30, 15), bid.getLocalDateTime());
        assertTrue(bid.isAutoBid());
        assertEquals("09:30:15", bid.getFormattedTimeHour());
        assertEquals("18/05/2026", bid.getFormattedTimeDay());
        assertTrue(bid.toString().contains("(Auto)"));
    }

    @Test
    void itemFactoryCreatesAllSupportedItemTypesWithDefaultsAndExtras() {
        Item electronics = ItemFactory.createItem(ItemFactory.ItemType.ELECTRONICS,
                "Laptop", "Desc", 100.0, "NEW", 100.0, "Dell", "24", "G15");
        Item art = ItemFactory.createItem(ItemFactory.ItemType.ART,
                "Painting", "Desc", 200.0, "NEW", 200.0);
        Item vehicle = ItemFactory.createItem(ItemFactory.ItemType.VEHICLE,
                "Car", "Desc", 300.0, "USED", 300.0, "Toyota", "Camry", "2020", "50000", "CAR");
        Item other = ItemFactory.createItem(ItemFactory.ItemType.OTHERITEM,
                "Book", "Desc", 10.0, "NEW", 10.0, "Collectible");

        assertInstanceOf(Electronics.class, electronics);
        assertEquals("Electronics", electronics.getType());
        assertEquals("Dell", ((Electronics) electronics).getBrand());
        assertInstanceOf(Art.class, art);
        assertEquals("Unknown", ((Art) art).getArtist());
        assertInstanceOf(Vehicle.class, vehicle);
        assertEquals("Toyota", ((Vehicle) vehicle).getBrand());
        assertInstanceOf(OtherItem.class, other);
        assertEquals("Collectible", ((OtherItem) other).getCategory());
    }

    @Test
    void itemFieldsAndDisplaysCanBeUpdated() {
        Electronics item = new Electronics("I1", "Laptop", "Desc", 100.0, "NEW", 100.0,
                "Dell", 24, "G15");

        item.setName("Updated");
        item.setDescription("Updated desc");
        item.setStartingPrice(120.0);
        item.setCurrentPrice(130.0);
        item.setStatus("USED");
        item.setImageData(new byte[] {1, 2, 3});
        item.setSellerId("SELLER");

        assertEquals("Updated", item.getName());
        assertEquals("Updated desc", item.getDescription());
        assertEquals(120.0, item.getStartingPrice());
        assertEquals(130.0, item.getCurrentPrice());
        assertEquals("USED", item.getStatus());
        assertEquals(3, item.getImageData().length);
        assertEquals("SELLER", item.getSellerId());
        assertTrue(item.display().contains("Dell"));
    }

    @Test
    void userStatsPasswordAndDisplaysWorkForBidderAndAdmin() {
        Bidder bidder = new Bidder("U1", "ann", "ann@example.test", "Ann", "secret", 10.0, 1, 0);
        Admin admin = new Admin("A1", "root", "root@example.test", "secret", "Root");
        Seller seller = new Seller("S1", "seller", "seller@example.test", "secret", "Seller", 1, 0.0);

        bidder.incrementBids();
        bidder.incrementWon();
        seller.addRevenue(50.0);
        bidder.setUsername("ann2");
        bidder.setPassword("new-secret");
        bidder.setFullName("Ann Updated");

        assertEquals("BIDDER", bidder.getRole());
        assertEquals("ann2", bidder.getUsername());
        assertTrue(bidder.checkPassword("new-secret"));
        assertFalse(bidder.checkPassword("secret"));
        assertEquals(2, bidder.getTotalBids());
        assertEquals(1, bidder.getWonAuctions());
        assertTrue(bidder.display().contains("Balance"));
        assertEquals("SELLER", seller.getRole());
        assertEquals(1, seller.getTotalItemslisted());
        assertEquals(50.0, seller.getTotalRevenue());
        assertEquals("ADMIN", admin.getRole());
        assertTrue(admin.display().contains("ADMIN"));
    }
}
