package com.ssscloud.auction.common.exception;

public class Exceptions extends RuntimeException{
    private final String errorCode;
    public Exceptions(String errorCode, String message){
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
