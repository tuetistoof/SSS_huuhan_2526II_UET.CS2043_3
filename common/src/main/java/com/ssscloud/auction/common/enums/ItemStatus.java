package com.ssscloud.auction.common.enums;

/**
 * Trạng thái của sản phẩm trong hệ thống
 */
public enum ItemStatus {
    /**
     * Sản phẩm vừa được tạo, chưa mở đấu giá
     */
    DRAFT("Nháp"),
    
    /**
     * Sản phẩm đã được xóa (soft delete)
     */
    DELETED("Đã xóa"),
    
    /**
     * Sản phẩm đang trong cuộc đấu giá
     */
    AUCTIONING("Đang đấu giá"),
    
    /**
     * Sản phẩm đã bán (hoàn thành đấu giá)
     */
    SOLD("Đã bán"),
    
    /**
     * Sản phẩm hết hạn đấu giá nhưng chưa bán được (có thể sửa lại)
     */
    EXPIRED("Hết hạn");

    private final String displayName;

    ItemStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEditable() {
        return this == DRAFT || this == EXPIRED;
    }

    public boolean isSold() {
        return this == SOLD;
    }

    public boolean isActive() {
        return this == DRAFT || this == AUCTIONING;
    }
}
