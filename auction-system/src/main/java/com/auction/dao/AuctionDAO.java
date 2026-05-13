package com.auction.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.auction.config.DBConnection;
import com.auction.model.Auction;
import com.auction.model.Auction.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.model.Item.Item;

public class AuctionDAO {
    private final ItemDAO itemDAO = new ItemDAO();

    public String createAuction(Auction auction) {
        String sql = """
                INSERT INTO auctions (
                    id, item_id, current_price, lead_bidder_id, lead_bidder_name,
                    start_time, end_time, status,
                    anti_sniping_enabled, snipe_window_seconds, snipe_extend_seconds
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String id = auction.getAuctionId() != null && !auction.getAuctionId().isBlank()
                ? auction.getAuctionId()
                : UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.setString(2, auction.getItem().getId());
            ps.setDouble(3, auction.getCurrentPrice());
            ps.setString(4, auction.getLeadBidderId());
            ps.setString(5, auction.getLeadBidderName());
            ps.setTimestamp(6, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(7, Timestamp.valueOf(auction.getEndTime()));
            ps.setString(8, auction.getStatus().name());
            ps.setBoolean(9, auction.isAntiSnipingEnabled());
            ps.setInt(10, auction.snipeWindowSeconds());
            ps.setInt(11, auction.snipeExtendSeconds());

            ps.executeUpdate();
            return id;
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] createAuction lỗi: " + e.getMessage());
            return null;
        }
    }

    public Auction getAuctionById(String auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAuctionById lỗi: " + e.getMessage());
        }
        return null;
    }

    public List<Auction> getAllAuctions() {
        return queryList("SELECT * FROM auctions ORDER BY created_at DESC, id DESC");
    }

    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY created_at DESC, id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAuctionsByStatus lỗi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Auction getAuctionByItemId(String itemId) {
        String sql = "SELECT * FROM auctions WHERE item_id = ? ORDER BY created_at DESC, id DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAuctionByItemId lỗi: " + e.getMessage());
        }
        return null;
    }

    public boolean updatePriceAndEndTime(String auctionId, double newPrice, LocalDateTime newEndTime) {
        String sql = "UPDATE auctions SET current_price = ?, end_time = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, newPrice);
            ps.setTimestamp(2, Timestamp.valueOf(newEndTime));
            ps.setString(3, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] updatePriceAndEndTime lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean updateAfterBid(String auctionId, BidTransaction bid, LocalDateTime newEndTime) {
        String sql = """
                UPDATE auctions
                SET current_price = ?, lead_bidder_id = ?, lead_bidder_name = ?, end_time = ?
                WHERE id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, bid.getAmount());
            ps.setString(2, bid.getBidderId());
            ps.setString(3, bid.getBidderName());
            ps.setTimestamp(4, Timestamp.valueOf(newEndTime));
            ps.setString(5, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] updateAfterBid lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(String auctionId, AuctionStatus status) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setString(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] updateStatus lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteAuction(String auctionId) {
        String sql = "DELETE FROM auctions WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] deleteAuction lỗi: " + e.getMessage());
            return false;
        }
    }

    private List<Auction> queryList(String sql) {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] queryList lỗi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Auction> mapResultSet(ResultSet rs) throws SQLException {
        List<Auction> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Auction mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String itemId = rs.getString("item_id");
        double currentPrice = rs.getDouble("current_price");
        String leadBidderId = rs.getString("lead_bidder_id");
        String leadBidderName = rs.getString("lead_bidder_name");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
        boolean antiSnipingEnabled = rs.getBoolean("anti_sniping_enabled");
        int snipeWindowSeconds = rs.getInt("snipe_window_seconds");
        int snipeExtendSeconds = rs.getInt("snipe_extend_seconds");

        Item item = itemDAO.findById(itemId);
        if (item == null) {
            throw new SQLException("Không tìm thấy item cho auction: " + itemId);
        }

        item.setCurrentPrice(currentPrice);

        Auction auction = new Auction(
                id,
                item,
                startTime,
                endTime,
                leadBidderName,
                leadBidderId,
                antiSnipingEnabled,
                snipeWindowSeconds,
                snipeExtendSeconds
        );
        auction.setCurrentPrice(currentPrice);
        auction.setStatus(status);
        auction.setEndTime(endTime);

        // Constructor Auction hiện tại đang bỏ qua leadBidder/currentPrice truyền vào.
        // Tạm đồng bộ lại bằng applyBid nếu DB có leader.
        if (leadBidderId != null && !leadBidderId.isBlank()) {
            auction.applyBid(new BidTransaction(
                    "DB_LEAD",
                    id,
                    leadBidderId,
                    leadBidderName != null ? leadBidderName : "Unknown",
                    currentPrice,
                    LocalDateTime.now(),
                    false
            ));
        }

        return auction;
    }
}
