package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;

public class Art extends Item {
    public Art(String id, String name, double basePrice, double transactionFee, LocalDate manufacturingDate,String creator, String description){
        super(id, name, basePrice, transactionFee, manufacturingDate, creator, description);
    }
    @Override
    public double getPrice() {
        return super.getBasePrice() + super.getTransactionFee();
    }

}