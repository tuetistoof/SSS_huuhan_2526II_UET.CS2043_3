package com.ssscloud.auction.client.util;

import java.lang.reflect.Type;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.dto.response.ApiResponse;
import com.ssscloud.auction.common.util.JsonUtils;

/**
 * helper unwrap chuỗi parse lặp đi lặp lại trong mọi controller.
 * Server luôn trả về: ClientMessage { action, data: ApiResponse { success, data: T } }
 *
 * Usage:
 *   socket.sendAsync(json)
 *         .thenAccept(raw -> {
 *             MyDTO result = ServerResponse.unwrap(raw, "MY_ACTION_RESPONSE", MyDTO.class);
 *             Platform.runLater(() -> render(result)); // null nếu lỗi
 *         });
 */
public final class ServerResponse {

    private ServerResponse() {}

    /**
     * Unwrap response cho kiểu đơn giản (không generic).
     * return data đã parse, hoặc null nếu bất kỳ bước nào thất bại.
     */
    public static <T> T unwrap(String raw, String expectedAction, Class<T> dataClass) {
        if (raw == null) return null;
        try {
            ClientMessage msg = JsonUtils.fromJson(raw, ClientMessage.class);
            if (msg == null || msg.getData() == null) return null;
            if (expectedAction != null && !expectedAction.equals(msg.getAction())) return null;

            String innerJson = JsonUtils.toJson(msg.getData());
            ApiResponse<?> apiResp = JsonUtils.fromJson(innerJson, ApiResponse.class);
            if (apiResp == null || !apiResp.isSuccess() || apiResp.getData() == null) return null;

            String dataJson = JsonUtils.toJson(apiResp.getData());
            return JsonUtils.fromJson(dataJson, dataClass);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unwrap response cho kiểu generic (List, ListResponse...).
     * Dùng TypeToken để truyền type.
     * return data đã parse, hoặc null nếu bất kỳ bước nào thất bại.
     */
    public static <T> T unwrapGeneric(String raw, String expectedAction, Type type) {
        if (raw == null) return null;
        try {
            ClientMessage msg = JsonUtils.fromJson(raw, ClientMessage.class);
            if (msg == null || msg.getData() == null) return null;
            if (expectedAction != null && !expectedAction.equals(msg.getAction())) return null;

            String innerJson = JsonUtils.toJson(msg.getData());
            ApiResponse<?> apiResp = JsonUtils.fromJson(innerJson, ApiResponse.class);
            if (apiResp == null || !apiResp.isSuccess() || apiResp.getData() == null) return null;

            String dataJson = JsonUtils.toJson(apiResp.getData());
            return JsonUtils.fromJsonGeneric(dataJson, type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Kiểm tra response có success không (không cần lấy data).
     */
    public static boolean isSuccess(String raw, String expectedAction) {
        if (raw == null) return false;
        try {
            ClientMessage msg = JsonUtils.fromJson(raw, ClientMessage.class);
            if (msg == null || msg.getData() == null) return false;
            if (expectedAction != null && !expectedAction.equals(msg.getAction())) return false;

            String innerJson = JsonUtils.toJson(msg.getData());
            ApiResponse<?> apiResp = JsonUtils.fromJson(innerJson, ApiResponse.class);
            return apiResp != null && apiResp.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy error message từ response thất bại.
     */
    public static String errorMessage(String raw) {
        if (raw == null) return "No response from server";
        try {
            ClientMessage msg = JsonUtils.fromJson(raw, ClientMessage.class);
            if (msg == null || msg.getData() == null) return "Invalid response";

            String innerJson = JsonUtils.toJson(msg.getData());
            ApiResponse<?> apiResp = JsonUtils.fromJson(innerJson, ApiResponse.class);
            if (apiResp != null && apiResp.getMessage() != null) return apiResp.getMessage();
        } catch (Exception ignored) {}
        return "Unexpected error";
    }
}
