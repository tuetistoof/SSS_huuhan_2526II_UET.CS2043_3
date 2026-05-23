package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.model.base.AuctionConfig;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * AntiSnipingService provides utility logic to prevent "sniping" by extending 
 * the auction duration if a bid is placed very close to the conclusion time.
 */
public class AntiSnipingService {
    private static final Logger logger = Logger.getLogger(AntiSnipingService.class.getName()); // Logging Standards: First attribute

    // --- CONSTRUCTOR ---

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private AntiSnipingService() {
    }

    // --- PUBLIC METHODS ---

    public static LocalDateTime processAntiSniping(AuctionConfig auctionConfig) throws Exception {
        try {
            LocalDateTime updatedEndTime = calculateExtendedEndTime(auctionConfig);
            if (updatedEndTime != null) {
                auctionConfig.setEndTime(updatedEndTime);
                logExtension(auctionConfig, updatedEndTime);
                return updatedEndTime;
            }
            return null;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AntiSnipingService.processAntiSniping: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    public static void logExtension(AuctionConfig auctionConfig, LocalDateTime updatedEndTime) {
        if (auctionConfig == null || updatedEndTime == null) {
            return;
        }
        logger.log(Level.INFO, "Anti-sniping triggered for auction: " + auctionConfig.getName()
                + ". Duration extended by " + auctionConfig.getExtendSecond()
                + "s. Updated conclusion time: " + updatedEndTime);
    }

    public static LocalDateTime calculateExtendedEndTime(AuctionConfig auctionConfig) throws Exception {
        try {
            if (auctionConfig == null || auctionConfig.getEndTime() == null) {
                logger.log(Level.INFO, "Anti-sniping processing skipped: target auction configuration or conclusion time is null.");
                return null;
            }

            LocalDateTime currentSystemTime = LocalDateTime.now();
            LocalDateTime originalEndTime = auctionConfig.getEndTime();

            long remainingSeconds = Duration.between(currentSystemTime, originalEndTime).getSeconds();
            int extensionThresholdSeconds = auctionConfig.getExtendSecond();

            if (remainingSeconds > 0 && remainingSeconds <= extensionThresholdSeconds) {
                return originalEndTime.plusSeconds(extensionThresholdSeconds);
            }
            return null;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AntiSnipingService.calculateExtendedEndTime: " + exception.getMessage(), exception);
            throw exception;
        }
    }
}
