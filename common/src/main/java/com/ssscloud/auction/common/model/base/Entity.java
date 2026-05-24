package com.ssscloud.auction.common.model.base;

import java.util.UUID;

public abstract class Entity {
  private String id;
  private String name;

  public Entity() {
    this.id = createId();
  }

  public Entity(String name) {
    this.id = createId();
    this.name = name;
  }

  public Entity(String id, String name) {
    this.id = id;
    this.name = name;
  }

  // getter setter
  // Id khong the thay doi
  public void setId(String id) {
    this.id = id;
  }

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
  public String createId() {
    return UUID.randomUUID().toString();
  }
}
