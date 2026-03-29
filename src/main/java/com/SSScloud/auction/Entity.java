package com.SSScloud.auction;

public abstract class Entity {
    private String Name;
    public Entity(String Name){
        this.Name = Name;
    }

    public String getName();
}