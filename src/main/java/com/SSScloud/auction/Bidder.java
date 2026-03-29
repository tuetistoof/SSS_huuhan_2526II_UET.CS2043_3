package com.SSScloud.auction;

public class Bidder extends User {
    private long AccountBalance;
    public Bidder (String Id, String Name, String UserName, String Password, long AccountBalance)
    {
        super (Id, Name, UserName, Password);
        this.AccountBalance = AccountBalance;
    }
    
}
