package com.ssscloud.auction.common.enums;

public enum AuctionStatus {

    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED,
    EXPIRED;

    public boolean isActive() {
        return this == OPEN || this == RUNNING;
    }

    public boolean isEnded() {
        return this == FINISHED || this == PAID || this == CANCELED || this == EXPIRED;
    }
}
    

