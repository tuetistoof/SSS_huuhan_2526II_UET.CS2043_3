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
    private static final Logger logger = Logger.getLogger(AntiSnipingService.class.getName());

    // Private constructor to prevent instantiation of this utility class.
    private AntiSnipingService() {
    }

    public static LocalDateTime processAntiSniping(AuctionConfig auctionConfig) {
        if (auctionConfig == null || auctionConfig.getEndTime() == null) {
            logger.log(Level.FINE, "Anti-sniping processing skipped: target auction configuration or conclusion time is null.");
            return null;
        } 

        LocalDateTime currentSystemTime = LocalDateTime.now();
        LocalDateTime originalEndTime = auctionConfig.getEndTime();

        long remainingSeconds = Duration.between(currentSystemTime, originalEndTime).getSeconds();
        int extensionThresholdSeconds = auctionConfig.getExtendSecond();

        if (remainingSeconds > 0 && remainingSeconds <= extensionThresholdSeconds) {
            LocalDateTime updatedEndTime = originalEndTime.plusSeconds(extensionThresholdSeconds);
            auctionConfig.setEndTime(updatedEndTime);
            logger.log(Level.INFO, "Anti-sniping triggered for auction ''{0}''. Duration extended by {1}s. Updated conclusion time: {2}", 
                new Object[]{auctionConfig.getName(), extensionThresholdSeconds, updatedEndTime});
            return updatedEndTime;
        }
        return null;
    }
}
