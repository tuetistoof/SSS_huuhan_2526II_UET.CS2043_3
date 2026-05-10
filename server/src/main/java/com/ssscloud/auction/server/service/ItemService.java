package com.ssscloud.auction.server.service;

import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.response.ItemDTO;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.server.dao.ItemDAO;
import com.ssscloud.auction.server.factory.ItemDTOFactory;

public class ItemService {
    public final Logger logger = Logger.getLogger(ItemService.class.getName());
    private final ItemDAO itemDAO;
    public ItemService (ItemDAO itemDAO){
        this.itemDAO = itemDAO;
    }
    public boolean saveItem(Item item) {
        return switch (item.getType()) {
            case "ART"        -> itemDAO.saveArt((com.ssscloud.auction.common.model.Art) item);
            case "VEHICLE"    -> itemDAO.saveVehicle((com.ssscloud.auction.common.model.Vehicle) item);
            case "ELECTRONIC" -> itemDAO.saveElectronic((com.ssscloud.auction.common.model.Electronic) item);
            default -> {
                logger.warning("Loại item không hợp lệ: " + item.getType());
                yield false; 
        }
        };
    }
    public ItemDTO getItemById (String id)
    {
        return ItemDTOFactory.toDTO(itemDAO.findById(id));
    }
}
