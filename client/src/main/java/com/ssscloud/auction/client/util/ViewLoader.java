package com.ssscloud.auction.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class ViewLoader {

    private ViewLoader() {}

    public record LoadResult<T>(Parent root, T controller) {}

    public static <T> LoadResult<T> load(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ViewLoader.class.getResource("/fxml/" + fxmlName));
            Parent root = loader.load();
            return new LoadResult<>(root, loader.getController());
        } catch (IOException e) {
            throw new RuntimeException("Cannot load FXML: " + fxmlName, e);
        }
    }
}