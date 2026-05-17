package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.base.User;

public class Bidder extends User {
    private long accountBalance;
    private long lockedBalance;

    // Constructor tạo mới (chưa có id, chưa có balance)
    public Bidder(String name, String userName, String password, String email, UserRole role) {
        super(name, userName, password, email, role);
        this.accountBalance = 0;
        this.lockedBalance = 0;
    }

    // Constructor tạo mới có balance (ít dùng)
    public Bidder(String name, String userName, String password, String email, UserRole role, long accountBalance) {
        super(name, userName, password, email, role);
        this.accountBalance = accountBalance;
        this.lockedBalance = 0;
    }

    // Constructor load từ DB — đầy đủ nhất
    public Bidder(String id, String name, String userName, String password, String email, UserRole role, long accountBalance, long lockedBalance) {
        super(id, name, userName, password, email, role);
        this.accountBalance = accountBalance;
        this.lockedBalance = lockedBalance;
    }

    // --- Business logic ---

    public long getLockedBalance() { return lockedBalance; }
    public long getAvailableBalance() { return accountBalance - lockedBalance; }


    // --- Getters / Setters ---

    public long getAccountBalance() { return accountBalance; }
    public void setAccountBalance(long accountBalance) { this.accountBalance = accountBalance; }
}