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
    private long version;

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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public ItemDTO getItemDTO() {
        return itemDTO;
    }
    public void setItemDTO(ItemDTO itemDTO) {
        this.itemDTO = itemDTO;
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

    @Override
    public String toString() {
        return "AuctionDTO{" +
            "id='"                + id                + '\'' +
            ", name='"            + name              + '\'' +
            ", startPrice="       + startPrice        +
            ", minIncrement="     + minIncrement      +
            ", status="           + status            +
            ", startTime="        + startTime         +
            ", endTime="          + endTime           +
            '}';
    }
}
