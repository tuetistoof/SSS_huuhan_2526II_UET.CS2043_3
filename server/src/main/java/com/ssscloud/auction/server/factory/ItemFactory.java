package com.ssscloud.auction.server.factory;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.request.ItemData;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.FactoryExceptions;
import com.ssscloud.auction.common.model.Art;
import com.ssscloud.auction.common.model.Electronic;
import com.ssscloud.auction.common.model.Vehicle;
import com.ssscloud.auction.common.model.base.Item;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemFactory {

    private static final Logger logger = Logger.getLogger(ItemFactory.class.getName());

    private ItemFactory() {
        /* Private constructor to prevent instantiation of utility class */
    }

    public static Item createItem(CreateAuctionRequest createAuctionRequest, String sellerId) 
            throws FactoryExceptions {
        logger.log(Level.INFO, "Initiating item creation from request for sellerId: " + sellerId);
        
        validateCreateAuctionRequest(createAuctionRequest, sellerId);
        
        ItemData itemData = createAuctionRequest.getItemData();
        String itemName = resolveItemName(itemData, createAuctionRequest);
        String itemType = normalizeItemType(itemData);
        
        Item item = buildItemByType(itemType, itemData);
        populateItemAttributes(item, itemData, itemName, sellerId);
        
        logger.log(Level.INFO, "Item entity successfully created with type: " + itemType);
        return item;
    }

    private static void validateCreateAuctionRequest(CreateAuctionRequest createAuctionRequest, String sellerId) 
            throws FactoryExceptions {
        if (createAuctionRequest == null) {
            logger.log(Level.SEVERE, "Validation failure: CreateAuctionRequest object is null.");
            throw new FactoryExceptions(ErrorCode.INVALID_AUCTION_ID, "The CreateAuctionRequest object cannot be null.");
        }
        if (createAuctionRequest.getItemData() == null) {
            logger.log(Level.SEVERE, "Validation failure: ItemData within CreateAuctionRequest is null.");
            throw new FactoryExceptions(ErrorCode.ITEM_NOT_FOUND, "The ItemData object within the request cannot be null.");
        }
        if (sellerId == null || sellerId.isBlank()) {
            logger.log(Level.SEVERE, "Validation failure: SellerId is null or blank.");
            throw new FactoryExceptions(ErrorCode.INVALID_AUCTION_ID, "The SellerId cannot be null or blank.");
        }
    }

    private static String resolveItemName(ItemData itemData, CreateAuctionRequest createAuctionRequest) {
        String itemName = itemData.getName();
        if (itemName == null || itemName.isBlank()) {
            itemName = createAuctionRequest.getName();
            logger.log(Level.FINE, "Using item name from CreateAuctionRequest: " + itemName);
        }
        return itemName;
    }

    private static String normalizeItemType(ItemData itemData) 
            throws FactoryExceptions {
        String rawType = itemData.getItemType();
        String itemType = (rawType != null) ? rawType.trim().toUpperCase() : "";
        
        if (itemType.isBlank()) {
            logger.log(Level.SEVERE, "Validation failure: Item type is blank or null.");
            throw new FactoryExceptions(ErrorCode.INVALID_ITEM_TYPE, "The item type cannot be null or blank.");
        }
        return itemType;
    }

    private static Item buildItemByType(String itemType, ItemData itemData) 
            throws FactoryExceptions {
        return switch (itemType) {
            case "ART" -> createArtItem(itemData); // Renamed for consistency
            case "VEHICLE" -> createVehicleItem(itemData); // Renamed for consistency
            case "ELECTRONIC" -> createElectronicItem(itemData); // Renamed for consistency
            default -> {
                logger.log(Level.SEVERE, "Factory creation failure: Invalid item type encountered: " + itemType);
                throw new FactoryExceptions(ErrorCode.INVALID_ITEM_TYPE, "Unsupported item type for creation: " + itemType);
            }
        };
    }

    private static Art createArtItem(ItemData itemData) {
        Art art = new Art();
        art.setCertificate(itemData.isHasCertificate());
        logger.log(Level.FINE, "Art item created with certificate status: " + itemData.isHasCertificate());
        return art;
    }

    private static Vehicle createVehicleItem(ItemData itemData) {
        Vehicle vehicle = new Vehicle();
        vehicle.setIsRepaired(itemData.isRepaired());
        vehicle.setWarrantyPeriod(itemData.getWarrantyPeriod());
        logger.log(Level.FINE, "Vehicle item created with warranty period: " + itemData.getWarrantyPeriod());
        return vehicle;
    }

    private static Electronic createElectronicItem(ItemData itemData) {
        Electronic electronic = new Electronic();
        electronic.setIsRepaired(itemData.isRepaired());
        electronic.setWarrantyPeriod(itemData.getWarrantyPeriod());
        logger.log(Level.FINE, "Electronic item created with warranty period: " + itemData.getWarrantyPeriod());
        return electronic;
    }

    private static void populateItemAttributes(Item item, ItemData itemData, 
            String itemName, String sellerId) {
        item.setName(itemName);
        item.setDescription(itemData.getDescription());
        item.setCreator(itemData.getCreator());
        item.setSellerId(sellerId);

        if (itemData.getImageUrls() != null && !itemData.getImageUrls().isEmpty()) {
            itemData.getImageUrls().forEach(item::addImage);
            logger.log(Level.FINE, "Added " + itemData.getImageUrls().size() + " images to item.");
        }
    }

}
