package com.ssscloud.auction.common.model.base;

import java.util.ArrayList;
import java.util.List;

import com.ssscloud.auction.common.payload.response.DTO.ItemDTO;

public abstract class Item extends Entity {
  // tinh theo gia viet nam nen de la long
  private String sellerId;
  private String creator;
  private String description;
  private String type;
  private List<String> imageUrl = new ArrayList<>();

  public Item() {} // dùng trong factory

  public Item(
      String name,
      String sellerID,
      String creator,
      String description,
      String type,
      List<String> imageUrl) {
    super(name);
    this.sellerId = sellerID;
    this.creator = creator;
    this.description = description;
    this.type = type;
    this.imageUrl = imageUrl;
  }

  public Item(
      String id,
      String name,
      String sellerID,
      String creator,
      String description,
      String type,
      List<String> imageUrl) {
    super(id, name);
    this.sellerId = sellerID;
    this.creator = creator;
    this.description = description;
    this.type = type;
    this.imageUrl = imageUrl;
  }

  // them anh xoa anh
  public void addImage(String url) {
    imageUrl.add(url);
  }

  public void delImage(String url) {
    if (imageUrl.remove(url)) System.out.println("xoa thanh cong");
    else System.out.println("khong co anh");
  }

  // getter setter
  // khong thay doi duoc nguoi ban
  public void setSellerId(String sellerId) {
    this.sellerId = sellerId;
  }

  public String getSellerId() {
    return sellerId;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public List<String> getImageUrl() {
    return imageUrl;
  }
  public final ItemDTO toDto() {
        ItemDTO itemDto = createDto();
        itemDto.setId(getId());
        itemDto.setName(getName());
        itemDto.setSellerId(getSellerId());
        itemDto.setCreator(getCreator());
        itemDto.setDescription(getDescription());
        itemDto.setItemType(getType());
        itemDto.setImageUrls(getImageUrl());
        return itemDto;
  }
  protected abstract ItemDTO createDto();
}
