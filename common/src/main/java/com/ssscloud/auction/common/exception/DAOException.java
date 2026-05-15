package com.ssscloud.auction.common.exception;

public class DAOException extends BaseException{
    public DAOException(String errorCode, String message){
        super(errorCode, message);
    }
}
