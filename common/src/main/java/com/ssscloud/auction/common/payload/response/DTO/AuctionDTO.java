package com.ssscloud.auction.common.payload.response.DTO;

import com.ssscloud.auction.common.enums.AuctionStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private long startPrice;
  private long minIncrement;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private AuctionStatus status;

  private UserDTO sellerDTO;
  private ItemDTO itemDTO;

  private List<BidDTO> bidDto;

  public AuctionDTO() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public long getStartPrice() {
    return startPrice;
  }

  public void setStartPrice(long startPrice) {
    this.startPrice = startPrice;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public void setStatus(AuctionStatus status) {
    this.status = status;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public ItemDTO getItemDTO() {
    return itemDTO;
  }

  public void setItemDTO(ItemDTO itemDTO) {
    this.itemDTO = itemDTO;
  }

  public long getMinIncrement() {
    return minIncrement;
  }

  public void setMinIncrement(long minIncrement) {
    this.minIncrement = minIncrement;
  }

  public List<BidDTO> getBidDto() {
    return bidDto;
  }

  public void setBidDto(List<BidDTO> bidDto) {
    this.bidDto = bidDto;
  }

  public UserDTO getSellerDTO() {
    return sellerDTO;
  }

  public void setSellerDTO(UserDTO sellerDTO) {
    this.sellerDTO = sellerDTO;
  }

  public BidDTO getLastBidDTO() {
    return bidDto.getLast();
  }

  public long getCurrentPrice() {
    if (bidDto.isEmpty()) return startPrice;
    else return bidDto.getLast().getBidAmount();
  }

  @Override
  public String toString() {
    return "AuctionDTO{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", startPrice="
        + startPrice
        + ", minIncrement="
        + minIncrement
        + ", status="
        + status
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + '}';
  }
}
