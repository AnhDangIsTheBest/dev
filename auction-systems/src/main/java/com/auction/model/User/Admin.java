package com.auction.model.User;


public class Admin extends User{
    private String adminLevel;
    public Admin(String id, String username, String email, String password, String fullname, String adminLevel){
        super(id,username,email,password,fullname);
        this.adminLevel = adminLevel;
    }
    @Override
    public String getRole(){ return "ADMIN";}
    
    public String getAdminLevel(){ return adminLevel;}
    public void setAdminLevel(String adminLevel){ this.adminLevel = adminLevel; }
    @Override 
    public String display(){
        return super.display() + String.format(" | Level: %s",adminLevel);
        
    }
}
