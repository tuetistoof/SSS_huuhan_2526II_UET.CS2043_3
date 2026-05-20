package com.ssscloud.auction.client.util;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;

import java.time.Duration;
import java.time.LocalDateTime;

public class AuctionCountdownTimer {

    private final Label lblTimer;
    private Timeline timeline;
    private LocalDateTime endTime;

    public AuctionCountdownTimer(Label lblTimer) {
        this.lblTimer = lblTimer;
    }

    public void start(LocalDateTime endTime) {
        this.endTime = endTime;
        stop();
        if (endTime == null || lblTimer == null) return;

        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /** Cập nhật endTime khi Anti-Sniping gia hạn — timer tự điều chỉnh tick tiếp theo. */
    public void extendTo(LocalDateTime newEndTime) {
        this.endTime = newEndTime;
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }


    private void tick() {
        if (endTime == null) return;
        Duration remaining = Duration.between(LocalDateTime.now(), endTime);
        if (remaining.isNegative() || remaining.isZero()) {
            if (lblTimer != null) lblTimer.setText("Time remaining: 00:00:00");
            stop();
            return;
        }
        long h = remaining.toHours();
        long m = remaining.toMinutesPart();
        long s = remaining.toSecondsPart();
        if (lblTimer != null) {
            lblTimer.setText(String.format("Time remaining: %02d:%02d:%02d", h, m, s));
        }
    }
}