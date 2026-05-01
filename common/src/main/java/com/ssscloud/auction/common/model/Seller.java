package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.base.User;

public class Seller extends User {
    private String bankAccount;
    public Seller (String name, String username, String password, String email, UserRole role)
    {
        super(name, username, password, email, role);
        this.bankAccount = null;
    }
    public Seller (String name, String username, String password, String email, UserRole role, String bankAccount)
    {
        super(name, username, password, email, role);
        this.bankAccount = bankAccount;
    }
    public Seller (String id, String name, String username, String password, String email, UserRole role, String bankAccount)
    {
        super(id, name, username, password, email, role);
        this.bankAccount = bankAccount;
    }
    //getter setter
    public String getBankAccount() {
        return bankAccount;
    }
    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }
}
