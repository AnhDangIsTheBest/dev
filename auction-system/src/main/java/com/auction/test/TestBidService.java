package com.auction.test;

import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.model.Item.OtherItem;
import com.auction.model.User.Bidder;
import com.auction.model.User.Seller;
import com.auction.service.AuctionService;
import com.auction.service.BidService;

import java.time.LocalDateTime;
import java.util.UUID;

public class TestBidService {
    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionService auctionService = new AuctionService();
        BidService bidService = new BidService();

        String suffix = UUID.randomUUID().toString().substring(0, 5);

        Seller seller = new Seller(
                "SELL" + suffix,
                "seller" + suffix,
                "seller" + suffix + "@test.com",
                "123456",
                "Test Seller",
                0,
                0
        );

        Bidder bidder1 = new Bidder(
                "BID1" + suffix,
                "bidder1" + suffix,
                "bidder1" + suffix + "@test.com",
                "Bidder One",
                "123456",
                1000000,
                0,
                0
        );

        Bidder bidder2 = new Bidder(
                "BID2" + suffix,
                "bidder2" + suffix,
                "bidder2" + suffix + "@test.com",
                "Bidder Two",
                "123456",
                1000000,
                0,
                0
        );

        System.out.println("===== TEST BID SERVICE =====");

        userDAO.insert(seller);
        userDAO.insert(bidder1);
        userDAO.insert(bidder2);

        OtherItem item = new OtherItem(
                "ITEM" + suffix,
                "Đồng hồ test",
                100000,
                100000,
                "AVAILABLE",
                "Item dùng để test service",
                "Other"
        );
        boolean itemOk = itemDAO.insert(item, seller.getId());
        System.out.println("Insert item: " + itemOk);

        String auctionId = auctionService.createAuction(
                item,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(30),
                true,
                60,
                30
        );
        System.out.println("Auction ID: " + auctionId);
        auctionService.startAuction(auctionId);

        bidService.registerAutoBid(auctionId, bidder2.getId(), bidder2.getUsername(), 250000, 20000);
        bidService.placeManualBid(auctionId, bidder1.getId(), bidder1.getUsername(), 150000);

        System.out.println("Auction after bid: " + auctionService.getAuctionById(auctionId).display());

        auctionService.deleteAuction(auctionId);
        itemDAO.delete(item.getId());
        userDAO.delete(bidder1.getId());
        userDAO.delete(bidder2.getId());
        userDAO.delete(seller.getId());

        System.out.println("===== DONE =====");
    }
}
