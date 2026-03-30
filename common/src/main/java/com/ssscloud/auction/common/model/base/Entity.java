package com.ssscloud.auction.common.model.base;

import java.time.LocalDate;

public abstract class Entity {
    private String Id;
    private String Name;
    private LocalDate  ManufacturingDate;
    public Entity(String Id, String Name, LocalDate ManufacturingDate){
        this.Id = Id;
        this.Name = Name;
        this.ManufacturingDate = ManufacturingDate;
    }

    public String getName(String Name){
        return this.Name;
    }

    public String getId() {
        return Id;
    }

    public LocalDate getManufacturingDate() {
        return ManufacturingDate;
    }

    public void setName(){
        this.Name = Name;   
    }

    public void setId(String id) {
        Id = id;
    }

    public void setManufacturingDate(LocalDate manufacturingDate) {
        ManufacturingDate = manufacturingDate;
    }
}