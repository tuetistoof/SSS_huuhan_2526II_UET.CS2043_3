package com.ssscloud.auction.common.model.base;

import java.time.LocalDate;

public abstract class Item extends Entity{
    private double BasePrice;
    private LocalDate ManufacturingDate;
    private String Brand, ItemDescibe;

    public Item(String Id, String Name, double BasePrice, LocalDate ManufacturingDate, String Brand, String ItemDescibe){
        super(Id, Name);
        this.BasePrice = BasePrice;
        this.ManufacturingDate = ManufacturingDate;
        this.Brand = Brand;
        this.ItemDescibe = ItemDescibe;
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

    public String getItemDescibe() {
        return ItemDescibe;
    }
    public void setItemDescibe(String itemDescibe) {
        ItemDescibe = itemDescibe;
    }
}

