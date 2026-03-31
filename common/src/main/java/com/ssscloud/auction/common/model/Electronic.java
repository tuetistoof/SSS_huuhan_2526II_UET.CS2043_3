package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;
import java.time.Period;

public class Electronic extends Item {
    private LocalDate purchaseDate;
    private Period warrantyPeriod;

    // constructor ko co 2 thuoc tinh rieng cua Electronic vi chi khi mua hang moi xuat hien trang thai day nen lat nua viet o setter
    public Electronic (String name, double basePrice, double transactionFee, LocalDate manufacturingDate,String creator, String description){        
        super(name, basePrice, transactionFee, manufacturingDate, creator, description);
        this.purchaseDate = null;
        this.warrantyPeriod = Period.ZERO;        
    }
    
    // neu sân pham da sam xuat lau hon 3 nam thi giam 10%
    @Override
    public double getPrice() {
        LocalDate now = LocalDate.now();
        Period period = Period.between(super.getManufacturingDate(), now);
        if (period.getYears() >= 3)
            return super.getBasePrice() * 0.9 + super.getTransactionFee();
        else return super.getBasePrice() + super.getTransactionFee();
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
