package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;

public class Electronics extends Item {
    private int WarantineTime;

    public Electronics(String Id, String Name, double BasePrice, LocalDate Manufacturingime, String Author, String ItemDescibe, int WarantineTime){
        super(Id, Name, BasePrice, Manufacturingime, Author, ItemDescibe);
        this.WarantineTime = WarantineTime;
    }

}
