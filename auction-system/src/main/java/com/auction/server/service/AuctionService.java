package com.auction.server.service;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.Item.Item;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidDAO bidDAO = new BidDAO();

    public String createAuction(Item item, LocalDateTime startTime, LocalDateTime endTime,
                                boolean antiSnipingEnabled, int snipeWindowSeconds, int snipeExtendSeconds) {
        if (item == null) throw new IllegalArgumentException("Item không được null");
        if (startTime == null || endTime == null) throw new IllegalArgumentException("Thời gian không được null");
        if (!endTime.isAfter(startTime)) throw new IllegalArgumentException("End time phải sau start time");

        Auction auction = new Auction(
                null,
                item,
                startTime,
                endTime,
                null,
                null,
                antiSnipingEnabled,
                snipeWindowSeconds,
                snipeExtendSeconds
        );
        return auctionDAO.createAuction(auction);
    }

    public boolean startAuction(String auctionId) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) return false;
        if (auction.getStatus() != AuctionStatus.OPEN) return false;
        return auctionDAO.updateStatus(auctionId, AuctionStatus.RUNNING);
    }

    public boolean finishAuction(String auctionId) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) return false;
        if (auction.getStatus() == AuctionStatus.PAID || auction.getStatus() == AuctionStatus.CANCELED) return false;
        return auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
    }

    public boolean cancelAuction(String auctionId) {
        return auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELED);
    }

    public boolean markPaid(String auctionId) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.FINISHED) return false;
        return auctionDAO.updateStatus(auctionId, AuctionStatus.PAID);
    }

    public Auction getAuctionById(String auctionId) {
        return auctionDAO.getAuctionById(auctionId);
    }

    public List<Auction> getAllAuctions() {
        return auctionDAO.getAllAuctions();
    }

    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        return auctionDAO.getAuctionsByStatus(status);
    }

    public boolean deleteAuction(String auctionId) {
        bidDAO.deleteBidsByAuction(auctionId);
        return auctionDAO.deleteAuction(auctionId);
    }
}
