package com.auction.shared.model.User;


public class Admin extends User{
    private static final long serialVersionUID = 1L;

    public Admin(String id, String username, String email, String password, String fullname){
        super(id,username,email,password,fullname);
    }
    @Override
    public String getRole(){ return "ADMIN";}

    @Override 
    public String display(){
        return super.display() + " | ADMIN";
        
    }
}
