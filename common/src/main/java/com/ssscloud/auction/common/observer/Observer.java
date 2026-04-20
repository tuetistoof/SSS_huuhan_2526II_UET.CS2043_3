package com.ssscloud.auction.common.observer;

/**
 * interface người theo dõi
 * trong bài ClientObserver implements Observer
 * 1 observer thông thường có method update()
 */

public interface Observer {
    /**
     *subject object vừa thay đổi — cast sang Auction để đọc data:
     *                Auction auction = (Auction) subject;
     *                double price = auction.getCurrentPrice();
     */
    public void update(Subject subject);
}
