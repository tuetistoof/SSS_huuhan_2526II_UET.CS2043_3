package com.ssscloud.auction.server.factory;

import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.model.item.Vehicle;
import com.ssscloud.auction.common.payload.request.ItemData;

import java.util.logging.Level;
import java.util.logging.Logger;

public class VehicleFactory extends ItemFactory {

    private static final Logger logger = Logger.getLogger(VehicleFactory.class.getName());

    @Override
    protected Item createConcreteItem(ItemData itemData) {
        Vehicle vehicle = new Vehicle();
        vehicle.setIsRepaired(itemData.isRepaired());
        vehicle.setWarrantyPeriod(itemData.getWarrantyPeriod());
        logger.log(Level.FINE, "Vehicle item created with warranty period: " + itemData.getWarrantyPeriod());
        return vehicle;
    }
}