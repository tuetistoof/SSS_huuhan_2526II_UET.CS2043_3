package com.ssscloud.auction.server.factory;

import com.ssscloud.auction.common.dto.response.ArtDTO;
import com.ssscloud.auction.common.dto.response.ElectricDTO;
import com.ssscloud.auction.common.dto.response.ItemDTO;
import com.ssscloud.auction.common.dto.response.VehicleDTO;
import com.ssscloud.auction.common.model.Art;
import com.ssscloud.auction.common.model.Electronic;
import com.ssscloud.auction.common.model.Vehicle;
import com.ssscloud.auction.common.model.base.Item;

public class ItemDTOFactory {

    public static ItemDTO toDTO(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item không được để trống");
        }

        ItemDTO dto = switch (item) {
            case Art art -> {
                ArtDTO artDTO = new ArtDTO();
                artDTO.setCertificate(art.getCertificate());
                yield artDTO;
            }
            case Vehicle vehicle -> {
                VehicleDTO vehicleDTO = new VehicleDTO();
                vehicleDTO.setIsRepaired(vehicle.getIsRepaired());
                vehicleDTO.setWarrantyPeriod(vehicle.getWarrantyPeriod());
                yield vehicleDTO;
            }
            case Electronic electronic -> {
                ElectricDTO electricDTO = new ElectricDTO();
                electricDTO.setIsRepaired(electronic.getIsRepaired());
                electricDTO.setWarrantyPeriod(electronic.getWarrantyPeriod());
                yield electricDTO;
            }
            default -> throw new IllegalArgumentException("Loại item không hợp lệ: " + item.getClass().getSimpleName());
        };

        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setSellerId(item.getSellerId());
        dto.setCreator(item.getCreator());
        dto.setDescription(item.getDescription());
        dto.setItemType(item.getType());
        dto.setImageUrls(item.getImageUrl());

        return dto;
    }

    /**
     * Cast một ItemDTO (đã là subclass) về đúng DTO con tương ứng dựa vào itemType.
     * Dùng khi nhận được ItemDTO từ network/deserialization nhưng cần thao tác với field riêng.
     */
    public static ItemDTO castToSubDTO(ItemDTO itemDTO) {
        if (itemDTO == null) {
            throw new IllegalArgumentException("ItemDTO không được để trống");
        }

        if (itemDTO instanceof ArtDTO || itemDTO instanceof VehicleDTO || itemDTO instanceof ElectricDTO) {
            return itemDTO;
        }

        String type = itemDTO.getItemType();
        if (type == null) {
            throw new IllegalArgumentException("itemType không được để trống");
        }

        ItemDTO subDTO = switch (type.trim().toUpperCase()) {
            case "ART" -> new ArtDTO();
            case "VEHICLE" -> new VehicleDTO();
            case "ELECTRONIC" -> new ElectricDTO();
            default -> throw new IllegalArgumentException("Loại item không hợp lệ: " + type);
        };

        subDTO.setId(itemDTO.getId());
        subDTO.setName(itemDTO.getName());
        subDTO.setSellerId(itemDTO.getSellerId());
        subDTO.setCreator(itemDTO.getCreator());
        subDTO.setDescription(itemDTO.getDescription());
        subDTO.setItemType(itemDTO.getItemType());
        subDTO.setImageUrls(itemDTO.getImageUrls());

        return subDTO;
    }
}