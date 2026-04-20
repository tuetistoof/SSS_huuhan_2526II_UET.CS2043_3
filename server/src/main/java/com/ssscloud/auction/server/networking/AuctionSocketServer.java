package com.ssscloud.auction.server.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.UserController;

public class AuctionSocketServer{
    // khong dung thi tam thoi dong vao cho do an canh bao 
    // private static final int maxThread = 1000;
    private static ExecutorService pool = Executors.newCachedThreadPool();

    
    public static void main(String[] args) {
        UserController userCtrl = new UserController();
        AuctionController auctionCtrl = new AuctionController();
        BidController bidCtrl = new BidController();
        MessageHandler messageHandler = new MessageHandler(userCtrl, auctionCtrl, bidCtrl);
    

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            while(true){
                Socket clientSocket = serverSocket.accept();
    
        
                pool.execute(new ClientHandler(clientSocket, messageHandler));
            }
        } 
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
}
