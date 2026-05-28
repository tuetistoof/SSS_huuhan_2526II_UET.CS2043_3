package com.ssscloud.auction.common.model.item;

import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.payload.response.DTO.ElectricDTO;
import com.ssscloud.auction.common.payload.response.DTO.ItemDTO;

import java.util.List;

public class Electronic extends Item {
  private boolean isRepaired;
  private int warrantyPeriod;

  // constructor ko co 2 thuoc tinh rieng cua Electronic vi chi khi mua hang moi xuat hien trang
  // thai day nen lat nua viet o setter

  public Electronic() {} // dùng trong factory

  public Electronic(
      String name,
      String sellerId,
      String creator,
      String description,
      String type,
      List<String> imageUrl,
      boolean isRepaired,
      int warrantyPeriod) {
    super(name, sellerId, creator, description, type, imageUrl);
    this.isRepaired = isRepaired;
    this.warrantyPeriod = warrantyPeriod;
  }

  public Electronic(
      String id,
      String name,
      String sellerId,
      String creator,
      String description,
      String type,
      List<String> imageUrl,
      boolean isRepaired,
      int warrantyPeriod) {
    super(id, name, sellerId, creator, description, type, imageUrl);
    this.isRepaired = isRepaired;
    this.warrantyPeriod = warrantyPeriod;
  }

  public boolean checkIsRepair() {
    return isRepaired;
  }

  @Override
  public String getType() {
    return "ELECTRONIC";
  }

  // getter setter

  public boolean getIsRepaired() {
    return isRepaired;
  }

  public void setIsRepaired(boolean isRepaired) {
    this.isRepaired = isRepaired;
  }

  public int getWarrantyPeriod() {
    return warrantyPeriod;
  }

  public void setWarrantyPeriod(int warrantyPeriod) {
    this.warrantyPeriod = warrantyPeriod;
  }
  @Override
  protected ItemDTO createDto() {
    ElectricDTO electricDto = new ElectricDTO();
    electricDto.setIsRepaired(getIsRepaired());
    electricDto.setWarrantyPeriod(getWarrantyPeriod());
    return electricDto;
  }
}
