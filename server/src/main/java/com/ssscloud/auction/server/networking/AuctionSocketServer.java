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
import com.ssscloud.auction.server.service.AntiSnipingService;
import com.ssscloud.auction.server.service.AuctionService;
import com.ssscloud.auction.server.service.AutoBidService;
import com.ssscloud.auction.server.service.BidService;

public class AuctionSocketServer {
    // khong dung thi tam thoi dong vao cho do an canh bao
    // private static final int maxThread = 1000;
    private static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();

        // AutoBidService autoBidService = new AutoBidService();
        AuctionService auctionService = new AuctionService(auctionDAO);

        BidService bidService = new BidService(auctionDAO, bidTransactionDAO);
        UserController userCtrl = new UserController(userDAO);

        AuctionController auctionCtrl = new AuctionController(auctionService);
        BidController bidCtrl = new BidController(bidService);
        MessageHandler messageHandler = new MessageHandler(userCtrl, auctionCtrl, bidCtrl);

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