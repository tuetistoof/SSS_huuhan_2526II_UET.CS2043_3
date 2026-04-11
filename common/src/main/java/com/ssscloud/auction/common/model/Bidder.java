package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.base.User;

public class Bidder extends User {
    private long accountBalance;
    // private long maxAutoBidAmount;
    // private long autoBidIncrement;
    public Bidder (String name, String userName, String password, String email, UserRole role, long accountBalance)
    {
        super(name, userName, password, email, role);
        this.accountBalance = accountBalance;
    }
    
    //getter setter
    public long getAccountBalance() {
        return accountBalance;
    }
    public void setAccountBalance(long accountBalance) {
        this.accountBalance = accountBalance;
    }
}
