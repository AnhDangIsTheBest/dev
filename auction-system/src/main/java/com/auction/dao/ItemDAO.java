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

public class ItemDAO {

    public boolean insert(Item item, int sellerId) {
        String sql = "INSERT INTO items(" +
                "name, description, type, starting_price, status, seller_id, " +
                "brand, model, warranty, vehicle_model, year, mileage, vehicle_type, " +
                "artist, year_created, material, category" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            fillCommonAndSpecificFields(ps, item, sellerId, false);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Item item) {
        String sql = "UPDATE items SET " +
                "name = ?, description = ?, type = ?, starting_price = ?, status = ?, " +
                "brand = ?, model = ?, warranty = ?, vehicle_model = ?, year = ?, mileage = ?, vehicle_type = ?, " +
                "artist = ?, year_created = ?, material = ?, category = ? " +
                "WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            fillCommonAndSpecificFields(ps, item, -1, true);
            ps.setInt(17, Integer.parseInt(item.getId()));

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int itemId) {
        String sql = "DELETE FROM items WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Item findById(int itemId) {
        String sql = "SELECT * FROM items WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items ORDER BY id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    public List<Item> getItemsBySeller(int sellerId) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sellerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    private void fillCommonAndSpecificFields(PreparedStatement ps, Item item, int sellerId, boolean isUpdate)
            throws SQLException {

        ps.setString(1, item.getName());
        ps.setString(2, item.getDescription());
        ps.setString(3, item.getType().toUpperCase());
        ps.setDouble(4, item.getStartingPrice());
        ps.setString(5, item.getStatus());

        int offset;
        if (!isUpdate) {
            ps.setInt(6, sellerId);
            offset = 6;
        } else {
            offset = 5;
        }

        // reset all specific fields về NULL trước
        ps.setNull(offset + 1, Types.VARCHAR); // brand
        ps.setNull(offset + 2, Types.VARCHAR); // model
        ps.setNull(offset + 3, Types.INTEGER); // warranty
        ps.setNull(offset + 4, Types.VARCHAR); // vehicle_model
        ps.setNull(offset + 5, Types.INTEGER); // year
        ps.setNull(offset + 6, Types.INTEGER); // mileage
        ps.setNull(offset + 7, Types.VARCHAR); // vehicle_type
        ps.setNull(offset + 8, Types.VARCHAR); // artist
        ps.setNull(offset + 9, Types.INTEGER); // year_created
        ps.setNull(offset + 10, Types.VARCHAR); // material
        ps.setNull(offset + 11, Types.VARCHAR); // category

        if (item instanceof Electronics e) {
            ps.setString(offset + 1, e.getBrand());
            ps.setString(offset + 2, e.getModel());
            ps.setInt(offset + 3, e.getWarranty());

        } else if (item instanceof Vehicle v) {
            ps.setString(offset + 1, v.getBrand());
            ps.setString(offset + 4, v.getVehicleModel());
            ps.setInt(offset + 5, v.getYear());
            ps.setInt(offset + 6, v.getMileage());
            ps.setString(offset + 7, v.getVehicleType());

        } else if (item instanceof Art a) {
            ps.setString(offset + 8, a.getArtist());
            ps.setInt(offset + 9, a.getYearCreated());
            ps.setString(offset + 10, a.getMaterial());

        } else if (item instanceof OtherItem o) {
            ps.setString(offset + 11, o.getCategory());
        }
    }

    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        String id = String.valueOf(rs.getInt("id"));
        String name = rs.getString("name");
        String description = rs.getString("description");
        String type = rs.getString("type");
        double startingPrice = rs.getDouble("starting_price");
        String status = rs.getString("status");

        switch (type) {
            case "ELECTRONICS":
                return new Electronics(
                        id,
                        name,
                        description,
                        startingPrice,
                        status,
                        startingPrice,
                        rs.getString("brand"),
                        rs.getInt("warranty"),
                        rs.getString("model")
                );

            case "VEHICLE":
                return new Vehicle(
                        id,
                        name,
                        startingPrice,
                        startingPrice,
                        status,
                        description,
                        rs.getString("brand"),
                        rs.getString("vehicle_model"),
                        rs.getInt("year"),
                        rs.getInt("mileage"),
                        rs.getString("vehicle_type")
                );

            case "ART":
                return new Art(
                        id,
                        name,
                        startingPrice,
                        startingPrice,
                        status,
                        description,
                        rs.getString("artist"),
                        rs.getInt("year_created"),
                        rs.getString("material")
                );

            case "OTHER":
                return new OtherItem(
                        id,
                        name,
                        startingPrice,
                        startingPrice,
                        status,
                        description,
                        rs.getString("category")
                );

            default:
                throw new SQLException("Unknown item type: " + type);
        }
    }
}