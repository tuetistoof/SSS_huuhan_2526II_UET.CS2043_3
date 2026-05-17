package com.ssscloud.auction.server.util;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionRegistry — lưu map userId → PrintWriter cho tất cả client đang kết nối
 * register ngay khi client login
 * unregister ngay khi client logout hoặc ngắt kết nối
 */
public class SessionRegistry {
    private static final SessionRegistry instance = new SessionRegistry();
    private SessionRegistry(){}
    public static SessionRegistry getInstance(){
        return instance;
    }

    private final Map<String, PrintWriter> sessions = new ConcurrentHashMap<>();

    public void register(String userId, PrintWriter writer) {
        sessions.put(userId, writer);
    }

    public void unregister(String userId) {
        sessions.remove(userId);
    }

    public PrintWriter getWriter(String userId) {
        return sessions.get(userId);
    }
 
    public boolean isOnline(String userId) {
        return sessions.containsKey(userId);
    }

    public Map<String, PrintWriter> getAllWriters() {
        return sessions;
    }
}