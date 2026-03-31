package com.ssscloud.auction.common.model.base;


public abstract class Entity {
    private String id;
    private String name;
    public Entity(String id, String name){
        this.id = id;
        this.name = name;

    }

    // getter setter
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    // Id khong the thay doi
    public String getId() {
        return id;
    }
}