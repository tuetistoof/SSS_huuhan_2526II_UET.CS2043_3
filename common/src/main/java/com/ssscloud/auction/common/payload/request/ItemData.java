package com.ssscloud.auction.common.payload.request;

import java.io.Serializable;
import java.util.List;

/** Request để tạo item mới */
public class ItemData implements Serializable {
  private static final long serialVersionUID = 1L;

  private String name;
  private String creator;
  private String description;
  private String itemType; // ART, ELECTRONIC, VEHICLE
  // art
  private boolean hasCertificate;
  // electronic, vehicle
  private boolean isRepaired; // đã qua sửa chữa
  private int warrantyPeriod; // bảo hành (tháng), 0 = không có

  private List<String> imageUrls;

  public ItemData() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getItemType() {
    return itemType;
  }

  public void setItemType(String itemType) {
    this.itemType = itemType;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public List<String> getImageUrls() {
    return imageUrls;
  }

  public void setImageUrls(List<String> imageUrls) {
    this.imageUrls = imageUrls;
  }

  public boolean isHasCertificate() {
    return hasCertificate;
  }

  public void setHasCertificate(boolean b) {
    this.hasCertificate = b;
  }

  public boolean isRepaired() {
    return isRepaired;
  }

  public void setIsRepaired(boolean b) {
    this.isRepaired = b;
  }

  public int getWarrantyPeriod() {
    return warrantyPeriod;
  }

  public void setWarrantyPeriod(int w) {
    this.warrantyPeriod = w;
  }

  @Override
  public String toString() {
    return "ItemData{name='"
        + name
        + "', creator='"
        + creator
        + "', hasCertificate="
        + hasCertificate
        + ", isRepaired="
        + isRepaired
        + ", warrantyPeriod="
        + warrantyPeriod
        + '}';
  }
}
