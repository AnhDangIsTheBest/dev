package com.auction.shared.model.User;

import com.auction.shared.model.Entity;

public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    protected String username,email;
    protected String password;
    protected String fullname;
    protected double balance;
    protected int totalBids;
    protected int wonAuctions;
    protected int totalItemslisted;
    protected double totalRevenue;
    public User(){};
    public User(String id, String username, String email,String password, String fullname){
        super(id);
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullname = fullname;
    }
    public String getUsername(){
        return username;
    }

    public String getEmail(){
        return email;
    }

    public String getPassword() {return password; }

    public String getId() {return id; }

    public String getFullname(){
        return fullname;
    }

    public abstract String getRole();

    public double getBalance(){ return balance;}
    public int getTotalBids(){ return totalBids;}
    public int getWonAuctions(){ return wonAuctions;}
    public int getTotalItemslisted(){ return totalItemslisted;}
    public double getTotalRevenue(){ return totalRevenue;}

    public void setBalance(double balance){ this.balance = balance;}
    public void setTotalBids(int totalBids){ this.totalBids = totalBids;}
    public void setWonAuctions(int wonAuctions){ this.wonAuctions = wonAuctions;}
    public void setTotalItemslisted(int totalItemslisted){ this.totalItemslisted = totalItemslisted;}
    public void setTotalRevenue(double totalRevenue){ this.totalRevenue = totalRevenue;}
    public void incrementBids(){this.totalBids++;}
    public void incrementWon(){ this.wonAuctions++;}
    public void incrementItemsListed(){ this.totalItemslisted++;}
    public void addRevenue(double amount){ this.totalRevenue += amount;}

    @Override 
    public String display(){
        return String.format("[ %s ] | %s (%s) - %s ",getRole(),fullname,username,email);
    }
    public void setUsername(String newUsername){
        this.username = newUsername;
    }
    public void setPassword(String newPassword){
        this.password = newPassword;
    }
    public void setFullName(String fullName){
        this.fullname = fullName;
    }

    public boolean checkPassword(String pwd){
        return this.password.equals(pwd);
    }
}
