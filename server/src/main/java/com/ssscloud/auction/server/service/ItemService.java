package com.ssscloud.auction.server.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.payload.response.DTO.ItemDTO;
import com.ssscloud.auction.server.dao.ItemDAO;

public class ItemService {
    private static final Logger logger = Logger.getLogger(ItemService.class.getName()); // Logging Standard: Declared first

    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    // --- PUBLIC METHODS ---

    public void saveItem(Item item) throws ServiceException, Exception {
        try {
            boolean isSaved = switch (item.getType()) {
                case "ART"        -> itemDAO.saveArt((com.ssscloud.auction.common.model.item.Art) item);
                case "VEHICLE"    -> itemDAO.saveVehicle((com.ssscloud.auction.common.model.item.Vehicle) item);
                case "ELECTRONIC" -> itemDAO.saveElectronic((com.ssscloud.auction.common.model.item.Electronic) item);
                default -> {
                    logger.log(Level.WARNING, "Unsupported item type encountered during save: " + item.getType());
                    throw new ServiceException(ErrorCode.ITEM_TYPE_UNSUPPORTED, "The provided item type is currently not supported: " + item.getType());
                }
            };
    
            if (!isSaved) {
                throw new ServiceException(ErrorCode.ITEM_SAVE_FAILED, "Critical failure: Failed to persist the item entity to the database.");
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Lỗi hệ thống tại ItemService.saveItem: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    public ItemDTO getItemById(String itemId) throws ServiceException, Exception { 
        try {
            validateItemId(itemId); 
            Item item = itemDAO.findById(itemId);
            if (item == null) {
                throw new ServiceException(ErrorCode.ITEM_NOT_FOUND, "Resource not found: Item with identifier " + itemId + " does not exist.");
            }
            ItemDTO itemDto = item.toDto();
            return itemDto;
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Lỗi hệ thống tại ItemService.getItemById: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    // --- VALIDATION METHODS ---

    private void validateItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_DATA, "Validation failure: The provided ItemId identifier cannot be null or empty.");
        }
    }
}
