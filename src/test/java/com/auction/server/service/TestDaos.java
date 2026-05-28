package com.auction.server.service;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Auction.AuctionStatus;
import com.auction.shared.model.AutoBidConfig;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Item.Electronics;
import com.auction.shared.model.Item.Item;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.model.User.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TestData {
    private TestData() {
    }

    static Electronics item(String id, String sellerId) {
        Electronics item = new Electronics(id, "Laptop", "Gaming laptop", 100.0, "NEW", 100.0,
                "Dell", 24, "G15");
        item.setSellerId(sellerId);
        return item;
    }

    static Auction auction(String auctionId, String sellerId) {
        return new Auction(
                auctionId,
                item("ITEM-" + auctionId, sellerId),
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10),
                null,
                null,
                false,
                30,
                60
        );
    }
}

class FakeAuctionDAO extends AuctionDAO {
    final Map<String, Auction> auctions = new HashMap<>();
    final List<String> deletedAuctionIds = new ArrayList<>();
    Auction createdAuction;
    String createdId = "AUC-CREATED";
    int updateAfterBidCalls;
    int updateStatusCalls;
    String lastStatusAuctionId;
    AuctionStatus lastStatus;

    void add(Auction auction) {
        auctions.put(auction.getAuctionId(), auction);
    }

    @Override
    public String createAuction(Auction auction) {
        createdAuction = auction;
        auctions.put(createdId, auction);
        return createdId;
    }

    @Override
    public Auction getAuctionById(String auctionId) {
        return auctions.get(auctionId);
    }

    @Override
    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }

    @Override
    public List<Auction> getAuctionsBySeller(String sellerId) {
        return auctions.values().stream()
                .filter(a -> a.getItem() != null && sellerId.equals(a.getItem().getSellerId()))
                .toList();
    }

    @Override
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        return auctions.values().stream()
                .filter(a -> a.getStatus() == status)
                .toList();
    }

    @Override
    public boolean updateAfterBid(String auctionId, BidTransaction bid, LocalDateTime newEndTime) {
        updateAfterBidCalls++;
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            return false;
        }
        auction.setEndTime(newEndTime);
        return true;
    }

    @Override
    public boolean updateStatus(String auctionId, AuctionStatus status) {
        updateStatusCalls++;
        lastStatusAuctionId = auctionId;
        lastStatus = status;
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            return false;
        }
        auction.setStatus(status);
        return true;
    }

    @Override
    public boolean deleteAuction(String auctionId) {
        deletedAuctionIds.add(auctionId);
        return auctions.remove(auctionId) != null;
    }
}

class FakeBidDAO extends BidDAO {
    final List<BidTransaction> placedBids = new ArrayList<>();
    final List<String> deletedAuctionIds = new ArrayList<>();
    boolean placeBidSucceeds = true;

    @Override
    public String placeBid(BidTransaction bid) {
        if (!placeBidSucceeds) {
            return null;
        }
        placedBids.add(bid);
        return bid.getTransactionId();
    }

    @Override
    public List<BidTransaction> getBidsByAuction(String auctionId) {
        return placedBids.stream()
                .filter(bid -> auctionId.equals(bid.getAuctionId()))
                .toList();
    }

    @Override
    public int countBids(String auctionId) {
        return getBidsByAuction(auctionId).size();
    }

    @Override
    public boolean hasBidded(String auctionId, String bidderId) {
        return placedBids.stream()
                .anyMatch(bid -> auctionId.equals(bid.getAuctionId()) && bidderId.equals(bid.getBidderId()));
    }

    @Override
    public boolean deleteBidsByAuction(String auctionId) {
        deletedAuctionIds.add(auctionId);
        return placedBids.removeIf(bid -> auctionId.equals(bid.getAuctionId()));
    }
}

class FakeAutoBidDAO extends AutoBidDAO {
    final Map<String, List<AutoBidConfig>> configs = new HashMap<>();
    boolean saveResult = true;

    @Override
    public boolean save(String auctionId, AutoBidConfig config) {
        configs.computeIfAbsent(auctionId, ignored -> new ArrayList<>()).add(config);
        return saveResult;
    }

    @Override
    public List<AutoBidConfig> getByAuction(String auctionId) {
        return new ArrayList<>(configs.getOrDefault(auctionId, List.of()));
    }

    @Override
    public boolean delete(String auctionId, String bidderId) {
        return configs.getOrDefault(auctionId, new ArrayList<>())
                .removeIf(config -> bidderId.equals(config.getBidderId()));
    }
}

class FakeUserDAO extends UserDAO {
    final Map<String, User> users = new HashMap<>();
    final Map<String, Integer> totalBidIncrements = new HashMap<>();
    final Map<String, Integer> totalItemIncrements = new HashMap<>();
    boolean insertResult = true;
    boolean updateResult = true;
    boolean deleteResult = true;
    boolean depositResult = true;
    RuntimeException usernameFailure;

    @Override
    public boolean insert(User user) {
        if (!insertResult) {
            return false;
        }
        users.put(user.getUsername(), user);
        return true;
    }

    @Override
    public boolean existsByUsername(String username) {
        if (usernameFailure != null) {
            throw usernameFailure;
        }
        return users.values().stream().anyMatch(user -> username.equals(user.getUsername()));
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.values().stream().anyMatch(user -> email.equals(user.getEmail()));
    }

    @Override
    public User login(String username, String password) {
        return users.values().stream()
                .filter(user -> username.equals(user.getUsername()) && user.checkPassword(password))
                .findFirst()
                .orElse(null);
    }

    @Override
    public User findById(String id) {
        return users.values().stream()
                .filter(user -> id.equals(user.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<User> getAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public boolean update(User user) {
        if (updateResult) {
            users.put(user.getUsername(), user);
        }
        return updateResult;
    }

    @Override
    public boolean delete(String id) {
        users.values().removeIf(user -> id.equals(user.getId()));
        return deleteResult;
    }

    @Override
    public boolean deposit(String id, double amount) {
        User user = findById(id);
        if (user instanceof Bidder bidder) {
            users.put(bidder.getUsername(), new Bidder(
                    bidder.getId(),
                    bidder.getUsername(),
                    bidder.getEmail(),
                    bidder.getFullname(),
                    bidder.getPassword(),
                    bidder.getBalance() + amount,
                    bidder.getTotalBids(),
                    bidder.getWonAuctions()
            ));
        }
        return depositResult;
    }

    @Override
    public boolean incrementTotalBids(String id) {
        totalBidIncrements.merge(id, 1, Integer::sum);
        return true;
    }

    @Override
    public boolean incrementTotalItemsListed(String id) {
        totalItemIncrements.merge(id, 1, Integer::sum);
        return true;
    }
}

class FakeItemDAO extends ItemDAO {
    final Map<String, Item> items = new HashMap<>();
    boolean insertResult = true;
    boolean updateResult = true;
    boolean deleteResult = true;

    @Override
    public boolean insert(Item item, String sellerId) {
        if (insertResult) {
            item.setSellerId(sellerId);
            items.put(item.getId(), item);
        }
        return insertResult;
    }

    @Override
    public Item findById(String itemId) {
        return items.get(itemId);
    }

    @Override
    public List<Item> getAllItems() {
        return new ArrayList<>(items.values());
    }

    @Override
    public List<Item> getItemsBySeller(String sellerId) {
        return items.values().stream()
                .filter(item -> sellerId.equals(item.getSellerId()))
                .toList();
    }

    @Override
    public boolean update(Item item) {
        if (updateResult) {
            items.put(item.getId(), item);
        }
        return updateResult;
    }

    @Override
    public boolean delete(String itemId) {
        if (deleteResult) {
            items.remove(itemId);
        }
        return deleteResult;
    }
}
