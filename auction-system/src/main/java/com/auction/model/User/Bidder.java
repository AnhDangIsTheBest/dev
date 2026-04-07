package com.auction.model.User;

public class Bidder extends User {
    private double balance;
    private int totalBids;
    private int wonAuctions;
    public Bidder(String id,String username, String email,String fullname,String password,double balance, int totalBids, int wonAuctions){
        super(id,username,email,password,fullname);
        this.balance = balance;
        this.totalBids = totalBids;
        this.wonAuctions = wonAuctions;
    }

    @Override
    public String getRole(){
        return "BIDDER";
    }

    public double getBalance(){ return balance;}
    public int getTotalBids(){ return totalBids;}
    public int getWonAuctions(){ return wonAuctions;}
    public void incrementBids(){this.totalBids++;}
    public void incrementWon(){ this.wonAuctions++;}

    @Override
    public String display(){
        return super.display() + String.format(" | Balance: %.1f | Bids: %d | Won: %d",balance,totalBids,wonAuctions);
    }
}
