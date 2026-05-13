package com.auction.dao;

import com.auction.config.DBConnection;
import com.auction.model.BidTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BidDAO {

    public String placeBid(BidTransaction bid) {
        String sql = """
                INSERT INTO bid_transactions (
                    id, auction_id, bidder_id, bidder_name, amount, bid_time, is_auto_bid
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        String id = bid.getTransactionId() != null && !bid.getTransactionId().isBlank()
                ? bid.getTransactionId()
                : UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.setString(2, bid.getAuctionId());
            ps.setString(3, bid.getBidderId());
            ps.setString(4, bid.getBidderName());
            ps.setDouble(5, bid.getAmount());
            ps.setTimestamp(6, Timestamp.valueOf(bid.getLocalDateTime()));
            ps.setBoolean(7, bid.isAutoBid());

            ps.executeUpdate();
            return id;
        } catch (SQLException e) {
            System.err.println("[BidDAO] placeBid lỗi: " + e.getMessage());
            return null;
        }
    }

    public List<BidTransaction> getBidsByAuction(String auctionId) {
        String sql = """
                SELECT * FROM bid_transactions
                WHERE auction_id = ?
                ORDER BY bid_time ASC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            System.err.println("[BidDAO] getBidsByAuction lỗi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public BidTransaction getLeadBid(String auctionId) {
        String sql = """
                SELECT * FROM bid_transactions
                WHERE auction_id = ?
                ORDER BY amount DESC, bid_time ASC
                LIMIT 1
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[BidDAO] getLeadBid lỗi: " + e.getMessage());
        }
        return null;
    }

    public List<BidTransaction> getBidsByBidder(String bidderId) {
        String sql = """
                SELECT * FROM bid_transactions
                WHERE bidder_id = ?
                ORDER BY bid_time DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bidderId);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            System.err.println("[BidDAO] getBidsByBidder lỗi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public int countBids(String auctionId) {
        String sql = "SELECT COUNT(*) FROM bid_transactions WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[BidDAO] countBids lỗi: " + e.getMessage());
        }
        return 0;
    }

    public boolean hasBidded(String auctionId, String bidderId) {
        String sql = "SELECT 1 FROM bid_transactions WHERE auction_id = ? AND bidder_id = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            ps.setString(2, bidderId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[BidDAO] hasBidded lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteBidsByAuction(String auctionId) {
        String sql = "DELETE FROM bid_transactions WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[BidDAO] deleteBidsByAuction lỗi: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Double> getMyBestBids(String bidderId) {
        String sql = """
                SELECT auction_id, MAX(amount) AS max_amount
                FROM bid_transactions
                WHERE bidder_id = ?
                GROUP BY auction_id
                """;

        Map<String, Double> bestBids = new HashMap<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bidderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bestBids.put(rs.getString("auction_id"), rs.getDouble("max_amount"));
            }
        } catch (SQLException e) {
            System.err.println("[BidDAO] getMyBestBids lá»—i: " + e.getMessage());
        }

        return bestBids;
    }

    private List<BidTransaction> mapResultSet(ResultSet rs) throws SQLException {
        List<BidTransaction> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private BidTransaction mapRow(ResultSet rs) throws SQLException {
        String transactionId = rs.getString("id");
        String auctionId = rs.getString("auction_id");
        String bidderId = rs.getString("bidder_id");
        String bidderName = rs.getString("bidder_name");
        double amount = rs.getDouble("amount");
        LocalDateTime timeStamp = rs.getTimestamp("bid_time").toLocalDateTime();
        boolean isAutoBid = rs.getBoolean("is_auto_bid");

        return new BidTransaction(transactionId, auctionId, bidderId, bidderName, amount, timeStamp, isAutoBid);
    }
}
