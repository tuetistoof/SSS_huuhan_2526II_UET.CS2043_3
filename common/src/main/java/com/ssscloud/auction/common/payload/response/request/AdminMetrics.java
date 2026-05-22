package com.ssscloud.auction.common.payload.response.request;

import java.io.Serializable;

public class AdminMetrics implements Serializable {
    // FIX #5: Đổi RunningCount, EndedCount → camelCase để đúng Java convention
    private long runningCount, endedCount, totalUsers;

    public AdminMetrics(long runningCount, long endedCount, long totalUsers) {
        this.runningCount = runningCount;
        this.endedCount   = endedCount;
        this.totalUsers   = totalUsers;
    }

    public long getRunningCount() {
        return runningCount;
    }
    public void setRunningCount(long runningCount) {
        this.runningCount = runningCount;
    }

    public long getEndedCount() {
        return endedCount;
    }
    public void setEndedCount(long endedCount) {
        this.endedCount = endedCount;
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
               "runningCount=" + runningCount +
               ", endedCount=" + endedCount +
               ", totalUsers=" + totalUsers +
               '}';
    }
}