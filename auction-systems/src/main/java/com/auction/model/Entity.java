package com.auction.model;
import java.io.Serializable;
public  abstract class Entity implements Serializable{
    protected String id;
    public Entity(String id){
        this.id  = id;
    }

    public abstract String display();

}