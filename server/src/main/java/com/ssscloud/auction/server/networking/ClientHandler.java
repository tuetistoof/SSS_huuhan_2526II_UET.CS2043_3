package com.ssscloud.auction.server.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.server.util.AuctionRegistry;
import com.ssscloud.auction.server.util.SessionRegistry;

/**
 * ClientHandler manages the lifecycle of an individual network client connection.
 * It listens for incoming JSON payloads and delegates processing logic to the MessageHandler.
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName()); // Logging Standards: First attribute

    private final Socket clientSocket;
    private final MessageHandler messageHandler;
    private String userId;
    private String username;
    private PrintWriter writer;

    public ClientHandler(Socket socket, MessageHandler messageHandler) {
        this.clientSocket = socket;
        this.messageHandler = messageHandler;
    }

    // --- PUBLIC METHODS ---

    @Override
    public void run() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8")); 
            this.writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF-8"), true); 

            String jsonPayload;
            while((jsonPayload = bufferedReader.readLine()) != null) {
                String jsonResponse = messageHandler.handleMessage(jsonPayload, this); 
                if (jsonResponse != null && !jsonResponse.isEmpty()) {
                    writer.println(jsonResponse);
                    writer.flush();
                }
            }
        } catch (IOException ioException) { 
            logger.log(Level.INFO, "Client connection terminated for userId: " + userId + ". Reason: " + ioException.getMessage());
        } finally {
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

    public void setSession(String userId, String username) {
        this.userId = userId;
        this.username = username;
        SessionRegistry.getInstance().register(userId, this.writer);
    }
    private void cleanupObservers() {
        try {
            for (Auction auction : AuctionRegistry.getInstance().getAllLive()) {
                ChangeManager.getInstance().detachByClientId(auction, userId);
            }
            logger.log(Level.INFO, "Cleaned up observers for disconnected userId: " + userId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during observer cleanup for userId: " + userId, e);
        }
    }
}
