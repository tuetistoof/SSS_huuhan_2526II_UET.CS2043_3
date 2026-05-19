
package com.ssscloud.auction.client.networking;

import javafx.application.Platform;
import java.util.function.Consumer;

/**
 * SocketDispatcher — lớp trung gian duy nhất giữa controller và socket.
 *
 * Cơ chế:
 *   1. Controller gọi dispatcher.request(json, onSuccess, onError) trên FX thread.
 *   2. Dispatcher gọi socket.sendAsync() — trả CompletableFuture ngay, không block.
 *   3. Listener Thread nhận response, resolve CF theo requestId.
 *   4. whenComplete() chạy, gọi Platform.runLater(onSuccess) — về FX thread.
 *   5. onSuccess chạy trên FX thread — update UI trực tiếp, không cần sync thêm.
 *
 * Quy tắc dùng:
 *   - Mọi sendAndReceive + new Thread() trong controller → thay bằng dispatcher.request().
 *   - PLACE_BID và UNSUBSCRIBE_AUCTION vẫn dùng socket.send() trực tiếp (fire-and-forget).
 *   - onSuccess và onError luôn chạy trên FX thread — được phép update UI trực tiếp.
 */
public class SocketDispatcher {

    private static final SocketDispatcher INSTANCE = new SocketDispatcher();
    private final AuctionClientSocket socket = AuctionClientSocket.getInstance();

    private SocketDispatcher() {}

    public static SocketDispatcher getInstance() {
        return INSTANCE;
    }

    /**
     * Gửi request, callback về FX thread khi có response.
     *
     * @param json      JSON request đã build sẵn
     * @param onSuccess nhận raw response String — chạy trên FX thread
     * @param onError   gọi khi response null hoặc timeout — chạy trên FX thread
     */
    public void request(String json, Consumer<String> onSuccess, Runnable onError) {
        socket.sendAsync(json).whenComplete((response, ex) ->
            Platform.runLater(() -> {
                if (ex != null || response == null) {
                    onError.run();
                } else {
                    onSuccess.accept(response);
                }
            })
        );
    }

    /**
     * Overload không cần xử lý lỗi riêng — lỗi bị bỏ qua silently.
     * Dùng khi action không cần báo lỗi cho user (vd: load danh sách phụ).
     */
    public void request(String json, Consumer<String> onSuccess) {
        request(json, onSuccess, () -> {});
    }
}