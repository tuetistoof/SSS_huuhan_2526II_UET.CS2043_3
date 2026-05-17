package com.ssscloud.auction.common.dto.response;

import java.io.Serializable;

public class AdminMetrics implements Serializable {
    private long RunningCount, EndedCount, totalUsers;
    public AdminMetrics(long runningCount, long endedCount, long totalUsers) {
        this.RunningCount = runningCount;
        this.EndedCount = endedCount;
        this.totalUsers = totalUsers;
    }

    public long getRunningCount() {
        return RunningCount;
    }
    public void setRunningCount(long runningCount) {
        this.RunningCount = runningCount;
    }

    public long getEndedCount() {
        return EndedCount;
    }
    public void setEndedCount(long endedCount) {
        this.EndedCount = endedCount;
    }

    public long getTotalUsers() {
        return totalUsers;
    }
    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    @Override
    public String toString() {
        return "AdminMetrics{" +
               "RunningCount=" + RunningCount +
               ", EndedCount=" + EndedCount +
               ", totalUsers=" + totalUsers +
               '}';
    }   
}
