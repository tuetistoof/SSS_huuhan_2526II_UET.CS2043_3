package com.ssscloud.auction.common.observer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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


    public void detachByClientId(Subject subject, String clientId) {
        List<Observer> observers = registry.get(subject);
        if (observers == null || clientId == null) return;
        observers.removeIf(o -> clientId.equals(o.getObserverId()));
        if (observers.isEmpty()) registry.remove(subject);
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

}