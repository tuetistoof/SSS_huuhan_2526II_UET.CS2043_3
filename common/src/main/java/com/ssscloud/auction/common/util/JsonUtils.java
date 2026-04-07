package com.ssscloud.auction.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;


public final class JsonUtils {

    // Tạo một Gson duy nhất để dùng chung toàn dự án
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()) // Hỗ trợ LocalDateTime
            .create();
    private JsonUtils() {}

    public static String toJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, classOfT);
        } catch (Exception e) {
            System.err.println("Lỗi parse JSON: " + e.getMessage());
            return null;
        }
    }

    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        return gson.toJson(obj);
    }
}