package com.auction.shared.model.User;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder(String id, String username, String email, String fullname, String password,
                  double balance, int totalBids, int wonAuctions) {
        super(id, username, email, password, fullname);
        this.balance = balance;
        this.totalBids = totalBids;
        this.wonAuctions = wonAuctions;
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    @Override
    public String display() {
        return super.display() + String.format(
                " | Balance: %.1f | Bids: %d | Won: %d | Items: %d | Revenue: %.1f",
                balance, totalBids, wonAuctions, totalItemslisted, totalRevenue);
    }
}
