package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;

public class Vehicle extends Item {
    public Vehicle(String Id, String Name, double BasePrice, LocalDate ManufacturingDate, String Brand, String Description){
        super(Id, Name, BasePrice, ManufacturingDate, Brand, Description);
    }

    @Override
    public double getPrice(){
        return super.getBasePrice();
    }
}
