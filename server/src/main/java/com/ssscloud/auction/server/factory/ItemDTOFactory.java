package com.ssscloud.auction.server.factory;

import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.FactoryException;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.model.item.Art;
import com.ssscloud.auction.common.model.item.Electronic;
import com.ssscloud.auction.common.model.item.Vehicle;
import com.ssscloud.auction.common.payload.response.DTO.ArtDTO;
import com.ssscloud.auction.common.payload.response.DTO.ElectricDTO;
import com.ssscloud.auction.common.payload.response.DTO.ItemDTO;
import com.ssscloud.auction.common.payload.response.DTO.VehicleDTO;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemDTOFactory {

    private static final Logger logger = Logger.getLogger(ItemDTOFactory.class.getName());

    private ItemDTOFactory() {
        /* Private constructor to prevent instantiation of utility class */
    }

    public static ItemDTO toDto(Item itemEntity) throws FactoryException, Exception {
        try {
            logger.log(Level.INFO, "Initiating conversion from Item entity to ItemDto.");
            validateItem(itemEntity);
    
            ItemDTO itemDto = buildItemDtoByType(itemEntity);
            populateItemDtoAttributes(itemDto, itemEntity);
    
            logger.log(Level.INFO, "Item entity successfully converted to DTO with type: " + itemEntity.getType());
            return itemDto;
        } catch (FactoryException factoryException) {
            // Re-throw specific business exceptions, already logged at their source
            throw factoryException;
        } catch (Exception exception) {
            // Catch and log any unexpected system errors
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in " + ItemDTOFactory.class.getSimpleName() + ".toDto: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    /**
     * Cast an ItemDTO (already a subclass) to the corresponding DTO subclass based on itemType.
     * Used when deserialized generic ItemDTO needs to be treated as a specific subtype.
     */
    public static ItemDTO castToSubDto(ItemDTO itemDto) throws FactoryException, Exception {
        try {
            logger.log(Level.INFO, "Casting generic ItemDto to specific subtype: " + itemDto.getItemType());
            validateItemDto(itemDto);
    
            if (itemDto instanceof ArtDTO || itemDto instanceof VehicleDTO || itemDto instanceof ElectricDTO) {
                logger.log(Level.FINE, "ItemDto is already a specific subtype instance.");
                return itemDto;
            }
    
            String itemType = itemDto.getItemType();
            ItemDTO subDto = createItemDtoSubclass(itemType);
            populateSubDtoAttributes(subDto, itemDto);
    
            logger.log(Level.INFO, "ItemDto successfully cast to subtype: " + itemType);
            return subDto;
        } catch (FactoryException factoryException) {
            throw factoryException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in " + ItemDTOFactory.class.getSimpleName() + ".castToSubDto: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    private static void validateItem(Item itemEntity) throws FactoryException {
        if (itemEntity == null) {
            logger.log(Level.SEVERE, "Validation failure: Provided Item entity for conversion is null.");
            throw new FactoryException(ErrorCode.ITEM_NOT_FOUND, "The Item entity source cannot be null.");
        }
    }

    private static ItemDTO buildItemDtoByType(Item itemEntity) throws FactoryException {
        return switch (itemEntity) {
            case Art artEntity -> createArtDto(artEntity);
            case Vehicle vehicleEntity -> createVehicleDto(vehicleEntity);
            case Electronic electronicEntity -> createElectronicDto(electronicEntity);
            default -> {
                logger.log(Level.SEVERE, "Invalid item type encountered: " + itemEntity.getClass().getSimpleName());
                throw new FactoryException(ErrorCode.INVALID_ITEM_TYPE, "Invalid item type: " + itemEntity.getClass().getSimpleName());
            }
        };
    }

    private static ArtDTO createArtDto(Art artEntity) {
        ArtDTO artDto = new ArtDTO();
        artDto.setCertificate(artEntity.getCertificate());
        logger.log(Level.FINE, "ArtDto instantiated with certificate: " + artEntity.getCertificate());
        return artDto;
    }

    private static VehicleDTO createVehicleDto(Vehicle vehicleEntity) {
        VehicleDTO vehicleDto = new VehicleDTO();
        vehicleDto.setIsRepaired(vehicleEntity.getIsRepaired());
        vehicleDto.setWarrantyPeriod(vehicleEntity.getWarrantyPeriod());
        logger.log(Level.FINE, "VehicleDto instantiated with warranty: " + vehicleEntity.getWarrantyPeriod());
        return vehicleDto;
    }

    private static ElectricDTO createElectronicDto(Electronic electronicEntity) {
        ElectricDTO electricDto = new ElectricDTO();
        electricDto.setIsRepaired(electronicEntity.getIsRepaired());
        electricDto.setWarrantyPeriod(electronicEntity.getWarrantyPeriod());
        logger.log(Level.FINE, "ElectricDto instantiated with warranty: " + electronicEntity.getWarrantyPeriod());
        return electricDto;
    }

    private static void populateItemDtoAttributes(ItemDTO itemDto, Item itemEntity) {
        itemDto.setId(itemEntity.getId());
        itemDto.setName(itemEntity.getName());
        itemDto.setSellerId(itemEntity.getSellerId());
        itemDto.setCreator(itemEntity.getCreator());
        itemDto.setDescription(itemEntity.getDescription());
        itemDto.setItemType(itemEntity.getType());
        itemDto.setImageUrls(itemEntity.getImageUrl());
    }

    private static void validateItemDto(ItemDTO itemDto) throws FactoryException {
        if (itemDto == null) {
            logger.log(Level.SEVERE, "Validation failure: ItemDto provided for casting is null.");
            throw new FactoryException(ErrorCode.ITEM_NOT_FOUND, "The source ItemDto cannot be null.");
        }

        String itemType = itemDto.getItemType();
        if (itemType == null || itemType.isBlank()) {
            logger.log(Level.SEVERE, "Validation failure: itemType attribute is missing in ItemDto.");
            throw new FactoryException(ErrorCode.INVALID_ITEM_TYPE, "ItemType attribute must not be null or blank.");
        }
    }

    private static ItemDTO createItemDtoSubclass(String itemType) throws FactoryException {
        return switch (itemType.trim().toUpperCase()) {
            case "ART" -> new ArtDTO();
            case "VEHICLE" -> new VehicleDTO();
            case "ELECTRONIC" -> new ElectricDTO();
            default -> {
                logger.log(Level.SEVERE, "Invalid item type for casting: " + itemType);
                throw new FactoryException(
                    ErrorCode.INVALID_ITEM_TYPE, 
                    "Invalid item type: " + itemType
                );
            }
        };
    }

    private static void populateSubDtoAttributes(ItemDTO subDto, ItemDTO sourceDto) {
        subDto.setId(sourceDto.getId());
        subDto.setName(sourceDto.getName());
        subDto.setSellerId(sourceDto.getSellerId());
        subDto.setCreator(sourceDto.getCreator());
        subDto.setDescription(sourceDto.getDescription());
        subDto.setItemType(sourceDto.getItemType());
        subDto.setImageUrls(sourceDto.getImageUrls());
    }
}