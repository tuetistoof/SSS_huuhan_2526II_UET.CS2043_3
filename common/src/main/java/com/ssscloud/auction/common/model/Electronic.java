package com.ssscloud.auction.common.model;

import com.ssscloud.auction.common.enums.ItemType;
import com.ssscloud.auction.common.model.base.Item;

import java.time.LocalDate;
import java.util.List;

public class Electronic extends Item {
    private boolean isRepaired; 
    private int warrantyPeriod;
    // constructor ko co 2 thuoc tinh rieng cua Electronic vi chi khi mua hang moi xuat hien trang thai day nen lat nua viet o setter

    public Electronic() {} // dùng trong factory
    public Electronic (String name, String sellerId, long basePrice, LocalDate manufacturingDate,String creator, String description, ItemType type, List<String> imageUrl, boolean isRepaired, int warrantyPeriod){        
        super(name, sellerId, basePrice, manufacturingDate, creator, description, type, imageUrl);
        this.isRepaired = isRepaired;
        this.warrantyPeriod = warrantyPeriod;
    }
    public Electronic (String id, String name, String sellerId, long basePrice, LocalDate manufacturingDate,String creator, String description, ItemType type, List<String> imageUrl, boolean isRepaired, int warrantyPeriod){        
        super(id, name, sellerId, basePrice, manufacturingDate, creator, description, type,imageUrl);
        this.isRepaired = isRepaired;
        this.warrantyPeriod = warrantyPeriod;
    }
    
    // neu sân pham da sam xuat lau hon 3 nam thi giam 10%
    // @Override
    // public double getPrice() {
    //     LocalDate now = LocalDate.now();
    //     Period period = Period.between(super.getManufacturingDate(), now);
    //     double lastPrice = super.getBasePrice();
    //     if (period.getYears() >= 3)
    //         lastPrice = super.getBasePrice() * 0.9;
    //     return lastPrice + Math.max (super.getBasePrice() * super.getTransactionFee(), super.getMaxTransactionFee());
    // }
    
    public boolean checkIsRepair(){
        return isRepaired;
    }

    // getter setter

    public boolean getIsRepair(){
        return isRepaired;
    }
    public void setIsRepair(boolean isRepaired){
        this.isRepaired = isRepaired;
    }

    public int getWarrantyPeriod() {
        return warrantyPeriod;
    }
    public void setWarrantyPeriod(int warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }
}
