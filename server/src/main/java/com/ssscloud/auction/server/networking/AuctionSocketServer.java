package com.ssscloud.auction.server.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.UserController;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.service.AuctionService;
import com.ssscloud.auction.server.service.AutoBidService;
import com.ssscloud.auction.server.service.BidService;
import com.ssscloud.auction.server.service.ConcurrentBidManager;

public class AuctionSocketServer {
    // khong dung thi tam thoi dong vao cho do an canh bao
    // private static final int maxThread = 1000;
    private static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();

        AutoBidService autoBidService = new AutoBidService(auctionDAO, userDAO);
        BidService bidService = new BidService(auctionDAO, userDAO);
        
        UserController userCtrl = new UserController(userDAO);

        AuctionService auctionService = new AuctionService(auctionDAO);
        AuctionController auctionCtrl = new AuctionController(auctionService);
        
        BidController bidCtrl = new BidController(bidService, autoBidService, bidTransactionDAO);
        ConcurrentBidManager.initialize(bidTransactionDAO, autoBidService);
        MessageHandler messageHandler = new MessageHandler(auctionDAO, userCtrl, auctionCtrl, bidCtrl);

        System.out.println("[Server] Khởi động port 5000...");

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();

                pool.execute(new ClientHandler(clientSocket, messageHandler));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}