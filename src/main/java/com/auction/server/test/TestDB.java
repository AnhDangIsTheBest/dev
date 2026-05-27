package com.auction.server.test;

import com.auction.server.config.DBConnection;

import java.sql.Connection;

public class TestDB {
    public static void main(String[] args) {
        System.out.println("===== TEST DB CONNECTION =====");

        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Connected!");
                System.out.println("Database: " + conn.getCatalog());
            } else {
                System.out.println("Connection is null or closed!");
            }
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
