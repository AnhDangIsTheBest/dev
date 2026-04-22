package com.auction.test;

import com.auction.config.DBConnection;

public class TestDB {
    public static void main(String[] args) {
        if (DBConnection.getConnection() != null) {
            System.out.println("Connected!");
        } else {
            System.out.println("Failed!");
        }
    }
}