package com.auction.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.auction.config.DBConnection;
import com.auction.model.Auction;
import com.auction.model.Auction.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.model.Item.Art;
import com.auction.model.Item.Electronics;
import com.auction.model.Item.Item;
import com.auction.model.Item.OtherItem;
import com.auction.model.Item.Vehicle;

public class AuctionDAO {
    private final BidDAO bidDAO = new BidDAO();

    private static final String SELECT_WITH_ITEM = """
            SELECT a.*,
                   i.name          AS item_name,
                   i.description   AS item_description,
                   i.type          AS item_type,
                   i.starting_price,
                   i.current_price AS item_current_price,
                   i.status        AS item_status,
                   i.image_data    AS item_image_data,
                   i.seller_id     AS item_seller_id,
                   e.brand         AS electronics_brand,
                   e.model         AS electronics_model,
                   e.warranty,
                   v.brand         AS vehicle_brand,
                   v.vehicle_model,
                   v.year,
                   v.mileage,
                   v.vehicle_type,
                   ar.artist,
                   ar.year_created,
                   ar.material,
                   o.category
            FROM auctions a
            JOIN items i ON i.id = a.item_id
            LEFT JOIN item_electronics e  ON e.item_id  = i.id
            LEFT JOIN item_vehicles    v  ON v.item_id  = i.id
            LEFT JOIN item_arts        ar ON ar.item_id = i.id
            LEFT JOIN item_others      o  ON o.item_id  = i.id
            """;

    // ── Create ────────────────────────────────────────────────────

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

    // ── Read ──────────────────────────────────────────────────────

    public Auction getAuctionById(String auctionId) {
        String sql = SELECT_WITH_ITEM + "WHERE a.id = ?";

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
        String sql = SELECT_WITH_ITEM + "ORDER BY a.created_at DESC, a.id DESC";
        return queryList(sql);
    }

    /**
     * Lấy tất cả auction mà bidderId đã từng tham gia đặt giá.
     */
    public List<Auction> getAuctionsByBidder(String bidderId) {
        String sql = SELECT_WITH_ITEM + """
                JOIN (
                    SELECT DISTINCT auction_id
                    FROM bid_transactions
                    WHERE bidder_id = ?
                ) b ON b.auction_id = a.id
                ORDER BY a.created_at DESC, a.id DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bidderId);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAuctionsByBidder lỗi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Auction> getAuctionsBySeller(String sellerId) {
        String sql = SELECT_WITH_ITEM + "WHERE i.seller_id = ? ORDER BY a.created_at DESC, a.id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sellerId);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAuctionsBySeller loi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        String sql = SELECT_WITH_ITEM + "WHERE a.status = ? ORDER BY a.created_at DESC, a.id DESC";

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
        String sql = SELECT_WITH_ITEM + "WHERE a.item_id = ? ORDER BY a.created_at DESC, a.id DESC LIMIT 1";

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

    // ── Update ────────────────────────────────────────────────────

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

    // ── Delete ────────────────────────────────────────────────────

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

    // ── Helpers ───────────────────────────────────────────────────

    private List<Auction> queryList(String sql) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

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
        // ── Auction fields ────────────────────────────────────────
        String id             = rs.getString("id");
        String itemId         = rs.getString("item_id");
        double currentPrice   = rs.getDouble("current_price");
        String leadBidderId   = rs.getString("lead_bidder_id");
        String leadBidderName = rs.getString("lead_bidder_name");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime   = rs.getTimestamp("end_time").toLocalDateTime();
        AuctionStatus status    = AuctionStatus.valueOf(rs.getString("status"));
        boolean antiSniping     = rs.getBoolean("anti_sniping_enabled");
        int snipeWindow         = rs.getInt("snipe_window_seconds");
        int snipeExtend         = rs.getInt("snipe_extend_seconds");

        // ── Build Item từ JOIN — không cần thêm DB call ───────────
        String itemType        = rs.getString("item_type");
        String itemName        = rs.getString("item_name");
        String itemDesc        = rs.getString("item_description");
        double startingPrice   = rs.getDouble("starting_price");
        double itemCurrentPrice = rs.getDouble("item_current_price");
        String itemStatus      = rs.getString("item_status");
        byte[] itemImageData   = rs.getBytes("item_image_data");
        String itemSellerId    = rs.getString("item_seller_id");

        Item item = switch (itemType.toUpperCase()) {
            case "ELECTRONICS" -> new Electronics(
                    itemId, itemName, itemDesc, startingPrice, itemStatus, itemCurrentPrice,
                    rs.getString("electronics_brand"),
                    rs.getInt("warranty"),
                    rs.getString("electronics_model")
            );
            case "VEHICLE" -> new Vehicle(
                    itemId, itemName, startingPrice, itemCurrentPrice, itemStatus, itemDesc,
                    rs.getString("vehicle_brand"),
                    rs.getString("vehicle_model"),
                    rs.getInt("year"),
                    rs.getInt("mileage"),
                    rs.getString("vehicle_type")
            );
            case "ART" -> new Art(
                    itemId, itemName, startingPrice, itemCurrentPrice, itemStatus, itemDesc,
                    rs.getString("artist"),
                    rs.getInt("year_created"),
                    rs.getString("material")
            );
            case "OTHER" -> new OtherItem(
                    itemId, itemName, startingPrice, itemCurrentPrice, itemStatus, itemDesc,
                    rs.getString("category")
            );
            default -> throw new SQLException("[AuctionDAO] mapRow: unknown item type: " + itemType);
        };

        item.setCurrentPrice(currentPrice);
        item.setImageData(itemImageData);
        item.setSellerId(itemSellerId);

        // ── Build Auction ─────────────────────────────────────────
        Auction auction = new Auction(
                id,
                item,
                startTime,
                endTime,
                leadBidderName,
                leadBidderId,
                antiSniping,
                snipeWindow,
                snipeExtend
        );
        auction.setCurrentPrice(currentPrice);
        auction.setStatus(status);
        auction.setEndTime(endTime);
        auction.setLeadBidder(leadBidderId, leadBidderName);
        auction.setBidHistory(bidDAO.getBidsByAuction(id));

        return auction;
    }
}
