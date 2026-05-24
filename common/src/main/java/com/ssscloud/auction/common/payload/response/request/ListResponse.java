package com.ssscloud.auction.common.payload.response.request;

import java.io.Serializable;
import java.util.List;

/**
 * DTO tổng quát dùng để trả về danh sách các đối tượng
 *
 * @param <T> Kiểu dữ liệu của các item trong danh sách (AuctionDTO, AuctionDisplayInfo, v.v.)
 */
public class ListResponse<T> implements Serializable {

  private static final long serialVersionUID = 1L;

  private List<T> data;

  public ListResponse() {}

  public ListResponse(List<T> data) {
    this.data = data;
  }

  // Getter & Setter
  public List<T> getData() {
    return data;
  }

  public void setData(List<T> data) {
    this.data = data;
  }
}
