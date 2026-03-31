package com.ssscloud.auction.common.model.base;

import java.time.LocalDate;

public abstract class Item extends Entity{
    private double basePrice;
    private LocalDate manufacturingDate;
    private String brand;
    private String description;
    private double transactionFee;
    public Item(String id, String name, double basePrice, double transactionFee, LocalDate manufacturingDate,String brand, String description){
        super(id, name);
        this.basePrice = basePrice;
        this.manufacturingDate = manufacturingDate;
        this.brand = brand;
        this.description = description;
    }

    // tinh gia san pham
    public abstract double getPrice();
    // getter setter
    public double getBasePrice() {
        return basePrice;
    }
    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getTransactionFee() {
        return transactionFee;
    }
    public void setTransactionFee(double transactionFee) {
        this.transactionFee = transactionFee;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }
    public void setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
    }

    public String getBrand() {
        return brand;
    }
    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}

