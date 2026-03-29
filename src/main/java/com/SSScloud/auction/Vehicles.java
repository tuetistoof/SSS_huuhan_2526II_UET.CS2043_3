package com.SSScloud.auction;

import java.time.LocalDate;

public class Vehicles extends Item{
    public Vehicles(String Id, String Name, String Author, double BasePrice, LocalDate Manufacturingime, String ItemDescibe){
        super(Id, Name, BasePrice, Manufacturingime, Author, ItemDescibe);
    }
}
