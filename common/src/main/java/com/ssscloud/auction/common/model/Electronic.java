package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;
import java.time.Period;

public class Electronic extends Item {
    private LocalDate purchaseDate;
    private Period warrantyPeriod;

    // constructor ko co 2 thuoc tinh rieng cua Electronic vi chi khi mua hang moi xuat hien trang thai day nen lat nua viet o setter
    public Electronic (String name, double basePrice, LocalDate manufacturingDate,String creator, String description, Period warrantyPeriod){        
        super(name, basePrice, manufacturingDate, creator, description);
        this.purchaseDate = null;
        this.warrantyPeriod = warrantyPeriod;
    }
    public Electronic(){}
    
    // neu sân pham da sam xuat lau hon 3 nam thi giam 10%
    @Override
    public double getPrice() {
        LocalDate now = LocalDate.now();
        Period period = Period.between(super.getManufacturingDate(), now);
        double lastPrice = super.getBasePrice();
        if (period.getYears() >= 3)
            lastPrice = super.getBasePrice() * 0.9;
        return lastPrice + Math.max (super.getBasePrice() * super.getTransactionFee(), super.getMaxTransactionFee());
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
