package com.ssscloud.auction.server.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.server.util.AuctionRegistry;
import com.ssscloud.auction.server.util.SessionRegistry;

/**
 * ClientHandler manages the lifecycle of an individual network client connection.
 * It reads socket messages sequentially, then dispatches request handling to a
 * per-client executor. Responses carry the same requestId as the request, so the
 * client can receive out-of-order responses safely.
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final Socket clientSocket;
    private final MessageHandler messageHandler;
    private final ExecutorService requestExecutor;
    private volatile String userId;
    private volatile String username;
    private PrintWriter writer;

    public ClientHandler(Socket socket, MessageHandler messageHandler) {
        this.clientSocket = socket;
        this.messageHandler = messageHandler;
        this.requestExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("client-request-" + socket.getPort());
            return thread;
        });
    }

    @Override
    public void run() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            this.writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);

            String jsonPayload;
            while ((jsonPayload = bufferedReader.readLine()) != null) {
                final String requestPayload = jsonPayload;
                requestExecutor.submit(() -> processRequest(requestPayload));
            }
        } catch (IOException ioException) {
            logger.log(Level.INFO, "Client connection terminated for userId: " + userId + ". Reason: " + ioException.getMessage());
        } finally {
            requestExecutor.shutdownNow();
            if (userId != null) {
                cleanupObservers();
                SessionRegistry.getInstance().unregister(userId);
            }
            try {
                this.clientSocket.close();
            } catch (IOException ioException) {
                logger.log(Level.WARNING, "Failure occurred while closing the client socket for userId: " + userId, ioException);
            }
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public void setSession(String userId, String username, long unsettledBalance) {
        this.userId = userId;
        this.username = username;
        SessionRegistry.getInstance().register(userId, this.writer, unsettledBalance);
    }

    public void write(String jsonMessage) {
        if (writer == null || jsonMessage == null || jsonMessage.isBlank()) {
            return;
        }
        synchronized (writer) {
            writer.println(jsonMessage);
            writer.flush();
        }
    }

    private void processRequest(String jsonPayload) {
        try {
            String jsonResponse = messageHandler.handleMessage(jsonPayload, this);
            if (jsonResponse != null && !jsonResponse.isEmpty()) {
                write(attachRequestId(jsonPayload, jsonResponse));
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to process client request.", exception);
        }
    }

    private String attachRequestId(String jsonPayload, String jsonResponse) {
        String requestId = extractRequestId(jsonPayload);
        if (requestId == null || requestId.isBlank()) {
            return jsonResponse;
        }

        try {
            JsonObject responseObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            responseObject.addProperty("requestId", requestId);
            return responseObject.toString();
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Unable to attach requestId to response.", exception);
            return jsonResponse;
        }
    }

    private String extractRequestId(String jsonPayload) {
        try {
            JsonObject requestObject = JsonParser.parseString(jsonPayload).getAsJsonObject();
            return requestObject.has("requestId") && !requestObject.get("requestId").isJsonNull()
                    ? requestObject.get("requestId").getAsString()
                    : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private void cleanupObservers() {
        try {
            for (Auction auction : AuctionRegistry.getInstance().getAllLive()) {
                ChangeManager.getInstance().detachByClientId(auction, userId)
                        .forEach(o -> {
                            if (o instanceof ClientObserver co) co.shutdown();
                        });
            }
            logger.log(Level.INFO, "Cleaned up observers for disconnected userId: " + userId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during observer cleanup for userId: " + userId, e);
        }
    }
}