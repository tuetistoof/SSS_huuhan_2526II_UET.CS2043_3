package com.ssscloud.auction.client.networking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * send: chỉ gửi mà không chờ response(dùng cho cơ chế push: vd bidding room controller)
 * sendAndReceive: gửi và chờ response (dùng cho cơ chế req-res: vd login controller)
 * 
 * 
 * listener: cơ chế khá giống observer, nhưng mà subject là mỗi lần server push json về, listener đăng kí và nghe
 * start listener thread: các listener đăng kí sẽ chạy thread ngầm 
 * 
 * nhiều controller đăng kí listener nhưng khi có tin nhắn không phải tất cả đều xử lí mà tin nhắn mà switch-case ở mỗi controller
 */
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
    private final List<MessageListener> listeners = new ArrayList<>();

    private void startListenerThread() {   //các listener đọc push từ server
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null){
                    String json = line;
                    if (pendingResponse != null && !pendingResponse.isDone()) {
                        pendingResponse.complete(json);
                    }
                    else {
                        for (MessageListener l : listeners) {
                            l.onMessageReceived(json);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Mất kết nối với Server: " + e.getMessage());
            }
        });
        t.setDaemon(true);  //thread tự tắt khi app đóng
        t.start();
    }
    
    public void connect(String host, int port) throws Exception{
        socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        connected = true;
        startListenerThread();
     }

    public void send(String json){
        if (connected && out != null){
            out.println(json); //gửi json qua socket
        }
    }

    // 1 cái Future để chứa phản hồi đang chờ xử lí
    private CompletableFuture<String> pendingResponse;

    public String sendAndReceive(String json) {
        try {
            if (connected && out != null){
                pendingResponse = new CompletableFuture<>();
                send(json);
                return pendingResponse.get(); // chờ luồng chạy xong lấy dữ liệu r mới trả về
            }
        }
        catch (InterruptedException | ExecutionException e) {
            System.out.println("Luồng bị gián đoạn: " + e.getMessage());
        }
        return null;
    }

    public void addListener(MessageListener listener){
        listeners.add(listener);
    }
    public void removeListener(MessageListener listener){
        listeners.remove(listener);
     }


}
