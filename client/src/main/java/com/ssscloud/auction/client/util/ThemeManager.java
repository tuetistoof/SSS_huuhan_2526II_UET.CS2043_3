package com.ssscloud.auction.client.util;

import java.net.URL;
import java.util.prefs.Preferences;

import javafx.scene.Parent;
import javafx.scene.Scene;

public final class ThemeManager {

    public enum Theme { LIGHT, DARK }

    private static final String DARK_THEME_CLASS = "theme-dark";
    private static final String DARK_TOKENS_PATH = "/css/tokens-dark.css";

    // Preferences key — lưu vào OS keychain / registry của máy người dùng
    private static final Preferences PREFS =
            Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_KEY = "appTheme";
    private static final String DEFAULT_THEME = Theme.LIGHT.name();

    private ThemeManager() {}

    // ── Lưu & load preference ──────────────────────────────────────────────

    public static Theme getSavedTheme() {
        String saved = PREFS.get(PREF_KEY, DEFAULT_THEME);
        try {
            return Theme.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return Theme.DARK;
        }
    }
    
    public static void saveTheme(Theme theme) {
        PREFS.put(PREF_KEY, theme.name());
    }

    // ── Apply ──────────────────────────────────────────────────────────────

    public static void apply(Scene scene, Theme theme) {
        if (scene == null || scene.getRoot() == null) return;
        ensureDarkTokensStylesheet(scene);
        apply(scene.getRoot(), theme);
        saveTheme(theme);
    }

    public static void apply(Parent root, Theme theme) {
        if (root == null) return;
        if (theme == Theme.DARK) {
            if (!root.getStyleClass().contains(DARK_THEME_CLASS))
                root.getStyleClass().add(DARK_THEME_CLASS);
        } else {
            root.getStyleClass().remove(DARK_THEME_CLASS);
        }
    }

    // ── Toggle ─────────────────────────────────────────────────────────────

    public static void toggle(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        Theme next = isDark(scene) ? Theme.LIGHT : Theme.DARK;
        apply(scene, next);
    }

    // ── Query ──────────────────────────────────────────────────────────────

    public static boolean isDark(Scene scene) {
        return scene != null
                && scene.getRoot() != null
                && scene.getRoot().getStyleClass().contains(DARK_THEME_CLASS);
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private static void ensureDarkTokensStylesheet(Scene scene) {
        URL url = ThemeManager.class.getResource(DARK_TOKENS_PATH);
        if (url == null)
            throw new IllegalStateException("Missing stylesheet: " + DARK_TOKENS_PATH);
        String stylesheet = url.toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet))
            scene.getStylesheets().add(stylesheet);
    }
}