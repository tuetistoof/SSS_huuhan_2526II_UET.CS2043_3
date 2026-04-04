package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.base.User;

public class Seller extends User {

    
    public Seller (String name, String userName, String password, String email, UserRole role)
    {
        super(name, userName, password, email, role);
    }
}
