package com.auction.dao;
 
import com.auction.model.Auction;
import com.auction.model.Auction.AuctionStatus;
import com.auction.model.Item.Item;
import com.auction.config.DBConnection;
 
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
 
public class AuctionDAO {
    public int createAuction(Auction auction) {
        String sql = """
                INSERT INTO auctions (item_id, start_time, end_time, current_price, status)
                VALUES (?, ?, ?, ?, ?)
                """;
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
 
            ps.setInt(1, Integer.parseInt(auction.getItem().getId()));
            ps.setTimestamp(2, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(3, Timestamp.valueOf(auction.getEndTime()));
            ps.setDouble(4, auction.getItem().getStartingPrice());
            ps.setString(5, auction.getStatus().name());
 
            ps.executeUpdate();
 
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
 
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] createAuction lỗi: " + e.getMessage());
        }
        return -1;
    }
 
    /** Lấy một phiên đấu giá theo ID. */
    public Auction getAuctionById(int auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
 
            ps.setInt(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
 
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAuctionById lỗi: " + e.getMessage());
        }
        return null;
    }
 
    /** Lấy tất cả phiên đấu giá. */
    public List<Auction> getAllAuctions() {
        return queryList("SELECT * FROM auctions ORDER BY id DESC");
    }
 
    /** Lấy phiên đấu giá theo trạng thái (OPEN, RUNNING, FINISHED, …). */
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY id DESC";
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
 
            ps.setString(1, status.name());
            return mapResultSet(ps.executeQuery());
 
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAuctionsByStatus lỗi: " + e.getMessage());
        }
        return new ArrayList<>();
    }
 
    /** Lấy phiên đấu giá theo item_id. */
    public Auction getAuctionByItemId(int itemId) {
        String sql = "SELECT * FROM auctions WHERE item_id = ? ORDER BY id DESC LIMIT 1";
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
 
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
 
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] getAuctionByItemId lỗi: " + e.getMessage());
        }
        return null;
    }
    /** Cập nhật giá hiện tại và end_time (dùng sau khi có bid mới / anti-sniping). */
    public boolean updatePriceAndEndTime(int auctionId, double newPrice, LocalDateTime newEndTime) {
        String sql = "UPDATE auctions SET current_price = ?, end_time = ? WHERE id = ?";
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
 
            ps.setDouble(1, newPrice);
            ps.setTimestamp(2, Timestamp.valueOf(newEndTime));
            ps.setInt(3, auctionId);
            return ps.executeUpdate() > 0;
 
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] updatePriceAndEndTime lỗi: " + e.getMessage());
        }
        return false;
    }
 
    /** Cập nhật trạng thái phiên đấu giá. */
    public boolean updateStatus(int auctionId, AuctionStatus status) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
 
            ps.setString(1, status.name());
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
 
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] updateStatus lỗi: " + e.getMessage());
        }
        return false;
    }

    /** Xoá phiên đấu giá (ON DELETE CASCADE sẽ xoá bids liên quan). */
    public boolean deleteAuction(int auctionId) {
        String sql = "DELETE FROM auctions WHERE id = ?";
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
 
            ps.setInt(1, auctionId);
            return ps.executeUpdate() > 0;
 
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] deleteAuction lỗi: " + e.getMessage());
        }
        return false;
    }
 
    private List<Auction> queryList(String sql) {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
 
            return mapResultSet(rs);
 
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] queryList lỗi: " + e.getMessage());
        }
        return new ArrayList<>();
    }
 
    private List<Auction> mapResultSet(ResultSet rs) throws SQLException {
        List<Auction> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }
 
    /**
     * Map một hàng ResultSet → Auction.
     * Item chỉ chứa id + startingPrice (current_price) để tránh N+1 query.
     * Nếu cần đầy đủ, inject ItemDAO rồi gọi itemDAO.getItemById().
     */
    private Auction mapRow(ResultSet rs) throws SQLException {
        int id            = rs.getInt("id");
        int itemId        = rs.getInt("item_id");
        double curPrice   = rs.getDouble("current_price");
        LocalDateTime st  = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime et  = rs.getTimestamp("end_time").toLocalDateTime();
        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
 
        // Tạo Item placeholder – có thể thay bằng ItemDAO.getItemById(itemId)
        Item item = new Item(String.valueOf(itemId));
        item.setStartingPrice(curPrice);
 
        Auction auction = new Auction(
                String.valueOf(id), item, st, et,
                null, null,
                false, 0, 0   // anti-sniping lưu ở in-memory, không có cột trong DB
        );
        auction.setStatus(status);
        return auction;
    }
}
