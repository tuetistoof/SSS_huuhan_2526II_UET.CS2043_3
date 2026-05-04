package com.ssscloud.auction.common.util;

import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;

public final class JsonUtils {

    // Tạo một Gson duy nhất để dùng chung toàn dự án
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()) // Hỗ trợ LocalDateTime
            
            // Sửa lỗi LocalDate
            .registerTypeAdapter(java.time.LocalDate.class, (com.google.gson.JsonSerializer<java.time.LocalDate>) (src, type, ctx) -> new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDate.class, (com.google.gson.JsonDeserializer<java.time.LocalDate>) (json, type, ctx) -> java.time.LocalDate.parse(json.getAsString()))
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

    public static <T> T fromJsonGeneric(String json, Type typeOfT) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, typeOfT);
        } catch (Exception e) {
            System.err.println("Lỗi parse JSON Generic: " + e.getMessage());
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