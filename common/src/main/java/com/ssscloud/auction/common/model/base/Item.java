package com.ssscloud.auction.common.model.base;

import java.time.LocalDate;

public abstract class Item extends Entity{
    private double basePrice;
    private LocalDate manufacturingDate;
    private String creator;
    private String description;
    private double transactionFee;
    public Item(String name, double basePrice, double transactionFee, LocalDate manufacturingDate,String creator, String description){
        super(name);
        this.basePrice = basePrice;
        this.manufacturingDate = manufacturingDate;
        this.creator = creator;
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
        return creator;
    }
    public void setBrand(String creator) {
        this.creator = creator;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}

