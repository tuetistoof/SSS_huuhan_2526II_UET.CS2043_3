package com.ssscloud.auction.server.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.ssscloud.auction.common.dto.request.CreateItemRequest;
import com.ssscloud.auction.common.dto.request.UpdateItemRequest;
import com.ssscloud.auction.common.dto.response.ItemDTO;
import com.ssscloud.auction.common.dto.response.ItemListResponse;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.enums.ItemStatus;
import com.ssscloud.auction.server.dao.ItemDAO;
import com.ssscloud.auction.server.service.ItemService;

/**
 * Controller xử lý các request liên quan đến items
 */
public class ItemController {
    private final ItemService itemService;
    private final ItemDAO itemDAO;

    public ItemController(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
        this.itemService = new ItemService(itemDAO);
    }

    /**
     * Tạo item mới
     * POST /items/create
     * @param req CreateItemRequest
     * @param sellerId ID của seller
     * @return ItemDTO
     */
    public ItemDTO createItem(CreateItemRequest req, String sellerId) {
        try {
            if (req.getTitle() == null || req.getTitle().isBlank()) {
                throw new IllegalArgumentException("Tiêu đề không được trống");
            }
            if (req.getItemType() == null || req.getItemType().isBlank()) {
                throw new IllegalArgumentException("Loại sản phẩm không được trống");
            }
            if (req.getBasePrice() <= 0) {
                throw new IllegalArgumentException("Giá sản phẩm phải lớn hơn 0");
            }

            Item item = itemService.createItem(req, sellerId, req.getItemType());
            return convertToDTO(item);
        } catch (Exception e) {
            System.err.println("[Item Controller] Lỗi tạo item: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Lấy danh sách items của seller
     * GET /items/seller/{sellerId}
     * @param sellerId ID của seller
     * @return ItemListResponse
     */
    public ItemListResponse getSellerItems(String sellerId) {
        try {
            List<Item> items = itemService.getItemsBySellerIdS(sellerId);
            List<ItemDTO> dtos = items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            return new ItemListResponse(dtos);
        } catch (Exception e) {
            System.err.println("[Item Controller] Lỗi lấy danh sách items: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Lấy danh sách items theo trạng thái
     * GET /items/seller/{sellerId}/status/{status}
     * @param sellerId ID của seller
     * @param status Trạng thái (DRAFT, AUCTIONING, SOLD, EXPIRED)
     * @return ItemListResponse
     */
    public ItemListResponse getItemsByStatus(String sellerId, String status) {
        try {
            ItemStatus itemStatus = ItemStatus.valueOf(status.toUpperCase());
            List<Item> items = itemService.getItemsByStatus(sellerId, itemStatus);
            List<ItemDTO> dtos = items.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            return new ItemListResponse(dtos);
        } catch (Exception e) {
            System.err.println("[Item Controller] Lỗi lấy items theo trạng thái: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Lấy thông tin chi tiết item
     * GET /items/{itemId}
     * @param itemId ID của item
     * @return ItemDTO
     */
    public ItemDTO getItem(String itemId) {
        try {
            Item item = itemService.getItem(itemId);
            if (item == null) {
                throw new IllegalArgumentException("Không tìm thấy item: " + itemId);
            }
            return convertToDTO(item);
        } catch (Exception e) {
            System.err.println("[Item Controller] Lỗi lấy item: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Cập nhật item (chỉ DRAFT hoặc EXPIRED)
     * PUT /items/{itemId}
     * @param itemId ID của item
     * @param req UpdateItemRequest
     * @return ItemDTO
     */
    public ItemDTO updateItem(String itemId, UpdateItemRequest req) {
        try {
            Item item = itemService.getItem(itemId);
            if (item == null) {
                throw new IllegalArgumentException("Không tìm thấy item: " + itemId);
            }

            // Cập nhật thông tin
            if (req.getTitle() != null && !req.getTitle().isBlank()) {
                item.setName(req.getTitle());
            }
            if (req.getDescription() != null) {
                item.setDescription(req.getDescription());
            }
            if (req.getBasePrice() > 0) {
                item.setBasePrice(req.getBasePrice());
            }
            if (req.getManufacturingDate() != null) {
                item.setManufacturingDate(req.getManufacturingDate());
            }
            if (req.getCreator() != null) {
                item.setCreator(req.getCreator());
            }

            item = itemService.updateItem(itemId, item);
            return convertToDTO(item);
        } catch (Exception e) {
            System.err.println("[Item Controller] Lỗi cập nhật item: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Xóa item (soft delete)
     * DELETE /items/{itemId}
     * @param itemId ID của item
     */
    public void deleteItem(String itemId) {
        try {
            itemService.deleteItem(itemId);
        } catch (Exception e) {
            System.err.println("[Item Controller] Lỗi xóa item: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Cập nhật trạng thái item
     * PUT /items/{itemId}/status
     * @param itemId ID của item
     * @param newStatus Trạng thái mới
     */
    public void updateItemStatus(String itemId, String newStatus) {
        try {
            ItemStatus status = ItemStatus.valueOf(newStatus.toUpperCase());
            itemService.updateItemStatus(itemId, status);
        } catch (Exception e) {
            System.err.println("[Item Controller] Lỗi cập nhật trạng thái item: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Kiểm tra xem item có thể sửa không
     * GET /items/{itemId}/can-edit
     * @param itemId ID của item
     * @return true nếu có thể sửa
     */
    public boolean canEditItem(String itemId) {
        return itemService.canEditItem(itemId);
    }

    /**
     * Kiểm tra xem item có thể mở đấu giá không
     * GET /items/{itemId}/can-auction
     * @param itemId ID của item
     * @return true nếu có thể mở đấu giá
     */
    public boolean canAuctionItem(String itemId) {
        return itemService.canAuctionItem(itemId);
    }

    /**
     * Chuyển đổi Item sang ItemDTO
     */
    private ItemDTO convertToDTO(Item item) {
        ItemDTO dto = new ItemDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setItemType(item.getType().name());
        dto.setBasePrice(item.getBasePrice());
        dto.setManufacturingDate(item.getManufacturingDate());
        dto.setCreator(item.getCreator());
        dto.setSellerId(item.getSellerId());
        dto.setStatus(item.getStatus().getDisplayName());
        dto.setImageUrls(item.getImageUrl());
        return dto;
    }
}
