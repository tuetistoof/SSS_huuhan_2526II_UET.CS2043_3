package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.ssscloud.auction.common.model.base.AuctionConfig;

/**
 * Tests for AntiSnipingService.
 * WHY: Anti-sniping is a core mechanic of the auction — if the extension logic
 * is wrong, bidders can snipe at the last second without consequence. These tests
 * verify the threshold boundary exactly as documented in CONTEXT.md §4.5.
 */
public class AntiSnipingServiceTest {

    private AuctionConfig buildConfig(LocalDateTime endTime, int extendSecond) {
        return new AuctionConfig(
            "test-auction", "Test Auction",
            10000L, 1000L,
            LocalDateTime.now().minusHours(1),
            endTime,
            extendSecond
        );
    }

    // --- Core anti-sniping trigger tests ---

    @Test
    void testTriggersWhenBidPlacedWithinExtensionWindow() throws Exception {
        // WHY: bid placed 20s before end, extend_second=36 → MUST trigger
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(20);
        AuctionConfig config = buildConfig(endTime, 36);

        LocalDateTime newEndTime = AntiSnipingService.processAntiSniping(config);

        assertNotNull(newEndTime, "Anti-sniping must trigger when remaining time < extend_second");
        assertEquals(endTime.plusSeconds(36), newEndTime,
            "New end time must be original endTime + extendSecond");
    }

    @Test
    void testDoesNotTriggerWhenBidPlacedOutsideWindow() throws Exception {
        // WHY: bid placed 60s before end, extend_second=36 → must NOT trigger
        AuctionConfig config = buildConfig(LocalDateTime.now().plusSeconds(60), 36);

        LocalDateTime result = AntiSnipingService.processAntiSniping(config);

        assertNull(result, "Anti-sniping must NOT trigger when remaining time > extend_second");
    }

    @Test
    void testTriggersAtExactBoundary() throws Exception {
        // WHY: remaining == extend_second is the exact edge case
        // CONTEXT.md: "remainingSeconds <= extensionThreshold → TRIGGER"
        AuctionConfig config = buildConfig(LocalDateTime.now().plusSeconds(36), 36);

        LocalDateTime result = AntiSnipingService.processAntiSniping(config);

        assertNotNull(result, "Anti-sniping must trigger when remaining == extend_second (boundary)");
    }

    @Test
    void testDoesNotTriggerWhenAuctionAlreadyExpired() throws Exception {
        // WHY: endTime in the past → remainingSeconds < 0 → no trigger
        AuctionConfig config = buildConfig(LocalDateTime.now().minusSeconds(10), 36);

        LocalDateTime result = AntiSnipingService.processAntiSniping(config);

        assertNull(result, "Anti-sniping must NOT trigger for already-expired auction");
    }

    @Test
    void testNullConfigReturnsNull() throws Exception {
        // WHY: defensive check — null config must not throw, must return null
        LocalDateTime result = AntiSnipingService.processAntiSniping(null);
        assertNull(result);
    }

    @Test
    void testNullEndTimeReturnsNull() throws Exception {
        // WHY: auction with no end time set — must not throw
        AuctionConfig config = new AuctionConfig(
            "test-id", "Test", 10000L, 1000L,
            LocalDateTime.now().minusHours(1),
            null, 36
        );
        LocalDateTime result = AntiSnipingService.processAntiSniping(config);
        assertNull(result);
    }

    @Test
    void testEndTimeIsUpdatedOnConfig() throws Exception {
        // WHY: after trigger, auctionConfig.getEndTime() must reflect new value
        // ConcurrentBidManager reads from config directly
        LocalDateTime originalEnd = LocalDateTime.now().plusSeconds(10);
        AuctionConfig config = buildConfig(originalEnd, 36);

        AntiSnipingService.processAntiSniping(config);

        assertEquals(originalEnd.plusSeconds(36), config.getEndTime(),
            "Config endTime must be mutated in-place after anti-sniping trigger");
    }
}