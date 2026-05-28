package com.ssscloud.auction.server.factory;

import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.model.item.Electronic;
import com.ssscloud.auction.common.payload.request.ItemData;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ElectronicFactory extends ItemFactory {

    private static final Logger logger = Logger.getLogger(ElectronicFactory.class.getName());

    @Override
    protected Item createConcreteItem(ItemData itemData) {
        Electronic electronic = new Electronic();
        electronic.setIsRepaired(itemData.isRepaired());
        electronic.setWarrantyPeriod(itemData.getWarrantyPeriod());
        logger.log(Level.FINE, "Electronic item created with warranty period: " + itemData.getWarrantyPeriod());
        return electronic;
    }
}