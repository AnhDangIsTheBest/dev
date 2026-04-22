package com.auction.dao;

import com.auction.config.DBConnection;
import com.auction.model.User.Admin;
import com.auction.model.User.Bidder;
import com.auction.model.User.Seller;
import com.auction.model.User.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /**
     * Thêm user mới vào DB.
     * Trả về true nếu thành công.
     */
    public boolean insert(User user) {
        String sql = """
                INSERT INTO users (username, password, role)
                VALUES (?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] insert lỗi: " + e.getMessage());
        }
        return false;
    }

    /**
     * Đăng nhập: tìm user theo username + password.
     * Trả về User nếu khớp, null nếu sai thông tin.
     */
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            System.err.println("[UserDAO] login lỗi: " + e.getMessage());
        }
        return null;
    }

    /** Lấy toàn bộ danh sách user. */
    public List<User> getAll() {
        String sql = "SELECT * FROM users";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return mapResultSet(rs);

        } catch (SQLException e) {
            System.err.println("[UserDAO] getAll lỗi: " + e.getMessage());
        }
        return new ArrayList<>();
    }
    /** Cập nhật thông tin user theo ID. Trả về true nếu thành công. */
    public boolean update(int id, String username, String password, String role) {
        String sql = """
                UPDATE users SET username = ?, password = ?, role = ?
                WHERE id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] update lỗi: " + e.getMessage());
        }
        return false;
    }

    /** Xoá user theo ID. Trả về true nếu thành công. */
    public boolean delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[UserDAO] delete lỗi: " + e.getMessage());
        }
        return false;
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
        String id       = String.valueOf(rs.getInt("id"));
        String username = rs.getString("username");
        String password = rs.getString("password");
        String role     = rs.getString("role");

        return switch (role.toUpperCase()) {
            case "ADMIN"  -> new Admin(id, username, "", password, "", "NORMAL");
            case "BIDDER" -> new Bidder(id, username, "", "", password, 0.0, 0, 0);
            case "SELLER" -> new Seller(id, username, "", password, "", 0, 0.0);
            default -> {
                System.err.println("[UserDAO] mapRow: unknown role: " + role);
                yield null;
            }
        };
    }
}