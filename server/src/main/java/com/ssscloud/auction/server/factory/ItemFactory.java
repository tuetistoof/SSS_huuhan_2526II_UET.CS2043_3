package com.ssscloud.auction.server.factory;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.request.ItemData;
import com.ssscloud.auction.common.model.Art;
import com.ssscloud.auction.common.model.Electronic;
import com.ssscloud.auction.common.model.Vehicle;
import com.ssscloud.auction.common.model.base.Item;

public class ItemFactory {

    public static Item createItem(CreateAuctionRequest request, String sellerId) {
        if (request.getItemData() == null) {
            throw new IllegalArgumentException("Dữ liệu item không được để trống");
        }
        ItemData data = request.getItemData();

        // ưu tiên lấy từ data, nếu data trống thì lấy từ request
        String itemName = data.getName();
        if (itemName == null || itemName.isBlank()) {
            itemName = request.getName();
        }

        String rawType = data.getItemType();
        String type = (rawType != null) ? rawType.trim().toUpperCase() : "";
        Item item = switch (type) {
            case "ART" -> {
                Art art = new Art();
                art.setCertificate(data.isHasCertificate());
                yield art;
            }
            case "VEHICLE" -> {
                Vehicle vehicle = new Vehicle();
                vehicle.setIsRepaired(data.isRepaired());
                vehicle.setWarrantyPeriod(data.getWarrantyPeriod());
                yield vehicle;
            }
            case "ELECTRONIC" -> {
                Electronic electronic = new Electronic();
                electronic.setIsRepaired(data.isRepaired());
                electronic.setWarrantyPeriod(data.getWarrantyPeriod());
                yield electronic;
            }
            default -> throw new IllegalArgumentException("Loại item không hợp lệ: " + type);
        };

        item.setName(itemName);
        item.setDescription(data.getDescription());
        item.setCreator(data.getCreator());
        item.setSellerId(sellerId);

        if (data.getImageUrls() != null) {
            data.getImageUrls().forEach(item::addImage);
        }
        return item;
    }

}
