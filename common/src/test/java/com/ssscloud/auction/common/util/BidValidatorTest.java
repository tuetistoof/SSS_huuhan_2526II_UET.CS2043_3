package com.ssscloud.auction.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BidValidatorTest{

   @Test
   void testIsPositiveBid(){
        assertTrue(BidValidator.isPositiveBid(1000L));
        assertFalse(BidValidator.isPositiveBid(0L));
        assertFalse(BidValidator.isPositiveBid(-5L));
   }

   @Test
    void testIsValidBid() {
        assertTrue(BidValidator.isValidBid(1100, 1000, 100));
        assertFalse(BidValidator.isValidBid(1099, 1000, 100));
        assertTrue(BidValidator.isValidBid(1300, 1000, 100));
    }

    @Test
    void testIsWithinLimit() {
        assertTrue(BidValidator.isWithinLimit(500, 1000));
        assertFalse(BidValidator.isWithinLimit(1500, 1000));
        assertFalse(BidValidator.isWithinLimit(1000, 1000));
    }
}