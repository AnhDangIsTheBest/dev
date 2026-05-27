package com.auction.server.dao;

import com.auction.server.config.DBConnection;
import com.auction.shared.model.AutoBidConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO {

    public boolean save(String auctionId, AutoBidConfig config) {
        String sql = """
                INSERT INTO auto_bid_configs (
                    auction_id, bidder_id, bidder_name, max_bid, increment, registered_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    bidder_name = VALUES(bidder_name),
                    max_bid = VALUES(max_bid),
                    increment = VALUES(increment),
                    registered_at = VALUES(registered_at)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            ps.setString(2, config.getBidderId());
            ps.setString(3, config.getBidderName());
            ps.setDouble(4, config.getMaxBid());
            ps.setDouble(5, config.getIncrement());
            ps.setLong(6, config.getRegisteredAt());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AutoBidDAO] save lỗi: " + e.getMessage());
            return false;
        }
    }

    public AutoBidConfig findByAuctionAndBidder(String auctionId, String bidderId) {
        String sql = """
                SELECT * FROM auto_bid_configs
                WHERE auction_id = ? AND bidder_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            ps.setString(2, bidderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[AutoBidDAO] findByAuctionAndBidder lỗi: " + e.getMessage());
        }
        return null;
    }

    public List<AutoBidConfig> getByAuction(String auctionId) {
        String sql = """
                SELECT * FROM auto_bid_configs
                WHERE auction_id = ?
                ORDER BY registered_at ASC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            System.err.println("[AutoBidDAO] getByAuction lỗi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean delete(String auctionId, String bidderId) {
        String sql = "DELETE FROM auto_bid_configs WHERE auction_id = ? AND bidder_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            ps.setString(2, bidderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AutoBidDAO] delete lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteByAuction(String auctionId) {
        String sql = "DELETE FROM auto_bid_configs WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[AutoBidDAO] deleteByAuction lỗi: " + e.getMessage());
            return false;
        }
    }

    private List<AutoBidConfig> mapResultSet(ResultSet rs) throws SQLException {
        List<AutoBidConfig> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private AutoBidConfig mapRow(ResultSet rs) throws SQLException {
        return new AutoBidConfig(
                rs.getString("bidder_id"),
                rs.getString("bidder_name"),
                rs.getDouble("max_bid"),
                rs.getDouble("increment"),
                rs.getLong("registered_at")
        );
    }
}
