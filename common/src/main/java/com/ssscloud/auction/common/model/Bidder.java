package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.User;

public class Bidder extends User {
    private long AccountBalance;
    public Bidder (String Id, String Name, String UserName, String Password, long AccountBalance)
    {
        super (Id, Name, UserName, Password);
        this.AccountBalance = AccountBalance;
    }
    
}
