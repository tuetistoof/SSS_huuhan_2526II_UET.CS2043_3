package com.ssscloud.auction.common.model.base;

import java.time.LocalDate;

public abstract class Item extends Entity{
    private double BasePrice;
    private LocalDate ManufacturingDate;
    private String Brand, Description;

    public Item(String Id, String Name, double BasePrice, LocalDate ManufacturingDate, String Brand, String Description){
        super(Id, Name);
        this.BasePrice = BasePrice;
        this.ManufacturingDate = ManufacturingDate;
        this.Brand = Brand;
        this.Description = Description;
    }

    // tinh gia san pham
    public abstract getPrice();
    
    // getter setter
    public double getBasePrice() {
        return BasePrice;
    }
    public void setBasePrice(double basePrice) {
        BasePrice = basePrice;
    }

    public LocalDate getManufacturingDate() {
        return ManufacturingDate;
    }
    public void setManufacturingDate(LocalDate manufacturingDate) {
        ManufacturingDate = manufacturingDate;
    }

    public String getBrand() {
        return Brand;
    }
    public void setBrand(String brand) {
        Brand = brand;
    }

    public String getDescription() {
        return Description;
    }
    public void setDescription(String description) {
        Description = description;
    }
}

