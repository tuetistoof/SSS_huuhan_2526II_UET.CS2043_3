package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.base.User;

public class Seller extends User {
    private String bankAccount;
    public Seller (String name, String userName, String password, String email, UserRole role, String bankAccount)
    {
        super(name, userName, password, email, role);
        this.bankAccount = bankAccount;
    }
    public Seller (String id, String name, String userName, String password, String email, UserRole role, String bankAccount)
    {
        super(name, userName, password, email, role);
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
