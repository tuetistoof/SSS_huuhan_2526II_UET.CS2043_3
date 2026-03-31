package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;
import java.time.Period;

public class Vehicle extends Item {
    private LocalDate purchaseDate;
    private Period warrantyPeriod;

    public Vehicle (String id, String name, double basePrice, double transactionFee, LocalDate manufacturingDate,String creator, String description){        
        super(id, name, basePrice, transactionFee, manufacturingDate, creator, description);
        this.purchaseDate = null;
        this.warrantyPeriod = Period.ZERO;        
    }
    @Override
    public double getPrice(){
        return super.getBasePrice() + super.getTransactionFee();
    }

    // getter setter
    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }
    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Period getWarrantyPeriod() {
        return warrantyPeriod;
    }
    public void setWarrantyPeriod(Period warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }
}
