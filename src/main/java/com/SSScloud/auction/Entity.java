package com.SSScloud.auction;


public abstract class Entity {
    private String Name, Id;

    public Entity(String Id, String Name){
        this.Name = Name;
        this.Id = Id;
    }

    public void setName(){
        this.Name = Name;   
    }

    public String getName(String Name){
        return this.Name;
    }
}