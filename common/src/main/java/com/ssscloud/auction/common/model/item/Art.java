package com.ssscloud.auction.common.model.item;

import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.payload.response.DTO.ArtDTO;
import com.ssscloud.auction.common.payload.response.DTO.ItemDTO;

import java.time.LocalDate;
import java.util.List;

public class Art extends Item {
  private boolean certificate;

  public Art() {} // dùng trong factory

  public Art(
      String name,
      String sellerId,
      long basePrice,
      LocalDate manufacturingDate,
      String creator,
      String description,
      String type,
      List<String> imageUrl,
      boolean certificate) {
    super(name, sellerId, creator, description, type, imageUrl);
    this.certificate = certificate;
  }

  public Art(
      String id,
      String name,
      String sellerId,
      String creator,
      String description,
      String type,
      List<String> imageUrl,
      boolean certificate) {
    super(id, name, sellerId, creator, description, type, imageUrl);
    this.certificate = certificate;
  }

  @Override
  public String getType() {
    return "ART";
  }

  // @Override
  // public double getPrice() {
  //     return super.getBasePrice() + Math.max (super.getBasePrice() * super.getTransactionFee(),
  // super.getMaxTransactionFee());
  // }

  // getter setter
  public boolean getCertificate() {
    return this.certificate;
  }

  public void setCertificate(boolean certificate) {
    this.certificate = certificate;
  }
  @Override
protected ItemDTO createDto() {
    ArtDTO artDto = new ArtDTO();
    artDto.setCertificate(getCertificate());
    return artDto;
}
}
