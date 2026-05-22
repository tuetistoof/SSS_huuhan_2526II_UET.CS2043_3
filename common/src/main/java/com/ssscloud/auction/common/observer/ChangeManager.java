package com.ssscloud.auction.common.observer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.model.auction.Auction;
/**
 * ChangeManager holds the registry of Subjects and their Observers, and provides methods to attach/detach observers and notify them of changes.
 * It is implemented as a thread-safe singleton to ensure consistent state across the application.
 */

public class ChangeManager { 
    private final Map<Subject, List<Observer>> registry = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(ChangeManager.class.getName());

    private static volatile ChangeManager instance = null;
    private ChangeManager(){}
    public static ChangeManager getInstance(){
        if (instance == null){
            synchronized (ChangeManager.class) {
                if (instance == null) {
                    instance = new ChangeManager();
                }
            }
        }
        return instance;
    }

    public void attach(Subject subject, Observer observer){
        registry.computeIfAbsent(subject, k -> new CopyOnWriteArrayList<>()).add(observer); // kiểm tra có list observer chưa không thì tạo thêm
    }

    public void detachByAdmin(Subject subject){ // admin
        registry.remove(subject);
    }


    // ChangeManager
    public List<Observer> detachByClientId(Subject subject, String clientId) {
        List<Observer> observers = registry.get(subject);
        if (observers == null || clientId == null) return List.of();
        
        List<Observer> removed = observers.stream()
                .filter(o -> clientId.equals(o.getObserverId()))
                .toList();
        observers.removeAll(removed);
        if (observers.isEmpty()) registry.remove(subject);
        return removed; // ← trả về để caller gọi shutdown
    }

    public void notify(Subject subject){
        List<Observer> observers = registry.get(subject);
        if (observers == null || observers.isEmpty()) return;

        for (Observer o : observers) {
            try {
                o.update(subject); 
            } catch (Exception e) {
                // 1 observer lỗi không được dừng các observer khác
                logger.log(Level.SEVERE, "[ChangeManager] Error notifying observer: " + e.getMessage(), e);
            }
        }
    }
    public boolean hasObserver(Subject subject, String observerId) {
        List<Observer> observers = registry.get(subject);
        if (observers == null) return false;
        return observers.stream().anyMatch(o -> observerId.equals(o.getObserverId()));
    }
    public int observerCount(Auction auction) {
        List<Observer> observers = registry.get(auction);
        return observers == null ? 0 : observers.size();
    }

}