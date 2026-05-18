package com.ssscloud.auction.client.networking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.common.dto.ClientMessage;

/**
 * send: chi gui ma khong cho response (dung cho push/fire-and-forget).
 * sendAsync/sendAndReceive: gui request-response voi requestId rieng de nhan dung response.
 *
 * Thread safety:
 * - Nhieu controller co the goi sendAsync/sendAndReceive song song.
 * - Listener thread resolve response vao dung CompletableFuture theo requestId.
 * - Writer duoc dong bo de tranh xen dong JSON khi nhieu thread cung gui.
 */
public class AuctionClientSocket {
    private static final Logger logger = Logger.getLogger(AuctionClientSocket.class.getName());
    private static AuctionClientSocket instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean connected = false;

    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    private final Object writeLock = new Object();

    private AuctionClientSocket() {}

    public static AuctionClientSocket getInstance() {
        if (instance == null) {
            synchronized (AuctionClientSocket.class) {
                if (instance == null) {
                    instance = new AuctionClientSocket();
                }
            }
        }
        return instance;
    }

    private void startListenerThread() {
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                    String type = getString(obj, "type");
                    if (type == null) {
                        type = ClientMessage.TYPE_RESPONSE;
                    }

                    if (ClientMessage.TYPE_PUSH.equalsIgnoreCase(type)) {
                        for (MessageListener listener : listeners) {
                            listener.onMessageReceived(line);
                        }
                    } else {
                        completePendingRequest(obj, line);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Connection lost with server: " + e.getMessage());
            } finally {
                connected = false;
                completeAllPendingExceptionally();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void connect(String host, int port) throws Exception {
        socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        connected = true;
        startListenerThread();
    }

    public void send(String json) {
        if (connected && out != null) {
            synchronized (writeLock) {
                out.println(json);
            }
        }
    }

    public CompletableFuture<String> sendAsync(String json) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (!connected || out == null) {
            future.complete(null);
            return future;
        }

        String requestId = UUID.randomUUID().toString();
        try {
            JsonObject requestObject = JsonParser.parseString(json).getAsJsonObject();
            requestObject.addProperty("requestId", requestId);

            pendingRequests.put(requestId, future);
            future.orTimeout(5, TimeUnit.SECONDS)
                    .whenComplete((response, error) -> pendingRequests.remove(requestId));

            send(requestObject.toString());
        } catch (Exception e) {
            pendingRequests.remove(requestId);
            future.completeExceptionally(e);
        }
        return future;
    }

    public String sendAndReceive(String json) {
        try {
            return sendAsync(json).get(6, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted while waiting for server response");
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            logger.log(Level.WARNING, "Server response failed: " + e.getMessage());
            return null;
        } catch (TimeoutException e) {
            logger.log(Level.WARNING, "Timed out while waiting for server response");
            return null;
        }
    }

    public void addListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return connected;
    }

    private void completePendingRequest(JsonObject obj, String line) {
        String requestId = getString(obj, "requestId");
        CompletableFuture<String> future = requestId != null ? pendingRequests.remove(requestId) : null;

        if (future != null) {
            future.complete(line);
        } else {
            logger.log(Level.WARNING, "Received response without a matching requestId: " + line);
        }
    }

    private void completeAllPendingExceptionally() {
        pendingRequests.forEach((requestId, future) ->
                future.completeExceptionally(new IllegalStateException("Socket connection closed")));
        pendingRequests.clear();
    }

    private String getString(JsonObject obj, String memberName) {
        return obj.has(memberName) && !obj.get(memberName).isJsonNull()
                ? obj.get(memberName).getAsString()
                : null;
    }
}