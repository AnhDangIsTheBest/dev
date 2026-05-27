package com.auction.server.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.AutoBidConfig;
import com.auction.shared.model.BidTransaction;

public class BidService {
    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;
    private final AutoBidDAO autoBidDAO;
    private final UserDAO userDAO;

    public BidService() {
        this(new AuctionDAO(), new BidDAO(), new AutoBidDAO(), new UserDAO());
    }

    BidService(AuctionDAO auctionDAO, BidDAO bidDAO, AutoBidDAO autoBidDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.autoBidDAO = autoBidDAO;
        this.userDAO = userDAO;
    }

    public String placeManualBid(String auctionId, String bidderId, String bidderName, double amount) {
        Auction auction = validateAuctionForBid(auctionId, bidderId);
        validateBidAmount(auction, amount);

        BidTransaction manualBid = createBid(auctionId, bidderId, bidderName, amount, false);
        String bidId = bidDAO.placeBid(manualBid);
        if (bidId == null) {
            return null;
        }

        userDAO.incrementTotalBids(bidderId);
        auction.applyBid(manualBid);
        auctionDAO.updateAfterBid(auctionId, manualBid, auction.getEndTime());

        runAutoBidBattle(auctionId);
        return bidId;
    }

    public boolean registerAutoBid(String auctionId, String bidderId, String bidderName, double maxBid, double increment) {
        Auction auction = validateAuctionForBid(auctionId, bidderId);

        if (maxBid <= auction.getCurrentPrice()) {
            throw new IllegalArgumentException("Giá auto bid tối đa phải lớn hơn giá hiện tại");
        }

        if (increment <= 0) {
            throw new IllegalArgumentException("Bước nhảy auto bid phải lớn hơn 0");
        }

        AutoBidConfig config = new AutoBidConfig(
                bidderId,
                bidderName,
                maxBid,
                increment,
                System.currentTimeMillis()
        );

        boolean saved = autoBidDAO.save(auctionId, config);

        if (saved) {
            runAutoBidBattle(auctionId);
        }

        return saved;
    }

    public boolean cancelAutoBid(String auctionId, String bidderId) {
        return autoBidDAO.delete(auctionId, bidderId);
    }

    private Auction validateAuctionForBid(String auctionId, String bidderId) {
        Auction auction = auctionDAO.getAuctionById(auctionId);

        if (auction == null) {
            throw new IllegalArgumentException("Không tìm thấy phiên đấu giá: " + auctionId);
        }

        if (auction.getStatus() != AuctionStatus.OPEN && auction.getStatus() != AuctionStatus.RUNNING) {
            throw new IllegalStateException("Phiên đấu giá không còn nhận bid");
        }

        if (isAuctionOwner(auction, bidderId)) {
            throw new IllegalStateException("Bạn không được tự đặt giá ở phiên đấu giá của mình");
        }

        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
            throw new IllegalStateException("Phiên đấu giá đã kết thúc");
        }

        if (auction.getStatus() == AuctionStatus.OPEN) {
            auction.setStatus(AuctionStatus.RUNNING);
            auctionDAO.updateStatus(auctionId, AuctionStatus.RUNNING);
        }

        return auction;
    }

    private void validateBidAmount(Auction auction, double amount) {
        if (amount <= auction.getCurrentPrice()) {
            throw new IllegalArgumentException("Giá bid phải lớn hơn giá hiện tại");
        }
    }

    private boolean isAuctionOwner(Auction auction, String bidderId) {
        return auction != null
                && auction.getItem() != null
                && auction.getItem().getSellerId() != null
                && bidderId != null
                && auction.getItem().getSellerId().equals(bidderId);
    }

    private void runAutoBidBattle(String auctionId) {
        Auction auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) {
            return;
        }

        double currentPrice = auction.getCurrentPrice();
        String leadBidderId = auction.getLeadBidderId();
        String sellerId = auction.getItem() == null ? null : auction.getItem().getSellerId();

        List<AutoBidConfig> configs = autoBidDAO.getByAuction(auctionId)
                .stream()
                .filter(c -> c.getMaxBid() > currentPrice)
                .filter(c -> sellerId == null || !c.getBidderId().equals(sellerId))
                .filter(c -> leadBidderId == null || !c.getBidderId().equals(leadBidderId))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        for (AutoBidConfig config : configs) {
            auction = auctionDAO.getAuctionById(auctionId);
            if (auction == null) {
                return;
            }

            if (auction.getLeadBidderId() != null 
                    && config.getBidderId().equals(auction.getLeadBidderId())) {
                continue;
            }

            if (config.getMaxBid() <= auction.getCurrentPrice()) {
                continue;
            }

            double nextAmount = Math.min(
                    auction.getCurrentPrice() + config.getIncrement(),
                    config.getMaxBid()
            );

            if (nextAmount <= auction.getCurrentPrice()) {
                continue;
            }

            BidTransaction autoBid = createBid(
                    auctionId,
                    config.getBidderId(),
                    config.getBidderName(),
                    nextAmount,
                    true
            );

            String bidId = bidDAO.placeBid(autoBid);

            if (bidId != null) {
                userDAO.incrementTotalBids(config.getBidderId());
                auction.applyBid(autoBid);
                auctionDAO.updateAfterBid(auctionId, autoBid, auction.getEndTime());
            }
        }
    }

    private BidTransaction createBid(String auctionId, String bidderId, String bidderName, double amount, boolean auto) {
        return new BidTransaction(
                UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                auctionId,
                bidderId,
                bidderName,
                amount,
                LocalDateTime.now(),
                auto
        );
    }
}
