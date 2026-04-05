package com.ssscloud.auction.server.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private Socket clientSocket;

    public ClientHandler(Socket socket){
        this.clientSocket = socket;
    }
    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF-8"), true);

            MessageHandler messageHandler = new MessageHandler();

            String jsonFromClient;
            while((jsonFromClient = reader.readLine()) != null){
                String jsonReponse = messageHandler.proccessRequest(jsonFromClient);
                writer.print(jsonReponse);
            }
        } catch (IOException e) {
            System.out.println("Client đã ngắt kết nối");
        } finally{
            this.clientSocket.close();
        }

    }
}
