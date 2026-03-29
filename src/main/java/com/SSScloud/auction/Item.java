package com.SSScloud.auction;

import java.time.LocalDate;

public abstract class Item extends Entity{
    private double BasePrice;
    private LocalDate Manufacturingime;
    private String Author, ItemDescibe;

    public Item(String Id, String Name, double BasePrice, LocalDate Manufacturingime, String Author, String ItemDescibe){
        super(Id, Name);
        this.BasePrice = BasePrice;
        this.Manufacturingime = Manufacturingime;
        this.Author = Author;
        this.ItemDescibe = ItemDescibe;
    }

    public void SetBasePrice(double BasePrice){
        this.BasePrice = BasePrice;
    }

    public double getFinalPrice(){
        return this.BasePrice;
    }

    public String getAuthor(){
        return this.Author;        
    }
    
    @Override
    public String toString() {
        return this.ItemDescibe;
    }
}

