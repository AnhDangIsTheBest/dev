package com.auction.dao;

import com.auction.config.DBConnection;
import com.auction.model.User.Admin;
import com.auction.model.User.Bidder;
import com.auction.model.User.Seller;
import com.auction.model.User.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDAO {

    public boolean insert(User user) {

        String id = user.getId() != null && !user.getId().isBlank()
                ? user.getId()
                : UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String sql = """
                INSERT INTO users (
                    id,
                    username,
                    email,
                    password,
                    fullname,
                    role,
                    balance,
                    total_bids,
                    won_auctions,
                    total_items_listed,
                    total_revenue
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getFullname());
            ps.setString(6, user.getRole());

            fillRoleFields(ps, user);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println("[UserDAO] insert lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean existsByUsername(String username) {

        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {

            System.err.println("[UserDAO] existsByUsername lỗi: " + e.getMessage());
        }

        return false;
    }

    public boolean existsByEmail(String email) {

        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {

            System.err.println("[UserDAO] existsByEmail lỗi: " + e.getMessage());
        }

        return false;
    }

    public User login(String username, String password) {

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (SQLException e) {

            System.err.println("[UserDAO] login lỗi: " + e.getMessage());
        }

        return null;
    }

    public User findById(String id) {

        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (SQLException e) {

            System.err.println("[UserDAO] findById lỗi: " + e.getMessage());
        }

        return null;
    }

    public List<User> getAll() {

        String sql = "SELECT * FROM users ORDER BY id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return mapResultSet(rs);

        } catch (SQLException e) {

            System.err.println("[UserDAO] getAll lỗi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean update(User user) {

        String sql = """
                UPDATE users SET
                    username = ?,
                    email = ?,
                    password = ?,
                    fullname = ?,
                    role = ?,
                    balance = ?,
                    total_bids = ?,
                    won_auctions = ?,
                    total_items_listed = ?,
                    total_revenue = ?
                WHERE id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getFullname());
            ps.setString(5, user.getRole());

            fillRoleFieldsForUpdate(ps, user);

            ps.setString(11, user.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println("[UserDAO] update lỗi: " + e.getMessage());
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

            System.err.println("[UserDAO] delete lỗi: " + e.getMessage());
            return false;
        }
    }

    private List<User> mapResultSet(ResultSet rs) throws SQLException {

        List<User> list = new ArrayList<>();

        while (rs.next()) {

            User user = mapRow(rs);

            if (user != null) {
                list.add(user);
            }
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

        return switch (role.toUpperCase()) {

            case "ADMIN" -> new Admin(
                    id,
                    username,
                    email,
                    password,
                    fullname
            );

            case "BIDDER" -> new Bidder(
                    id,
                    username,
                    email,
                    fullname,
                    password,
                    rs.getDouble("balance"),
                    rs.getInt("total_bids"),
                    rs.getInt("won_auctions")
            );

            case "SELLER" -> new Seller(
                    id,
                    username,
                    email,
                    password,
                    fullname,
                    rs.getInt("total_items_listed"),
                    rs.getDouble("total_revenue")
            );

            default -> {
                System.err.println("[UserDAO] Unknown role: " + role);
                yield null;
            }
        };
    }

    private void fillRoleFields(PreparedStatement ps, User user) throws SQLException {

        if (user instanceof Admin) {

            ps.setDouble(7, 0.0);
            ps.setInt(8, 0);
            ps.setInt(9, 0);
            ps.setInt(10, 0);
            ps.setDouble(11, 0.0);

        } else if (user instanceof Bidder b) {

            ps.setDouble(7, b.getBalance());
            ps.setInt(8, b.getTotalBids());
            ps.setInt(9, b.getWonAuctions());
            ps.setInt(10, 0);
            ps.setDouble(11, 0.0);

        } else if (user instanceof Seller s) {

            ps.setDouble(7, 0.0);
            ps.setInt(8, 0);
            ps.setInt(9, 0);
            ps.setInt(10, s.getTotalItemslisted());
            ps.setDouble(11, s.getTotalRevenue());
        }
    }

    private void fillRoleFieldsForUpdate(PreparedStatement ps, User user) throws SQLException {

        if (user instanceof Admin) {

            ps.setDouble(6, 0.0);
            ps.setInt(7, 0);
            ps.setInt(8, 0);
            ps.setInt(9, 0);
            ps.setDouble(10, 0.0);

        } else if (user instanceof Bidder b) {

            ps.setDouble(6, b.getBalance());
            ps.setInt(7, b.getTotalBids());
            ps.setInt(8, b.getWonAuctions());
            ps.setInt(9, 0);
            ps.setDouble(10, 0.0);

        } else if (user instanceof Seller s) {

            ps.setDouble(6, 0.0);
            ps.setInt(7, 0);
            ps.setInt(8, 0);
            ps.setInt(9, s.getTotalItemslisted());
            ps.setDouble(10, s.getTotalRevenue());
        }
    }
}