package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;

public class Vehicle extends Item {
    private boolean isRepaired;
    private LocalDate purchaseDate;
    private int warrantyPeriod;


    public Vehicle() {} //constructor cần dùng trong factory
    public Vehicle (String name, String sellerId, long basePrice, LocalDate manufacturingDate,String creator, String description, boolean isRepaired, int warrantyPeriod){        
        super(name, sellerId, basePrice, manufacturingDate, creator, description);
        this.isRepaired = isRepaired;
        this.purchaseDate = null;
        this.warrantyPeriod = warrantyPeriod;
    }
    public Vehicle (String id, String name, String sellerId, long basePrice, LocalDate manufacturingDate,String creator, String description, boolean isRepaired,LocalDate purchaseDate, int warrantyPeriod){        
        super(id, name, sellerId, basePrice, manufacturingDate, creator, description);
        this.isRepaired = isRepaired;
        this.purchaseDate = purchaseDate;
        this.warrantyPeriod = warrantyPeriod;
    }
    
    // @Override
    // public double getPrice(){
    //     return super.getBasePrice() + Math.max (super.getBasePrice() * super.getTransactionFee(), super.getMaxTransactionFee());
    // }
    // getter setter
    public boolean getIsRepaired(){
        return isRepaired;
    }
    public void setIsRepaires(boolean isRepaired){
        this.isRepaired = isRepaired;
    }
    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }
    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public int getWarrantyPeriod() {
        return warrantyPeriod;
    }
    public void setWarrantyPeriod(int warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }
}
