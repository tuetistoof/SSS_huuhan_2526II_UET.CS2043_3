package com.ssscloud.auction.common.exception;

public class AuctionException extends RuntimeException {
    private final String errorCode; //để gọi tên 1 số lỗi như BID_TOO_LOW,...

    public AuctionException(String errorCode, String message){
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode(){
        return this.errorCode;
    }



    
}
