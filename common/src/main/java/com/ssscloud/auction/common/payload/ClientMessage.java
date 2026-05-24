package com.ssscloud.auction.common.payload;

import java.io.Serializable;

public class ClientMessage implements Serializable {

  private static final long serialVersionUID = 1L;

  // Hai constant để AuctionClientSocket phân loại message
  public static final String TYPE_PUSH = "PUSH";
  public static final String TYPE_RESPONSE = "RESPONSE";

  private String action;
  private String type; // "PUSH" | "RESPONSE"
  private String requestId;
  private Object data;

  public ClientMessage() {}

  public ClientMessage(String action, Object data) {
    this.action = action;
    this.data = data;
  }

  public static ClientMessage request(String action, Object data) {
    ClientMessage msg = new ClientMessage(action, data);
    msg.type = TYPE_RESPONSE;
    return msg;
  }

  public static ClientMessage push(String action, Object data) {
    ClientMessage msg = new ClientMessage(action, data);
    msg.type = TYPE_PUSH;
    return msg;
  }

  // Getters & Setters
  public String getAction() {
    return action;
  }

  public void setAction(String a) {
    this.action = a;
  }

  public String getType() {
    return type;
  }

  public void setType(String t) {
    this.type = t;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object d) {
    this.data = d;
  }

  @Override
  public String toString() {
    return "ClientMessage{action='"
        + action
        + "', type='"
        + type
        + "', requestId='"
        + requestId
        + "', data="
        + data
        + '}';
  }
}
