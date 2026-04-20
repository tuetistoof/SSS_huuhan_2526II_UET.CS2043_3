package com.ssscloud.auction.common.observer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *  Observer cũ (mỗi Subject tự giữ list):
 *    Auction_A.observers = [C1, C2]
 *    Auction_B.observers = [C3]
 *    → Muốn biết "C1 đang xem phiên nào?" → phải duyệt tất cả Auction
 *    → Auction phải biết Observer → coupling cao
 *
 *  ChangeManager mới (1 HashMap tập trung):
 *    map = {
 *      Auction_A → [C1, C2],
 *      Auction_B → [C3]
 *    }
 *    → Auction KHÔNG biết Observer tồn tại → coupling thấp hơn
 *    → Tất cả mapping nằm 1 chỗ 
 * 
 * các method: attach, detach, notify
 */



public class ChangeManager {
    //danh bạ 
    private final Map<Subject, List<Observer>> registry = new ConcurrentHashMap<>();

    //Singleton
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

    public void detach(Subject subject, Observer observer){
        List<Observer> observers = registry.get(subject); //tìm list observer
        if (observers != null) {
            observers.remove(observer); //remove khỏi list
            if (observers.isEmpty()) {
                registry.remove(subject);
            } //nếu list rỗng thig xóa hỏi map
        }
    }

    public void notify(Subject subject){
        List<Observer> observers = registry.get(subject);
        if (observers == null || observers.isEmpty()) return;

        for (Observer o : observers) {
            try {
                o.update(subject); 
            } catch (Exception e) {
                // 1 observer lỗi không được dừng các observer khác
                System.err.println("[ChangeManager] Lỗi notify observer: " + e.getMessage());
            }
        }
    }
}


