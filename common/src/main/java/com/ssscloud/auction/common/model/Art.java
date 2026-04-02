package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;

public class Art extends Item {
    public Art(String name, double basePrice, LocalDate manufacturingDate,String creator, String description){
        super(name, basePrice, manufacturingDate, creator, description);
    }
    @Override
    public double getPrice() {
        return super.getBasePrice() + Math.max (super.getBasePrice() * super.getTransactionFee(), super.getMaxTransactionFee());
    }

}