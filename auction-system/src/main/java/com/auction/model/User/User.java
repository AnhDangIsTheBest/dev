package com.auction.model.User;
import com.auction.model.Entity;

public abstract class User extends Entity {
    protected String username,email;
    protected String password;
    protected String fullname;
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
    
    public String getFullname(){
        return fullname;
    }
    public String getPassword() { return password;}
    public abstract String getRole();
    @Override 
    public String display(){
        return String.format("[ %s ] | %s (%s) - %s ",getRole(),fullname,username,email);
    }
    public void setUsername(String newUsername){
        this.username = newUsername;
    }
    public void setPassword(String newPassword){
        this.password = password;
    }
    public void setFullName(String fullName){
        this.fullname = fullName;
    }

    public boolean checkPassword(String pwd){
        return this.password.equals(pwd);
    }
}