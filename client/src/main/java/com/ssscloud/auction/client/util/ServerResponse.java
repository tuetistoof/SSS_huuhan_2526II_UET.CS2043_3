package com.ssscloud.auction.client.util;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ssscloud.auction.common.payload.ClientMessage;
import com.ssscloud.auction.common.payload.response.request.ApiResponse;
import com.ssscloud.auction.common.payload.response.request.ListResponse;
import com.ssscloud.auction.common.util.JsonUtils;

/**
 * Helper unwrap chuỗi JSON nhiều lớp từ server về đúng kiểu T.
 *
 * Server luôn bọc: ClientMessage { data: ApiResponse { data: T } } Nhưng T có 3 dạng tùy action:
 *
 *   Dạng 1 — Object đơn: vd ApiResponse { data: AuctionDTO }
 *     Dùng: unwrap(raw, action, AuctionDTO.class)
 *
 *   Dạng 2 — ListResponse wrapper: vd  ApiResponse { data: ListResponse { data: List<AuctionDTO> } }
 *     Dùng: unwrapList(raw, action, AuctionDTO.class)
 *
 *   Dạng 3 — List trực tiếp (không qua ListResponse): ApiResponse { data: List<BidDTO> }
 *     Dùng: unwrapDirectList(raw, action, BidDTO.class)
 */
public final class ServerResponse {

    private ServerResponse() {}

    // Dạng 1: ApiResponse<T> — T là object đơn (DTO, Boolean, Long...)
    public static <T> T unwrap(String raw, String expectedAction, Class<T> dataClass) {
        String innerJson = extractApiResponseData(raw, expectedAction);
        if (innerJson == null) return null;
        return JsonUtils.fromJson(innerJson, dataClass);
    }
    public static <T> T unwrapType(String raw, String expectedAction, Type dataType) {
        String innerJson = extractApiResponseData(raw, expectedAction);
        if (innerJson == null) return null;
        return JsonUtils.fromJsonGeneric(innerJson, dataType);
    }

    // Dạng 2: ApiResponse<ListResponse<T>> — server bọc qua ListResponse
    public static <T> java.util.List<T> unwrapList(String raw, String expectedAction, Class<T> itemClass) {
        String innerJson = extractApiResponseData(raw, expectedAction);
        if (innerJson == null) return null;

        // innerJson lúc này là JSON của ListResponse<T>
        Type listRespType = TypeToken.getParameterized(ListResponse.class, itemClass).getType();
        ListResponse<T> listResp = JsonUtils.fromJsonGeneric(innerJson, listRespType);
        if (listResp == null) return null;
        return listResp.getData();
    }

    // Dạng 3: ApiResponse<List<T>> — server để List trực tiếp, không bọc ListResponse
    public static <T> List<T> unwrapDirectList(String raw, String expectedAction, Class<T> itemClass) {
        String innerJson = extractApiResponseData(raw, expectedAction);
        if (innerJson == null) return null;

        Type listType = TypeToken.getParameterized(java.util.List.class, itemClass).getType();
        return JsonUtils.fromJsonGeneric(innerJson, listType);
    }

    // Tiện ích: isSuccess / errorMessage

    /**
     * Kiểm tra response có success không mà không cần lấy data.
     * Dùng cho các action không trả data: PLACE_BID, TOGGLE_FOLLOW, v.v.
     */
    public static boolean isSuccess(String raw) {
        if (raw == null) return false;
        try {
            ClientMessage msg = JsonUtils.fromJson(raw, ClientMessage.class);
            if (msg == null || msg.getData() == null) return false;
            String innerJson = JsonUtils.toJson(msg.getData());
            // Parse ApiResponse<?> chỉ để đọc field success — data không quan trọng
            Type type = new TypeToken<ApiResponse<Object>>() {}.getType();
            ApiResponse<Object> apiResp = JsonUtils.fromJsonGeneric(innerJson, type);
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
            Type type = new TypeToken<ApiResponse<Object>>() {}.getType();
            ApiResponse<Object> apiResp = JsonUtils.fromJsonGeneric(innerJson, type);
            if (apiResp != null && apiResp.getMessage() != null) return apiResp.getMessage();
        } catch (Exception ignored) {}
        return "Unexpected error";
    }

    // Private — bóc 2 lớp ngoài: ClientMessage → ApiResponse, trả JSON của data

    private static String extractApiResponseData(String raw, String expectedAction) {
        if (raw == null) return null;
        try {
            ClientMessage msg = JsonUtils.fromJson(raw, ClientMessage.class);
            if (msg == null || msg.getData() == null) return null;
            if (expectedAction != null && !expectedAction.equals(msg.getAction())) return null;

            String apiJson = JsonUtils.toJson(msg.getData());
            Type apiType = new TypeToken<ApiResponse<Object>>() {}.getType();
            ApiResponse<Object> apiResp = JsonUtils.fromJsonGeneric(apiJson, apiType);

            if (apiResp == null || !apiResp.isSuccess() || apiResp.getData() == null) return null;

            return JsonUtils.toJson(apiResp.getData());
        } catch (Exception e) {
            return null;
        }
    }
}