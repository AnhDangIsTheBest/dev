package com.auction.test;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.model.Auction;
import com.auction.model.Auction.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.model.Item.Item;
import com.auction.model.Item.OtherItem;
import com.auction.model.User.Bidder;
import com.auction.model.User.Seller;
import com.auction.model.User.User;

import java.time.LocalDateTime;
import java.util.List;

public class TestAuctionDAO {
    public static void main(String[] args) {
        AuctionDAO auctionDAO = new AuctionDAO();
        BidDAO bidDAO = new BidDAO();
        ItemDAO itemDAO = new ItemDAO();
        UserDAO userDAO = new UserDAO();

        String suffix = String.valueOf(System.currentTimeMillis());
        String shortSuffix = suffix.substring(suffix.length() - 8);

        String sellerId = "SEL" + shortSuffix;
        String bidderId = "BID" + shortSuffix;
        String itemId = "ITM_AUC_" + suffix.substring(suffix.length() - 6);
        String auctionId = "AUC" + shortSuffix;
        String bidId = "BTR" + shortSuffix;

        System.out.println("===== TEST AUCTION DAO =====");

        System.out.println("\n--- 0. CREATE TEST USERS ---");
        User seller = new Seller(
                sellerId,
                "seller_auc_" + suffix,
                "seller_auc_" + suffix + "@test.com",
                "123456",
                "Seller Auction Test",
                0,
                0
        );
        User bidder = new Bidder(
                bidderId,
                "bidder_auc_" + suffix,
                "bidder_auc_" + suffix + "@test.com",
                "Bidder Auction Test",
                "123456",
                5_000_000,
                0,
                0
        );
        System.out.println("Insert seller: " + userDAO.insert(seller));
        System.out.println("Insert bidder: " + userDAO.insert(bidder));

        System.out.println("\n--- 1. CREATE ITEM ---");
        Item item = new OtherItem(
                itemId,
                "Test Auction Item",
                100_000,
                100_000,
                "AVAILABLE",
                "Mo ta test auction",
                "General"
        );
        System.out.println("Insert item: " + itemDAO.insert(item, sellerId));

        System.out.println("\n--- 2. CREATE AUCTION ---");
        Auction auction = new Auction(
                auctionId,
                item,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(30),
                null,
                null,
                true,
                60,
                30
        );
        String createdAuctionId = auctionDAO.createAuction(auction);
        System.out.println("Created auction ID: " + createdAuctionId);

        System.out.println("\n--- 3. GET BY ID ---");
        Auction found = auctionDAO.getAuctionById(auctionId);
        System.out.println(found != null ? found.display() : "Not found");

        System.out.println("\n--- 4. GET ALL ---");
        List<Auction> all = auctionDAO.getAllAuctions();
        System.out.println("Total auctions: " + all.size());
        for (Auction a : all) {
            System.out.println("  " + a.display());
        }

        System.out.println("\n--- 5. GET BY STATUS OPEN ---");
        List<Auction> openList = auctionDAO.getAuctionsByStatus(AuctionStatus.OPEN);
        System.out.println("Open auctions: " + openList.size());

        System.out.println("\n--- 6. UPDATE STATUS -> RUNNING ---");
        System.out.println("Update status: " + auctionDAO.updateStatus(auctionId, AuctionStatus.RUNNING));

        System.out.println("\n--- 7. UPDATE PRICE & END TIME ---");
        LocalDateTime newEndTime = LocalDateTime.now().plusHours(1);
        System.out.println("Update price/endTime: " + auctionDAO.updatePriceAndEndTime(auctionId, 150_000, newEndTime));

        System.out.println("\n--- 8. PLACE BID ---");
        BidTransaction bid = new BidTransaction(
                bidId,
                auctionId,
                bidderId,
                "Bidder Auction Test",
                200_000,
                LocalDateTime.now(),
                false
        );
        String createdBidId = bidDAO.placeBid(bid);
        System.out.println("Placed bid ID: " + createdBidId);
        System.out.println("Update auction after bid: " + auctionDAO.updateAfterBid(auctionId, bid, newEndTime));

        System.out.println("\n--- 9. READ BID DATA ---");
        System.out.println("Bids count: " + bidDAO.countBids(auctionId));
        System.out.println("Has bidded: " + bidDAO.hasBidded(auctionId, bidderId));

        BidTransaction lead = bidDAO.getLeadBid(auctionId);
        System.out.println(lead != null ? "Lead bid: " + lead : "Lead bid not found");

        List<BidTransaction> bids = bidDAO.getBidsByAuction(auctionId);
        System.out.println("Bid history:");
        for (BidTransaction b : bids) {
            System.out.println("  " + b);
        }

        System.out.println("\n--- 10. GET AUCTION BY ITEM ID ---");
        Auction byItem = auctionDAO.getAuctionByItemId(itemId);
        System.out.println(byItem != null ? byItem.display() : "Not found by item id");

        System.out.println("\n--- 11. DELETE AUCTION ---");
        System.out.println("Delete auction: " + auctionDAO.deleteAuction(auctionId));
        Auction afterDelete = auctionDAO.getAuctionById(auctionId);
        System.out.println(afterDelete == null ? "Auction deleted OK" : "Still exists: " + afterDelete.display());

        System.out.println("\n--- 12. CLEANUP ---");
        System.out.println("Delete item  : " + itemDAO.delete(itemId));
        System.out.println("Delete bidder: " + userDAO.delete(bidderId));
        System.out.println("Delete seller: " + userDAO.delete(sellerId));

        System.out.println("\n===== DONE =====");
    }
}
