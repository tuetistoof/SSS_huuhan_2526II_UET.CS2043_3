package com.SSScloud.auction;

import java.time.LocalDate;

public abstract class Item extends Entity{
    private double BasePrice;
    private LocalDate Manufacturingime;

    public Item(String Id, String Name, double BasePrice, LocalDate Manufacturingime){
        super(Id, Name);
        this.BasePrice;= BasePrice;
        this.Manufacturingime = Manufacturingime;
    }

    public void SetBasePrice(double BasePrice){
        this.BasePrice = BasePrice;
    }
}

