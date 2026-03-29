package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;

public class Arts extends Item {
    public Arts(String Id, String Name, double BasePrice, LocalDate Manufacturingime, String Author, String ItemDescibe){
        super(Id, Name, BasePrice, Manufacturingime, Author, ItemDescibe);
    }
}