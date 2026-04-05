package com.ssscloud.auction.common.model.base;

import java.util.UUID;

public abstract class Entity {
    private String id;
    private String name;
    public Entity(){}
    public Entity(String name){
        this.id = createId();
        this.name = name;

    }

    // getter setter
    // Id khong the thay doi
    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    // ham bo tro
    public String createId()
    {
        return UUID.randomUUID().toString();
    }
}