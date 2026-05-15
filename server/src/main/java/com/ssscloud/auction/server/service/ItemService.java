package com.ssscloud.auction.server.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceExceptions;
import com.ssscloud.auction.common.dto.response.ItemDTO;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.server.dao.ItemDAO;
import com.ssscloud.auction.server.factory.ItemDTOFactory;

public class ItemService {
    private static final Logger logger = Logger.getLogger(ItemService.class.getName()); // Logging Standard: Declared first

    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    // --- PUBLIC METHODS ---

    public void saveItem(Item item) throws ServiceExceptions {
        boolean isSaved = switch (item.getType()) {
            case "ART"        -> itemDAO.saveArt((com.ssscloud.auction.common.model.Art) item);
            case "VEHICLE"    -> itemDAO.saveVehicle((com.ssscloud.auction.common.model.Vehicle) item);
            case "ELECTRONIC" -> itemDAO.saveElectronic((com.ssscloud.auction.common.model.Electronic) item);
            default -> {
                logger.log(Level.WARNING, "Unsupported item type encountered during save: " + item.getType());
                throw new ServiceExceptions(ErrorCode.ITEM_TYPE_UNSUPPORTED, "The provided item type is currently not supported: " + item.getType());
            }
        };

        if (!isSaved) {
            throw new ServiceExceptions(ErrorCode.ITEM_SAVE_FAILED, "Critical failure: Failed to persist the item entity to the database.");
        }
    }

    public ItemDTO getItemById(String itemId) throws ServiceExceptions { 
        validateItemId(itemId); 
        Item item = itemDAO.findById(itemId);
        if (item == null) {
            throw new ServiceExceptions(ErrorCode.ITEM_NOT_FOUND, "Resource not found: Item with identifier " + itemId + " does not exist.");
        }
        ItemDTO itemDto = ItemDTOFactory.toDto
        (item);
        return itemDto;
    }

    // --- VALIDATION METHODS ---

    private void validateItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new ServiceExceptions(ErrorCode.INVALID_DATA, "Validation failure: The provided ItemId identifier cannot be null or empty.");
        }
    }
}
