package com.ssscloud.auction.common.enums;

public enum AppConstant {
    // Định nghĩa enum constant kèm giá trị truyền vào constructor
    DEFAULT_EXTENSION_SECONDS(36);

    private final int value;

    // Constructor của enum luôn luôn là private
    AppConstant(int value) {
        this.value = value;
    }

    // Getter để lấy giá trị ra dùng
    public int getValue() {
        return value;
    }
}