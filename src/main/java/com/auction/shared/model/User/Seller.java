package com.auction.shared.model.User;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    public Seller(String id, String username, String email, String password, String fullname,
                  int totalItemlisted, double totalRevenue) {
        super(id, username, email, password, fullname);
        this.totalItemslisted = totalItemlisted;
        this.totalRevenue = totalRevenue;
    }

    @Override
    public String getRole() {
        return "SELLER";
    }

    @Override
    public String display() {
        return super.display() + String.format(
                " | Balance: %.1f | Bids: %d | Won: %d | Total Items: %d | Revenue: %.1f",
                balance, totalBids, wonAuctions, totalItemslisted, totalRevenue);
    }
}
