package com.auction.dao;

import com.auction.config.DBConnection;
import com.auction.model.Item.Art;
import com.auction.model.Item.Electronics;
import com.auction.model.Item.Item;
import com.auction.model.Item.OtherItem;
import com.auction.model.Item.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemDAO {

    public boolean insert(Item item, String sellerId) {
        String id = item.getId() != null && !item.getId().isBlank()
                ? item.getId()
                : UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String sql = """
                INSERT INTO items (
                    id, name, description, type, starting_price, current_price, status, seller_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, item.getName());
                ps.setString(3, item.getDescription());
                ps.setString(4, dbType(item));
                ps.setDouble(5, item.getStartingPrice());
                ps.setDouble(6, item.getCurrentPrice());
                ps.setString(7, item.getStatus());
                ps.setString(8, sellerId);
                ps.executeUpdate();
            }

            insertSpecific(conn, id, item);
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] insert lỗi: " + e.getMessage());
            return false;
        }
    }

    public Item findById(String itemId) {
        String sql = """
                SELECT i.*,
                       e.brand AS electronics_brand, e.model, e.warranty,
                       v.brand AS vehicle_brand, v.vehicle_model, v.year, v.mileage, v.vehicle_type,
                       a.artist, a.year_created, a.material,
                       o.category
                FROM items i
                LEFT JOIN item_electronics e ON e.item_id = i.id
                LEFT JOIN item_vehicles v ON v.item_id = i.id
                LEFT JOIN item_arts a ON a.item_id = i.id
                LEFT JOIN item_others o ON o.item_id = i.id
                WHERE i.id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[ItemDAO] findById lỗi: " + e.getMessage());
        }
        return null;
    }

    public List<Item> getAllItems() {
        return queryItems("""
                SELECT i.*,
                       e.brand AS electronics_brand, e.model, e.warranty,
                       v.brand AS vehicle_brand, v.vehicle_model, v.year, v.mileage, v.vehicle_type,
                       a.artist, a.year_created, a.material,
                       o.category
                FROM items i
                LEFT JOIN item_electronics e ON e.item_id = i.id
                LEFT JOIN item_vehicles v ON v.item_id = i.id
                LEFT JOIN item_arts a ON a.item_id = i.id
                LEFT JOIN item_others o ON o.item_id = i.id
                ORDER BY i.id DESC
                """);
    }

    public List<Item> getItemsBySeller(String sellerId) {
        String sql = """
                SELECT i.*,
                       e.brand AS electronics_brand, e.model, e.warranty,
                       v.brand AS vehicle_brand, v.vehicle_model, v.year, v.mileage, v.vehicle_type,
                       a.artist, a.year_created, a.material,
                       o.category
                FROM items i
                LEFT JOIN item_electronics e ON e.item_id = i.id
                LEFT JOIN item_vehicles v ON v.item_id = i.id
                LEFT JOIN item_arts a ON a.item_id = i.id
                LEFT JOIN item_others o ON o.item_id = i.id
                WHERE i.seller_id = ?
                ORDER BY i.id DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sellerId);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            System.err.println("[ItemDAO] getItemsBySeller lỗi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean update(Item item) {
        String sql = """
                UPDATE items
                SET name = ?, description = ?, type = ?, starting_price = ?, current_price = ?, status = ?
                WHERE id = ?
                """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, item.getName());
                ps.setString(2, item.getDescription());
                ps.setString(3, dbType(item));
                ps.setDouble(4, item.getStartingPrice());
                ps.setDouble(5, item.getCurrentPrice());
                ps.setString(6, item.getStatus());
                ps.setString(7, item.getId());
                ps.executeUpdate();
            }

            deleteSpecific(conn, item.getId());
            insertSpecific(conn, item.getId(), item);
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] update lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(String itemId) {
        String sql = "DELETE FROM items WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ItemDAO] delete lỗi: " + e.getMessage());
            return false;
        }
    }

    private List<Item> queryItems(String sql) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("[ItemDAO] queryItems lỗi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Item> mapResultSet(ResultSet rs) throws SQLException {
        List<Item> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        String type = rs.getString("type");
        double startingPrice = rs.getDouble("starting_price");
        double currentPrice = rs.getDouble("current_price");
        String status = rs.getString("status");

        return switch (type.toUpperCase()) {
            case "ELECTRONICS" -> new Electronics(
                    id, name, description, startingPrice, status, currentPrice,
                    rs.getString("electronics_brand"),
                    rs.getInt("warranty"),
                    rs.getString("model")
            );
            case "VEHICLE" -> new Vehicle(
                    id, name, startingPrice, currentPrice, status, description,
                    rs.getString("vehicle_brand"),
                    rs.getString("vehicle_model"),
                    rs.getInt("year"),
                    rs.getInt("mileage"),
                    rs.getString("vehicle_type")
            );
            case "ART" -> new Art(
                    id, name, startingPrice, currentPrice, status, description,
                    rs.getString("artist"),
                    rs.getInt("year_created"),
                    rs.getString("material")
            );
            case "OTHER" -> new OtherItem(
                    id, name, startingPrice, currentPrice, status, description,
                    rs.getString("category")
            );
            default -> throw new SQLException("[ItemDAO] mapRow: unknown item type: " + type);
        };
    }

    private void insertSpecific(Connection conn, String itemId, Item item) throws SQLException {
        if (item instanceof Electronics e) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO item_electronics (item_id, brand, model, warranty)
                    VALUES (?, ?, ?, ?)
                    """)) {
                ps.setString(1, itemId);
                ps.setString(2, e.getBrand());
                ps.setString(3, e.getModel());
                ps.setInt(4, e.getWarranty());
                ps.executeUpdate();
            }
        } else if (item instanceof Vehicle v) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO item_vehicles (item_id, brand, vehicle_model, year, mileage, vehicle_type)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setString(1, itemId);
                ps.setString(2, v.getBrand());
                ps.setString(3, v.getVehicleModel());
                ps.setInt(4, v.getYear());
                ps.setInt(5, v.getMileage());
                ps.setString(6, v.getVehicleType());
                ps.executeUpdate();
            }
        } else if (item instanceof Art a) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO item_arts (item_id, artist, year_created, material)
                    VALUES (?, ?, ?, ?)
                    """)) {
                ps.setString(1, itemId);
                ps.setString(2, a.getArtist());
                ps.setInt(3, a.getYearCreated());
                ps.setString(4, a.getMaterial());
                ps.executeUpdate();
            }
        } else if (item instanceof OtherItem o) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO item_others (item_id, category)
                    VALUES (?, ?)
                    """)) {
                ps.setString(1, itemId);
                ps.setString(2, o.getCategory());
                ps.executeUpdate();
            }
        }
    }

    private void deleteSpecific(Connection conn, String itemId) throws SQLException {
        String[] tables = {"item_electronics", "item_vehicles", "item_arts", "item_others"};
        for (String table : tables) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE item_id = ?")) {
                ps.setString(1, itemId);
                ps.executeUpdate();
            }
        }
    }

    private String dbType(Item item) {
        return item.getType().toUpperCase();
    }
}
