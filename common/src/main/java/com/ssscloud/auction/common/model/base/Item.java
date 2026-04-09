package com.ssscloud.auction.common.model.base;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class Item extends Entity{
    // tinh theo gia viet nam nen de la long
    private long basePrice;
    private LocalDate manufacturingDate;
    private String creator;
    private String description;
    private String sellerId;   
    private List <String> imageUrl = new ArrayList<>(); 
    // khong nen de la final de cho he thong co the sua duoc kieu v
    // he thong tinh toan tien tu dong lam tron len
    private final double transactionFee = 0.1; 
    private final double maxTransactionFee = 10000.0;
    public Item(String sellerID, String name, long basePrice, LocalDate manufacturingDate,String creator, String description){
        super(name);
        this.sellerId = sellerID;
        this.basePrice = basePrice;
        this.manufacturingDate = manufacturingDate;
        this.creator = creator;
        this.description = description;
    }
    // them anh xoa anh
    public void addImage (String url)
    {
        imageUrl.add (url);
    }
    public void delImage (String url)
    {
        if (imageUrl.remove(url))
            System.out.println ("xoa thanh cong");
        else System.out.println ("khong co anh");
    }
    // tinh gia san pham
    public abstract double getPrice();
    // getter setter
    // khong thay doi duoc nguoi ban
    public String getSellerId() {
        return sellerId;
    }
    public double getBasePrice() {
        return basePrice;
    }
    public void setBasePrice(long basePrice) {
        this.basePrice = basePrice;
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
    public double getTransactionFee() {
        return transactionFee;
    }
    public double getMaxTransactionFee() {
        return maxTransactionFee;
    }
}