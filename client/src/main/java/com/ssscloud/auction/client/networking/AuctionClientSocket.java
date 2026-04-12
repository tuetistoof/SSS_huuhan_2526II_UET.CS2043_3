package com.ssscloud.auction.client.networking;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AuctionClientSocket {
    //Singleton
    private static AuctionClientSocket instance;
    private AuctionClientSocket() {}
 
    public static AuctionClientSocket getInstance() {
        if (instance == null) {
            instance = new AuctionClientSocket();
        }
        return instance;
    }

    private Socket socket;
    private PrintWriter out;      // ghi ra server
    private BufferedReader in;    // đọc từ server
    private boolean connected = false;
    
}
