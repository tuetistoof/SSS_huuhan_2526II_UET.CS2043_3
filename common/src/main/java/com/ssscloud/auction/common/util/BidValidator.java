package com.ssscloud.auction.common.util;

public class BidValidator {
    public static boolean isValidBid(double newBidAmount, double currentPrice, double minIncrement){
        return (newBidAmount >= currentPrice + minIncrement);
    }

    public static boolean isPositiveBid(long bidAmount){
        return (bidAmount > 0);
    }

    public static boolean isWithinLimit(double bidAmount, double limit){
        return (bidAmount < limit);
    }
    
}
