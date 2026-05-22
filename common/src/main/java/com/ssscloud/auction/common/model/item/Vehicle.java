package com.ssscloud.auction.common.model.item;


import com.ssscloud.auction.common.model.base.Item;


import java.util.List;

public class Vehicle extends Item {
    private boolean isRepaired;
    private int warrantyPeriod;


    public Vehicle() {} //constructor cần dùng trong factory
    public Vehicle (String name, String sellerId,String creator, String description, String type, List<String> imageUrl, boolean isRepaired, int warrantyPeriod){        
        super(name, sellerId, creator, description, type, imageUrl);
        this.isRepaired = isRepaired;
        this.warrantyPeriod = warrantyPeriod;
    }
    public Vehicle (String id, String name, String sellerId,String creator, String description, String type,  List<String> imageUrl, boolean isRepaired, int warrantyPeriod){        
        super(id, name, sellerId, creator, description, type,imageUrl);
        this.isRepaired = isRepaired;
        this.warrantyPeriod = warrantyPeriod;
    }
    
    @Override
    public String getType() {
        return "VEHICLE";
    }
    // @Override
    // public double getPrice(){
    //     return super.getBasePrice() + Math.max (super.getBasePrice() * super.getTransactionFee(), super.getMaxTransactionFee());
    // }
    // getter setter
    public boolean getIsRepaired(){
        return isRepaired;
    }
    public void setIsRepaired(boolean isRepaired){
        this.isRepaired = isRepaired;
    }
    public int getWarrantyPeriod() {
        return warrantyPeriod;
    }
    public void setWarrantyPeriod(int warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }
}
