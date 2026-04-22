package com.ssscloud.auction.common.model.base;

import java.time.LocalDateTime;

public class AuctionConfig extends Entity{
    private long minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private  int extendSecond;
    public AuctionConfig (){};
    // tao moi
    public AuctionConfig (String name, long minIncrement, LocalDateTime startTime, LocalDateTime endTime, int extendSecond){
        super (name);
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.extendSecond = extendSecond;
    }
    // constructor day du cho dao
    public AuctionConfig (String id, String name, long minIncrement, LocalDateTime startTime, LocalDateTime endTime, int extendSecond){
        super (id, name);
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.extendSecond = extendSecond;
    }
    // getter setter
    public long getMinIncrement() {
        return minIncrement;
    }
    public void setMinIncrement(long minIncrement) {
        this.minIncrement = minIncrement;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public int getExtendSecond() {
        return extendSecond;
    }
    public void setExtendSecond(int extendSecond) {
        this.extendSecond = extendSecond;
    }
}
