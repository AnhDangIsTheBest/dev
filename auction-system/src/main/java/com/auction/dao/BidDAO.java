package com.auction.dao;

import com.auction.model.BidTransaction;
import com.auction.config.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    public int placeBid(BidTransaction bid) {
        String sql = """
                INSERT INTO bids (auction_id, bidder_id, amount, bid_time)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, Integer.parseInt(bid.getAuctionId()));
            ps.setInt(2, Integer.parseInt(bid.getBidderId()));
            ps.setDouble(3, bid.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(bid.getLocalDateTime()));

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);

        } catch (SQLException e) {
            System.err.println("[BidDAO] placeBid lỗi: " + e.getMessage());
        }
        return -1;
    }

    // ─────────────────────────────────────────────
    //  READ
    // ─────────────────────────────────────────────

    /** Lấy toàn bộ lịch sử bid của một phiên đấu giá, sắp xếp theo thời gian. */
    public List<BidTransaction> getBidsByAuction(int auctionId) {
        String sql = """
                SELECT b.id, b.auction_id, b.bidder_id, u.username,
                       b.amount, b.bid_time
                FROM bids b
                JOIN users u ON u.id = b.bidder_id
                WHERE b.auction_id = ?
                ORDER BY b.bid_time ASC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            return mapResultSet(ps.executeQuery());

        } catch (SQLException e) {
            System.err.println("[BidDAO] getBidsByAuction lỗi: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /** Lấy bid cao nhất (leader) của một phiên đấu giá. */
    public BidTransaction getLeadBid(int auctionId) {
        String sql = """
                SELECT b.id, b.auction_id, b.bidder_id, u.username,
                       b.amount, b.bid_time
                FROM bids b
                JOIN users u ON u.id = b.bidder_id
                WHERE b.auction_id = ?
                ORDER BY b.amount DESC, b.bid_time ASC
                LIMIT 1
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            System.err.println("[BidDAO] getLeadBid lỗi: " + e.getMessage());
        }
        return null;
    }

    /** Lấy tất cả bid của một người dùng (xem lịch sử cá nhân). */
    public List<BidTransaction> getBidsByBidder(int bidderId) {
        String sql = """
                SELECT b.id, b.auction_id, b.bidder_id, u.username,
                       b.amount, b.bid_time
                FROM bids b
                JOIN users u ON u.id = b.bidder_id
                WHERE b.bidder_id = ?
                ORDER BY b.bid_time DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bidderId);
            return mapResultSet(ps.executeQuery());

        } catch (SQLException e) {
            System.err.println("[BidDAO] getBidsByBidder lỗi: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /** Đếm số lượt bid của một phiên đấu giá. */
    public int countBids(int auctionId) {
        String sql = "SELECT COUNT(*) FROM bids WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.err.println("[BidDAO] countBids lỗi: " + e.getMessage());
        }
        return 0;
    }

    /** Kiểm tra người dùng đã bid trong phiên này chưa. */
    public boolean hasBidded(int auctionId, int bidderId) {
        String sql = "SELECT 1 FROM bids WHERE auction_id = ? AND bidder_id = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            ps.setInt(2, bidderId);
            return ps.executeQuery().next();

        } catch (SQLException e) {
            System.err.println("[BidDAO] hasBidded lỗi: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────

    /** Xoá toàn bộ bid của một phiên (thường dùng khi huỷ auction). */
    public boolean deleteBidsByAuction(int auctionId) {
        String sql = "DELETE FROM bids WHERE auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[BidDAO] deleteBidsByAuction lỗi: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────

    private List<BidTransaction> mapResultSet(ResultSet rs) throws SQLException {
        List<BidTransaction> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private BidTransaction mapRow(ResultSet rs) throws SQLException {
        String transId   = String.valueOf(rs.getInt("id"));
        String auctionId = String.valueOf(rs.getInt("auction_id"));
        String bidderId  = String.valueOf(rs.getInt("bidder_id"));
        String bidderName = rs.getString("username");
        double amount    = rs.getDouble("amount");
        LocalDateTime ts = rs.getTimestamp("bid_time").toLocalDateTime();

        // isAutoBid không lưu trong DB → mặc định false khi đọc từ DB
        return new BidTransaction(transId, auctionId, bidderId, bidderName, amount, ts, false);
    }
}