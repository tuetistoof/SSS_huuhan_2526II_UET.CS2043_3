package com.ssscloud.auction.client.controller.shared;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.scene.shape.Arc;
import javafx.util.Duration;

public class LoadingController {
    @FXML
    private Arc loadingSpinner;

    private RotateTransition rotateTransition;

    @FXML
    public void initialize() {
        rotateTransition = new RotateTransition(Duration.millis(800), loadingSpinner);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
    }
    
    public void playAnimation() {
        if (rotateTransition != null) {
            rotateTransition.play();
        }
    }

    public void stopAnimation() {
        if (rotateTransition != null) {
            rotateTransition.stop();
        }
    }
}
