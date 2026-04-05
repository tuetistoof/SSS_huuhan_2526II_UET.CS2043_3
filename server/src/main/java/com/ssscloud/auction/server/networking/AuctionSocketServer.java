package com.ssscloud.auction.server.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionSocketServer{
    private static final int maxThread = 1000;
    private static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            while(true){
                Socket clientSocket = serverSocket.accept();

                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
}
