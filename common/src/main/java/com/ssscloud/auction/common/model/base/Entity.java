package com.ssscloud.auction.common.model.base;


public abstract class Entity {
    private String Id;
    private String Name;
    public Entity(String Id, String Name){
        this.Id = Id;
        this.Name = Name;

    }

    // getter setter
    
    public String getName(String Name){
        return this.Name;
    }
    public void setName(){
        this.Name = Name;   
    }
    
    // Id khong the thay doi
    public String getId() {
        return Id;
    }
}