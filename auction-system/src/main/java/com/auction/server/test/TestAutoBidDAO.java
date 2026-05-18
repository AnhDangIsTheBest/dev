package com.auction.server.test;

import java.time.LocalDateTime;
import java.util.List;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.AutoBidConfig;
import com.auction.shared.model.Item.OtherItem;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.model.User.User;

public class TestAutoBidDAO {

    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        AutoBidDAO autoBidDAO = new AutoBidDAO();

        System.out.println("===== TEST AUTO BID DAO =====");

        String username = "autobid_test_user_" + System.currentTimeMillis();
        String password = "123456";

        User bidder = new Bidder(
                null,
                username,
                username + "@gmail.com",
                "Auto Bid Test User",
                password,
                1_000_000,
                0,
                0
        );

        boolean userInserted = userDAO.insert(bidder);
        System.out.println("Insert bidder: " + userInserted);

        User loginUser = userDAO.login(username, password);
        if (loginUser == null) {
            System.out.println("Không tạo / lấy được bidder, dừng test.");
            return;
        }

        String bidderId = loginUser.getId();
        String bidderName = loginUser.getUsername();
        System.out.println("Bidder ID: " + bidderId);

        OtherItem item = new OtherItem(
                null,
                "AutoBid Test Item " + System.currentTimeMillis(),
                100_000,
                100_000,
                "NEW",
                "Item dùng để test AutoBidDAO",
                "General"
        );

        boolean itemInserted = itemDAO.insert(item, bidderId);
        System.out.println("Insert item: " + itemInserted);

        List<com.auction.shared.model.Item.Item> items = itemDAO.getAllItems();

        String itemId = null;
        com.auction.shared.model.Item.Item savedItem = null;

        for (com.auction.shared.model.Item.Item i : items) {
            if (i.getName().startsWith("AutoBid Test Item")) {
                itemId = i.getId();
                savedItem = i;
                break;
            }
        }

        if (itemId == null || savedItem == null) {
            System.out.println("Không tạo / lấy được item, dừng test.");
            userDAO.delete(bidderId);
            return;
        }

        System.out.println("Item ID: " + itemId);

        Auction auction = new Auction(
                null,
                savedItem,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(30),
                null,
                null,
                true,
                60,
                30
        );

        String auctionId = auctionDAO.createAuction(auction);
        System.out.println("Auction ID: " + auctionId);

        if (auctionId == null) {
            System.out.println("Không tạo được auction, dừng test.");
            itemDAO.delete(itemId);
            userDAO.delete(bidderId);
            return;
        }

        AutoBidConfig config = new AutoBidConfig(
                bidderId,
                bidderName,
                500_000,
                10_000,
                System.currentTimeMillis()
        );

        boolean saved = autoBidDAO.save(auctionId, config);
        System.out.println("Save auto bid: " + saved);

        AutoBidConfig found = autoBidDAO.findByAuctionAndBidder(auctionId, bidderId);
        System.out.println("Found: " + found);

        List<AutoBidConfig> list = autoBidDAO.getByAuction(auctionId);
        System.out.println("Total auto bid configs: " + list.size());

        for (AutoBidConfig c : list) {
            System.out.println("  " + c);
        }

        boolean deletedConfig = autoBidDAO.delete(auctionId, bidderId);
        System.out.println("Delete auto bid: " + deletedConfig);

        boolean deletedAuction = auctionDAO.deleteAuction(auctionId);
        System.out.println("Delete auction: " + deletedAuction);

        boolean deletedItem = itemDAO.delete(itemId);
        System.out.println("Delete item: " + deletedItem);

        if (deletedItem) {
            boolean deletedUser = userDAO.delete(bidderId);
            System.out.println("Delete bidder: " + deletedUser);
        } else {
            System.out.println("Không xoá bidder vì item vẫn còn liên kết.");
}

        System.out.println("===== DONE =====");
    }
}
