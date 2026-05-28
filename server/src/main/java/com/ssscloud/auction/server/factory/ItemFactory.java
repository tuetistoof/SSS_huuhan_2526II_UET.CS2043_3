package com.ssscloud.auction.server.factory;

import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.FactoryException;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.payload.request.CreateAuctionRequest;
import com.ssscloud.auction.common.payload.request.ItemData;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ItemFactory {

    private static final Logger logger = Logger.getLogger(ItemFactory.class.getName());
    private static final Map<String, ItemFactory> FACTORIES = Map.of(
            "ART", new ArtFactory(),
            "VEHICLE", new VehicleFactory(),
            "ELECTRONIC", new ElectronicFactory()
    );

    protected ItemFactory() {}

    public static Item createItem(CreateAuctionRequest createAuctionRequest, String sellerId)
            throws FactoryException, Exception {
        try {
            logger.log(Level.INFO, "Initiating item creation from request for sellerId: " + sellerId);

            validateCreateAuctionRequest(createAuctionRequest, sellerId);

            ItemData itemData = createAuctionRequest.getItemData();
            String itemName = resolveItemName(itemData, createAuctionRequest);
            String itemType = normalizeItemType(itemData);

            ItemFactory factory = getFactory(itemType);
            Item item = factory.createConcreteItem(itemData);
            populateItemAttributes(item, itemData, itemName, sellerId);

            logger.log(Level.INFO, "Item entity successfully created with type: " + itemType);
            return item;
        } catch (FactoryException factoryException) {
            throw factoryException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Loi he thong khong xac dinh tai ItemFactory.createItem: "
                    + exception.getMessage(), exception);
            throw exception;
        }
    }

    protected abstract Item createConcreteItem(ItemData itemData) throws FactoryException;

    private static ItemFactory getFactory(String itemType) throws FactoryException {
        ItemFactory factory = FACTORIES.get(itemType);
        if (factory == null) {
            logger.log(Level.SEVERE, "Factory creation failure: Invalid item type encountered: " + itemType);
            throw new FactoryException(ErrorCode.INVALID_ITEM_TYPE, "Unsupported item type for creation: " + itemType);
        }
        return factory;
    }
    
    //Helpers

    private static void validateCreateAuctionRequest(CreateAuctionRequest createAuctionRequest, String sellerId)
            throws FactoryException {
        if (createAuctionRequest == null) {
            logger.log(Level.SEVERE, "Validation failure: CreateAuctionRequest object is null.");
            throw new FactoryException(ErrorCode.INVALID_AUCTION_ID, "The CreateAuctionRequest object cannot be null.");
        }
        if (createAuctionRequest.getItemData() == null) {
            logger.log(Level.SEVERE, "Validation failure: ItemData within CreateAuctionRequest is null.");
            throw new FactoryException(ErrorCode.ITEM_NOT_FOUND, "The ItemData object within the request cannot be null.");
        }
        if (sellerId == null || sellerId.isBlank()) {
            logger.log(Level.SEVERE, "Validation failure: SellerId is null or blank.");
            throw new FactoryException(ErrorCode.INVALID_AUCTION_ID, "The SellerId cannot be null or blank.");
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

    private static String normalizeItemType(ItemData itemData) throws FactoryException {
        String rawType = itemData.getItemType();
        String itemType = rawType != null ? rawType.trim().toUpperCase() : "";

        if (itemType.isBlank()) {
            logger.log(Level.SEVERE, "Validation failure: Item type is blank or null.");
            throw new FactoryException(ErrorCode.INVALID_ITEM_TYPE, "The item type cannot be null or blank.");
        }
        return itemType;
    }

    private static void populateItemAttributes(Item item, ItemData itemData, String itemName, String sellerId) {
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