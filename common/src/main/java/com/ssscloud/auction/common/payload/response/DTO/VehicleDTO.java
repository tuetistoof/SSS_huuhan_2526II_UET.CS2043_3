package com.ssscloud.auction.common.payload.response.DTO;

public class VehicleDTO extends ItemDTO {
    private static final long serialVersionUID = 1L;
    private boolean isRepaired;
    private int warrantyPeriod;
    public boolean getIsRepaired() {
        return isRepaired;
    }
    public void setIsRepaired(boolean isRepaired) {
        this.isRepaired = isRepaired;
    }

    public int getWarrantyPeriod() {
        return warrantyPeriod;
    }
    
    public void setWarrantyPeriod(int warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }
}
