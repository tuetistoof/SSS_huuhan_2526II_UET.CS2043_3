package com.ssscloud.auction.client.networking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssscloud.auction.common.dto.ClientMessage;

/**
 * send: chỉ gửi mà không chờ response(dùng cho cơ chế push: vd bidding room controller)
 * sendAndReceive: gửi và chờ response (dùng cho cơ chế req-res: vd login controller)
 * 
 * 
 * listener: cơ chế khá giống observer, nhưng mà subject là mỗi lần server push json về, listener đăng kí và nghe
 * start listener thread: các listener đăng kí sẽ chạy thread ngầm 
 *
 * Thread safety: sử dụng ReentrantLock để đảm bảo chỉ có 1 sendAndReceive tại 1 thời điểm, tránh trường hợp response bị trộn lẫn khi có nhiều req-res cùng lúc
 */
public class AuctionClientSocket {
    private static final Logger logger = Logger.getLogger(AuctionClientSocket.class.getName());
    private static AuctionClientSocket instance;
    private AuctionClientSocket() {}

 
    public static AuctionClientSocket getInstance() {
        if (instance == null) {
            synchronized (AuctionClientSocket.class) {
                if (instance == null) {
                    instance = new AuctionClientSocket();
                }
            }
        }
        return instance;
    }

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>(); // Thread-safe list for listeners
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();// Queue to hold responses for sendAndReceive calls
    private final ReentrantLock reqLock = new ReentrantLock(); //chỉ có 1 sendAndReceive tại 1 thời điểm

    private void startListenerThread() {  
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null){
                    JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                    String type = obj.has("type") ? obj.get("type").getAsString() : ClientMessage.TYPE_RESPONSE;
 
                    if (ClientMessage.TYPE_PUSH.equalsIgnoreCase(type)) {
                        // Push thì gửi cho listener xử lí
                        for (MessageListener l : listeners) {
                            l.onMessageReceived(line);
                        }
                    } else {
                        // Response thì bỏ vào queue
                        responseQueue.put(line);
                    }
                  
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Connection lost with server: " + e.getMessage());
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

    public String sendAndReceive(String json) {
        if (!connected || out == null) return null;
        reqLock.lock(); //đảm bảo chỉ 1 sendAndReceive tại 1 thời điểm
        try {
            responseQueue.clear();
            send(json);
            return responseQueue.poll(5, TimeUnit.SECONDS); //chờ tối đa 5s để lấy response, tránh trường hợp server không phản hồi
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted while waiting for server response");
            Thread.currentThread().interrupt(); 
            return null;
        } finally {
            reqLock.unlock();
        }
    }

    public void addListener(MessageListener listener){
        listeners.add(listener);
    }
    public void removeListener(MessageListener listener){
        listeners.remove(listener);
    }
    public boolean isConnected() { return connected; }


}