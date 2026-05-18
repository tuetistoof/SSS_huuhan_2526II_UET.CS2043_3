package com.ssscloud.auction.server.util;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.ClientMessage;
import com.ssscloud.auction.common.util.JsonUtils;

/**
 * SessionRegistry — quản lý session của tất cả client đang kết nối.
 * Mỗi session lưu PrintWriter (socket) và unsettledBalance (lock/pending).
 * register() khi login, unregister() khi logout hoặc disconnect.
 */
public class SessionRegistry {
    private static final Logger logger = Logger.getLogger(SessionRegistry.class.getName());
    private static final SessionRegistry instance = new SessionRegistry();

    private SessionRegistry() {}

    public static SessionRegistry getInstance() {
        return instance;
    }

    // --- INNER CLASS ---

    private static class UserSession {
        private final PrintWriter writer;
        private long unsettledBalance;

        UserSession(PrintWriter writer, long unsettledBalance) {
            this.writer = writer;
            this.unsettledBalance = unsettledBalance;
        }
    }

    // --- STATE ---

    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    // --- PUBLIC METHODS ---

    public void register(String userId, PrintWriter writer, long unsettledBalance) {
        sessions.put(userId, new UserSession(writer, unsettledBalance));
        logger.log(Level.INFO, "Session registered for userId: {0}", userId);
    }

    public void unregister(String userId) {
        sessions.remove(userId);
        logger.log(Level.INFO, "Session unregistered for userId: {0}", userId);
    }

    public boolean isOnline(String userId) {
        return sessions.containsKey(userId);
    }

    public PrintWriter getWriter(String userId) {
        UserSession session = sessions.get(userId);
        return session != null ? session.writer : null;
    }

    public long getUnsettledBalance(String userId) {
        UserSession session = sessions.get(userId);
        return session != null ? session.unsettledBalance : 0L;
    }

    public void setUnsettledBalance(String userId, long amount) {
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.unsettledBalance = amount;
        } else {
            logger.log(Level.WARNING, "setUnsettledBalance: no session found for userId: {0}", userId);
        }
    }

    public void addUnsettledBalance(String userId, long delta) {
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.unsettledBalance += delta;
            notifyUnsettledBalanceUpdate(userId, session.unsettledBalance);
        } else {
            logger.log(Level.WARNING, "addUnsettledBalance: no session found for userId: {0}", userId);
        }
    }

    private void notifyUnsettledBalanceUpdate(String userId, long unsettledBalance) {
        PrintWriter writer = instance.getWriter(userId);
        if (writer == null) return;

        try {
            synchronized (writer) {
                writer.println(JsonUtils.toJson(ClientMessage.push("UNSETTLED_UPDATE", unsettledBalance)));
                writer.flush();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to notify balance update for userId: " + userId, e);
        }
    }

    public Map<String, PrintWriter> getAllWriters() {
        Map<String, PrintWriter> writers = new ConcurrentHashMap<>();
        sessions.forEach((userId, session) -> writers.put(userId, session.writer));
        return writers;
    }
}