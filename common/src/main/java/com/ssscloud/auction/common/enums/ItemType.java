package com.ssscloud.auction.common.enums;

public enum ItemType {
    ART,
    VEHICLE,
    ELECTRONIC;
    
    public boolean isArt(){
        return this == ART;
    }
    public boolean isVehicle(){
        return this == VEHICLE;
    }
    public boolean isElectronic(){
        return this == ELECTRONIC;
    }
    
}
