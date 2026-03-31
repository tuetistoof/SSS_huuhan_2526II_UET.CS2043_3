package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;

public class Electronic extends Item {
    private LocalDate ;

    public Electronic(String Id, String Name, double BasePrice, LocalDate Manufacturingime, String Author, String ItemDescibe, int WarantineTime){
        super(Id, Name, BasePrice, Manufacturingime, Author, ItemDescibe);
        this.WarantineTime = WarantineTime;
    }
}
