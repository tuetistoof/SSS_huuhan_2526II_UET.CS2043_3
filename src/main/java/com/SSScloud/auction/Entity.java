package com.SSScloud.auction;

import java.time.LocalDate;

public abstract class Entity {
    private String Name;
     private LocalDate TimeImport;

    public Entity(String Name, LocalDate TimeImport){
        this.Name = Name;
        this.TimeImport = TimeImport;
    }

    public void setName(){
        this.Name = Name;
    }
    public String getName(String Name){
        return this.Name;
    }
}