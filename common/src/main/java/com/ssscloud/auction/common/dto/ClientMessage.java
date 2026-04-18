package com.ssscloud.auction.common.dto;

import java.io.Serializable;

/**
 * ClientMessage - Lớp wrapper dùng để gửi message từ Client lên Server
**/
public class ClientMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String action;   // Tên hành động: LOGIN, REGISTER, PLACE_BID, CREATE_AUCTION...
    private Object data;     // Dữ liệu đính kèm (có thể là bất kỳ DTO nào)

    public ClientMessage() {
    }
    public ClientMessage(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    // Getter & Setter
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ClientMessage{" +
                "action='" + action + '\'' +
                ", data=" + data +
                '}';
    }
}