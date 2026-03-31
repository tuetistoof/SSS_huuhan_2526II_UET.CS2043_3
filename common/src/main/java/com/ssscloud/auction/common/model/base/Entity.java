package com.ssscloud.auction.common.model.base;

import java.util.UUID;

public abstract class Entity {
    private String id;
    private String name;
    public Entity(String id, String name){
        this.id = createId();
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

    // ham bo tro
    public String createId()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }
}