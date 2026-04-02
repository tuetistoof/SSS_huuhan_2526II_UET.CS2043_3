package com.ssscloud.auction.common.exception;

public class InvalidBidException extends AuctionException {
    public InvalidBidException(String message){
        super("INVALID_BID", message);
    
    }
    public InvalidBidException(){
        super("INVALID_BID", "Your bid is invalid, try again");
    }

}
