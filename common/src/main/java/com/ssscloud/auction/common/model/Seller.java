package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.base.User;

public class Seller extends User {
    private String bankAccount;
    private long accountBalance;
    private long pendingBalance;
    public Seller (String name, String username, String password, String email, UserRole role)
    {
        super(name, username, password, email, role);
        this.bankAccount = null;
        this.accountBalance = 0;
        this.pendingBalance = 0;
    }
    public Seller (String name, String username, String password, String email, UserRole role, String bankAccount)
    {
        super(name, username, password, email, role);
        this.bankAccount = bankAccount;
        this.accountBalance = 0;
        this.pendingBalance = 0;
    }
    public Seller (String name, String username, String password, String email, UserRole role, String bankAccount, long accountBalance)
    {
        super(name, username, password, email, role);
        this.bankAccount = bankAccount;
        this.accountBalance = accountBalance;
        this.pendingBalance = 0;
    }
    public Seller (String name, String username, String password, String email, UserRole role, String bankAccount, long accountBalance, long pendingBalance)
    {
        super(name, username, password, email, role);
        this.bankAccount = bankAccount;
        this.accountBalance = accountBalance;
        this.pendingBalance = pendingBalance;
    }
    public Seller (String id, String name, String username, String password, String email, UserRole role, String bankAccount, long accountBalance, long pendingBalance)
    {
        super(id, name, username, password, email, role);
        this.bankAccount = bankAccount;
        this.accountBalance = accountBalance;
        this.pendingBalance = pendingBalance;
    }
    public long getPendingBalance() {
        return pendingBalance;
    }
    public void setPendingBalance(long pendingBalance) {
        this.pendingBalance = pendingBalance;
    }
    
    //getter setter
    public String getBankAccount() {
        return bankAccount;
    }
    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public long getAccountBalance() {
        return accountBalance;
    }
    public void setAccountBalance(long accountBalance) {
        this.accountBalance = accountBalance;
    }
}
    