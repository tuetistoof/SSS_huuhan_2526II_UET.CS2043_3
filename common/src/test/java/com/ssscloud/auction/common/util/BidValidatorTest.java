package com.ssscloud.auction.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BidValidatorTest {

    // --- isPositiveBid ---

    @Test
    void testIsPositiveBidWithPositiveAmount() {
        assertTrue(BidValidator.isPositiveBid(1000L));
    }

    @Test
    void testIsPositiveBidWithZero() {
        assertFalse(BidValidator.isPositiveBid(0L));
    }

    @Test
    void testIsPositiveBidWithNegativeAmount() {
        assertFalse(BidValidator.isPositiveBid(-5L));
    }

    // --- isValidBid ---

    @Test
    void testIsValidBidWhenAmountMeetsMinIncrement() {
        // 1100 >= 1000 + 100 → valid
        assertTrue(BidValidator.isValidBid(1100, 1000, 100));
    }

    @Test
    void testIsValidBidWhenAmountBelowMinIncrement() {
        // 1099 < 1000 + 100 → invalid
        assertFalse(BidValidator.isValidBid(1099, 1000, 100));
    }

    @Test
    void testIsValidBidWhenAmountExceedsMinIncrement() {
        // 1300 >= 1000 + 100 → valid
        assertTrue(BidValidator.isValidBid(1300, 1000, 100));
    }

    // --- isWithinLimit ---

    @Test
    void testIsWithinLimitWhenBelowLimit() {
        assertTrue(BidValidator.isWithinLimit(500, 1000));
    }

    @Test
    void testIsWithinLimitWhenExceedsLimit() {
        assertFalse(BidValidator.isWithinLimit(1500, 1000));
    }

    @Test
    void testIsWithinLimitWhenEqualToLimit() {
        // uses strict < not <=, so equal is NOT within limit
        assertFalse(BidValidator.isWithinLimit(1000, 1000));
    }
}