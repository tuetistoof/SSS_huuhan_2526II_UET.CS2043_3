package com.ssscloud.auction.common.payload.response.request;

import com.ssscloud.auction.common.payload.response.DTO.ItemDTO;
import java.io.Serializable;
import java.util.List;

/** Response chứa danh sách items */
public class ItemListResponse implements Serializable {
  private static final long serialVersionUID = 1L;

  private List<ItemDTO> items;
  private int total;

  public ItemListResponse() {}

  public ItemListResponse(List<ItemDTO> items) {
    this.items = items;
    this.total = items.size();
  }

  public ItemListResponse(List<ItemDTO> items, int total) {
    this.items = items;
    this.total = total;
  }

  public List<ItemDTO> getItems() {
    return items;
  }

  public void setItems(List<ItemDTO> items) {
    this.items = items;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  @Override
  public String toString() {
    return "ItemListResponse{" + "total=" + total + ", items=" + items.size() + '}';
  }
}
