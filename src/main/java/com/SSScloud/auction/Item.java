package com.SSScloud.auction;

import java.time.LocalDate;

public abstract class Item extends Entity{
    private String Id;
    private double BasePrice;

    public Item(String id, String name, double BasePrice, LocalDate TimeImport){
        super(name, TimeImport);
        this.Id = id;
        this.BasePrice = BasePrice;
    }

    public void SetBasePrice(double BasePrice){
        this.BasePrice = BasePrice;
    }
}

