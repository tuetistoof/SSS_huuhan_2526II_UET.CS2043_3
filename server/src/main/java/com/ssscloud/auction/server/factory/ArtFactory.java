package com.ssscloud.auction.server.factory;

import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.model.item.Art;
import com.ssscloud.auction.common.payload.request.ItemData;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ArtFactory extends ItemFactory {

    private static final Logger logger = Logger.getLogger(ArtFactory.class.getName());

    @Override
    protected Item createConcreteItem(ItemData itemData) {
        Art art = new Art();
        art.setCertificate(itemData.isHasCertificate());
        logger.log(Level.FINE, "Art item created with certificate status: " + itemData.isHasCertificate());
        return art;
    }
}