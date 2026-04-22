package com.ssscloud.auction.server.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private Socket clientSocket;
    private MessageHandler messageHandler;

    // Session của client này — được set sau khi LOGIN thành công
    private String userId;
    private String username;
    
    public ClientHandler(Socket socket, MessageHandler messageHandler){
        this.clientSocket = socket;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8")); //nhận từ client về server
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF-8"), true); //gửi từ server lên Client

            String jsonFromClient;
            while((jsonFromClient = reader.readLine()) != null){
                String jsonResponse = messageHandler.handleMessage(jsonFromClient, this); //truyền this vào để id,username được set sẵn sau khi login
                if (jsonResponse != null && !jsonResponse.isEmpty()) {
                    writer.println(jsonResponse);
                    writer.flush();
                }
            }
        } 
        catch (IOException e) {
            System.out.println("Client đã ngắt kết nối");
        } 
        finally {
            try {
                this.clientSocket.close();
            } 
            catch (IOException e) {
                System.out.println("Lỗi đóng socket: " + e.getMessage());
            }
        }
    }

    //getter setter
    public String getUserId()   { return userId; }
    public String getUsername() { return username; }
    public void setSession(String userId, String username) {
        this.userId   = userId;
        this.username = username;
    }
}

