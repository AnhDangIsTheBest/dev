package com.auction.model;
import java.io.Serializable;
public  abstract class Entity implements Serializable{
    protected String id;
    public Entity(){};
    public Entity(String id){
        this.id  = id;
    }

    public String getId(){return id;}

    public abstract String display();

}