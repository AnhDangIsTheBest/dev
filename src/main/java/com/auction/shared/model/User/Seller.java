package com.auction.shared.model.User;

import com.auction.shared.model.Entity;

public class Seller extends User {
    private int totalItemlisted;
    private double totalRevenue;

    public Seller(String id, String username, String email, String password, String fullname, int totalItemlisted, double totalRevenue) {
        super(id, username, email, password, fullname);
        this.totalItemlisted = totalItemlisted;
        this.totalRevenue = totalRevenue;
    }

    @Override
    public String display() {
        return super.display() + String.format(" | Total Items: %d | Revenue: %.1f", totalItemlisted, totalRevenue);
    }

    public String getRole() {
        return "SELLER";
    }

    public int getTotalItemslisted() {
        return totalItemlisted;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void addRevenue(double amount) {
        this.totalRevenue += amount;
    }

}
