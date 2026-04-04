package com.ssscloud.auction.common.enums;


public enum BidType {
    MANUAL,
    AUTO;

    public boolean isAutoBid() {
        return this == AUTO;
    }
}