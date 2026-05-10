package com.ssscloud.auction.server.controller;

import com.ssscloud.auction.server.dao.ItemDAO;

public class ItemController {
    private ItemDAO itemDAO;
    public ItemController (ItemDAO itemDAO)
    {
        this.itemDAO = itemDAO;
    }
}
