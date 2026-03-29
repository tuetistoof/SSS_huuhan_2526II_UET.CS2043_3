package com.SSScloud.auction;

public abstract class User extends Entity{
    private String UserName;
    private String Password;
    public User (String Id, String Name, String UserName, String Password)
    {
        super (Id, Name);
        this.UserName = UserName;
        this.Password = Password;
    }
}
