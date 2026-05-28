package com.auction.server.dao;

import com.auction.server.config.DBConnection;
import com.auction.shared.model.User.Admin;
import com.auction.shared.model.User.Bidder;
import com.auction.shared.model.User.Seller;
import com.auction.shared.model.User.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDAO {
    private static final Object SCHEMA_LOCK = new Object();
    private static volatile boolean userSchemaReady = false;

    public boolean insert(User user) {
        String id = user.getId() != null && !user.getId().isBlank()
                ? user.getId()
                : UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String sql = """
                INSERT INTO users (
                    id, username, email, password, fullname, role,
                    balance, total_bids, won_auctions,
                    total_items_listed, total_revenue
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection()) {
            ensureUserSchema(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, user.getUsername());
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getPassword());
                ps.setString(5, user.getFullname());
                ps.setString(6, user.getRole());
                fillAllRoleFields(ps, user, 7);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] insert loi: " + e.getMessage());
            return false;
        }
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ensureUserSchema(conn);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] existsByUsername loi: " + e.getMessage());
            throw new RuntimeException("Khong the kiem tra username: " + e.getMessage(), e);
        }
        return false;
    }

    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ensureUserSchema(conn);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[UserDAO] login loi: " + e.getMessage());
        }
        return null;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ensureUserSchema(conn);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] existsByEmail loi: " + e.getMessage());
            throw new RuntimeException("Khong the kiem tra email: " + e.getMessage(), e);
        }
        return false;
    }

    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ensureUserSchema(conn);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[UserDAO] findById loi: " + e.getMessage());
        }
        return null;
    }

    public List<User> getAll() {
        String sql = "SELECT * FROM users ORDER BY created_at DESC, id DESC";

        try (Connection conn = DBConnection.getConnection()) {
            ensureUserSchema(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] getAll loi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean update(User user) {
        String sql = """
                UPDATE users SET
                    username = ?, email = ?, password = ?, fullname = ?, role = ?,
                    balance = ?, total_bids = ?, won_auctions = ?,
                    total_items_listed = ?, total_revenue = ?
                WHERE id = ?
                """;

        try (Connection conn = DBConnection.getConnection()) {
            ensureUserSchema(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPassword());
                ps.setString(4, user.getFullname());
                ps.setString(5, user.getRole());
                fillAllRoleFields(ps, user, 6);
                ps.setString(11, user.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] update loi: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] delete loi: " + e.getMessage());
            return false;
        }
    }

    public boolean deposit(String id, double amount) {
        String sql = "UPDATE users SET balance = COALESCE(balance, 0) + ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            ensureUserSchema(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setString(2, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] deposit loi: " + e.getMessage());
            return false;
        }
    }

    public boolean incrementTotalBids(String id) {
        String sql = "UPDATE users SET total_bids = COALESCE(total_bids, 0) + 1 WHERE id = ?";
        return updateCounter(sql, id);
    }

    public boolean incrementTotalItemsListed(String id) {
        String sql = "UPDATE users SET total_items_listed = COALESCE(total_items_listed, 0) + 1 WHERE id = ?";
        return updateCounter(sql, id);
    }

    private boolean updateCounter(String sql, String id) {
        try (Connection conn = DBConnection.getConnection()) {
            ensureUserSchema(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateCounter loi: " + e.getMessage());
            return false;
        }
    }

    private List<User> mapResultSet(ResultSet rs) throws SQLException {
        List<User> list = new ArrayList<>();
        while (rs.next()) {
            User user = mapRow(rs);
            if (user != null) list.add(user);
        }
        return list;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String password = rs.getString("password");
        String fullname = rs.getString("fullname");
        String role = rs.getString("role");

        String normalizedRole = role == null ? "" : role.toUpperCase();
        return switch (normalizedRole) {
            case "ADMIN" -> new Admin(id, username, email, password, fullname);
            case "BIDDER" -> new Bidder(
                    id, username, email, fullname, password,
                    rs.getDouble("balance"),
                    rs.getInt("total_bids"),
                    rs.getInt("won_auctions")
            );
            case "SELLER" -> new Seller(
                    id, username, email, password, fullname,
                    rs.getInt("total_items_listed"),
                    rs.getDouble("total_revenue")
            );
            default -> {
                System.err.println("[UserDAO] mapRow: unknown role: " + role);
                yield null;
            }
        };
    }

    private void fillAllRoleFields(PreparedStatement ps, User user, int startIndex) throws SQLException {
        if (user instanceof Bidder bidder) {
            ps.setDouble(startIndex, bidder.getBalance());
            ps.setInt(startIndex + 1, bidder.getTotalBids());
            ps.setInt(startIndex + 2, bidder.getWonAuctions());
        } else {
            ps.setDouble(startIndex, 0);
            ps.setInt(startIndex + 1, 0);
            ps.setInt(startIndex + 2, 0);
        }

        if (user instanceof Seller seller) {
            ps.setInt(startIndex + 3, seller.getTotalItemslisted());
            ps.setDouble(startIndex + 4, seller.getTotalRevenue());
        } else {
            ps.setInt(startIndex + 3, 0);
            ps.setDouble(startIndex + 4, 0);
        }
    }

    private void ensureUserSchema(Connection conn) throws SQLException {
        if (userSchemaReady) return;

        synchronized (SCHEMA_LOCK) {
            if (userSchemaReady) return;

            addColumnIfMissing(conn, "balance", "ALTER TABLE users ADD COLUMN balance DECIMAL(15,2) DEFAULT 0");
            addColumnIfMissing(conn, "total_bids", "ALTER TABLE users ADD COLUMN total_bids INT DEFAULT 0");
            addColumnIfMissing(conn, "won_auctions", "ALTER TABLE users ADD COLUMN won_auctions INT DEFAULT 0");
            addColumnIfMissing(conn, "total_items_listed", "ALTER TABLE users ADD COLUMN total_items_listed INT DEFAULT 0");
            addColumnIfMissing(conn, "total_revenue", "ALTER TABLE users ADD COLUMN total_revenue DECIMAL(15,2) DEFAULT 0");

            userSchemaReady = true;
        }
    }

    private void addColumnIfMissing(Connection conn, String columnName, String ddl) throws SQLException {
        if (hasColumn(conn, columnName)) return;

        try (Statement st = conn.createStatement()) {
            st.executeUpdate(ddl);
        } catch (SQLException e) {
            if (!e.getMessage().toLowerCase().contains("duplicate column")) {
                throw e;
            }
        }
    }

    private boolean hasColumn(Connection conn, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "users", columnName)) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "USERS", columnName)) {
            return rs.next();
        }
    }
}
