package com.SSScloud.auction;

import java.time.LocalDate;

public class Arts extends Item{
    public Arts(String Id, String Name, double BasePrice, LocalDate Manufacturingime, String Author, String ItemDescibe){
        super(Id, Name, BasePrice, Manufacturingime, Author, ItemDescibe);
    }
}