package com.ssscloud.auction.client.util;

import java.net.URL;

import javafx.scene.Parent;
import javafx.scene.Scene;

public final class ThemeManager {

    public enum Theme {
        LIGHT,
        DARK
    }

    private static final String DARK_THEME_CLASS = "theme-dark";
    private static final String DARK_TOKENS_PATH = "/css/tokens-dark.css";

    private ThemeManager() {
    }

    public static void apply(Scene scene, Theme theme) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        ensureDarkTokensStylesheet(scene);
        apply(scene.getRoot(), theme);
    }

    public static void apply(Parent root, Theme theme) {
        if (root == null) {
            return;
        }

        if (theme == Theme.DARK) {
            if (!root.getStyleClass().contains(DARK_THEME_CLASS)) {
                root.getStyleClass().add(DARK_THEME_CLASS);
            }
        } else {
            root.getStyleClass().remove(DARK_THEME_CLASS);
        }
    }

    public static void toggle(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        Theme nextTheme = isDark(scene) ? Theme.LIGHT : Theme.DARK;
        apply(scene, nextTheme);
    }

    public static boolean isDark(Scene scene) {
        return scene != null
                && scene.getRoot() != null
                && scene.getRoot().getStyleClass().contains(DARK_THEME_CLASS);
    }

    private static void ensureDarkTokensStylesheet(Scene scene) {
        URL url = ThemeManager.class.getResource(DARK_TOKENS_PATH);
        if (url == null) {
            throw new IllegalStateException("Missing stylesheet: " + DARK_TOKENS_PATH);
        }

        String stylesheet = url.toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }
}