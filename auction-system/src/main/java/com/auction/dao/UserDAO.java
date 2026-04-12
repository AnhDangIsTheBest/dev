package com.auction.dao;

import com.auction.model.User.User;
import com.auction.model.User.Admin;
import com.auction.model.User.Bidder;
import com.auction.model.User.Seller;
import com.auction.config.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Login
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractUser(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Insert user
    public boolean insert(User user) {
        String sql = "INSERT INTO users(username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Get all users
    public List<User> getAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User user = extractUser(rs);
                if (user != null) {
                    list.add(user);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Delete user
    public boolean delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Update user
    public boolean update(int id, String username, String password, String role) {
        String sql = "UPDATE users SET username = ?, password = ?, role = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setInt(4, id);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ResultSet -> User subclass
    private User extractUser(ResultSet rs) throws SQLException {
        String id = String.valueOf(rs.getInt("id"));
        String username = rs.getString("username");
        String password = rs.getString("password");
        String role = rs.getString("role");

        if ("ADMIN".equalsIgnoreCase(role)) {
            return new Admin(id, username, "", password, "", "NORMAL");
        } else if ("BIDDER".equalsIgnoreCase(role)) {
            return new Bidder(id, username, "", "", password, 0.0, 0, 0);
        } else if ("SELLER".equalsIgnoreCase(role)) {
            return new Seller(id, username, "", password, "", 0, 0.0);
        }

        return null;
    }
}