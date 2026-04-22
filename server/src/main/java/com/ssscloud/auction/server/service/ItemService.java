package com.ssscloud.auction.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.enums.ItemStatus;
import com.ssscloud.auction.common.enums.ItemType;
import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.server.dao.ItemDAO;
import com.ssscloud.auction.server.factory.ItemFactory;

/**
 * Service quản lý items của sellers
 * Hỗ trợ CRUD và quản lý trạng thái items
 */
public class ItemService {
    private final ItemDAO itemDAO = new ItemDAO();
    private final Map<String, Item> itemCache = new ConcurrentHashMap<>();


    /**
     * Tạo item mới cho seller
     * @param req Request chứa thông tin item
     * @param sellerId ID của seller
     * @param itemType Loại item (ART, ELECTRONIC, VEHICLE)
     * @return Item vừa tạo
     */
    public Item createItem(CreateAuctionRequest req, String sellerId, ItemType itemType) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tiêu đề không được trống");
        }
        if (itemType == null) {
            throw new IllegalArgumentException("Loại sản phẩm không được trống");
        }

        // Tạo item bằng ItemFactory
        Item item = ItemFactory.createItem(req, itemType);
        String itemId = UUID.randomUUID().toString();
        item.setId(itemId);
        item.setSellerId(sellerId);
        item.setStatus(ItemStatus.DRAFT);  // Trạng thái ban đầu là DRAFT

        // Lưu vào cache
        itemCache.put(itemId, item);

        // Lưu vào database
        if (itemDAO != null) {
            itemDAO.save(item, itemType);
        }

        System.out.println("[Item Service] Tạo item mới: " + itemId + 
                         " | Seller: " + sellerId + 
                         " | Tên: " + item.getName() + 
                         " | Loại: " + itemType);

        return item;
    }

    /**
     * Lấy item theo ID
     * @param itemId ID của item
     * @return Item tương ứng hoặc null nếu không tìm thấy
     */
    public Item getItem(String itemId) {
        Item item = itemCache.get(itemId);
        if (item == null && itemDAO != null) {
            item = itemDAO.findById(itemId);
            if (item != null) {
                itemCache.put(itemId, item);
            }
        }
        return item;
    }

    /**
     * Lấy danh sách items của seller
     * @param sellerId ID của seller
     * @return List items của seller
     */
    public List<Item> getItemsBySellerIdS(String sellerId) {
        List<Item> items = new ArrayList<>();
        
        // Từ cache trước
        items.addAll(
            itemCache.values().stream()
                .filter(item -> sellerId.equals(item.getSellerId()))
                .collect(Collectors.toList())
        );

        // Từ database nếu có
        if (itemDAO != null) {
            List<Item> dbItems = itemDAO.findBySellerId(sellerId);
            for (Item item : dbItems) {
                if (!itemCache.containsKey(item.getId())) {
                    items.add(item);
                    itemCache.put(item.getId(), item);
                }
            }
        }

        return items;
    }

    /**
     * Lấy danh sách items theo trạng thái
     * @param sellerId ID của seller
     * @param status Trạng thái cần lọc
     * @return List items có trạng thái tương ứng
     */
    public List<Item> getItemsByStatus(String sellerId, ItemStatus status) {
        return itemDAO.findBySellerId(sellerId).stream()
            .filter(item -> status == item.getStatus())
            .collect(Collectors.toList());
    }

    /**
     * Cập nhật item
     * Chỉ cho phép cập nhật items ở trạng thái DRAFT hoặc EXPIRED
     * @param itemId ID của item
     * @param updatedItem Item chứa dữ liệu cập nhật
     * @return Item sau khi cập nhật
     * @throws IllegalArgumentException nếu item không ở trạng thái cho phép sửa
     */
    public Item updateItem(String itemId, Item updatedItem) {
        Item item = getItem(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Không tìm thấy item: " + itemId);
        }

        // Chỉ cho phép sửa khi DRAFT hoặc EXPIRED
        if (!item.getStatus().isEditable()) {
            throw new IllegalArgumentException(
                "Không thể sửa item ở trạng thái: " + item.getStatus().getDisplayName()
            );
        }

        // Cập nhật thông tin
        item.setName(updatedItem.getName());
        item.setDescription(updatedItem.getDescription());
        item.setBasePrice(updatedItem.getBasePrice());
        item.setManufacturingDate(updatedItem.getManufacturingDate());
        item.setCreator(updatedItem.getCreator());

        // Lưu vào database
        if (itemDAO != null) {
            itemDAO.update(item);
        }

        System.out.println("[Item Service] Cập nhật item: " + itemId);
        return item;
    }

    /**
     * Cập nhật trạng thái item
     * @param itemId ID của item
     * @param newStatus Trạng thái mới
     */
    public void updateItemStatus(String itemId, ItemStatus newStatus) {
        Item item = getItem(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Không tìm thấy item: " + itemId);
        }

        ItemStatus oldStatus = item.getStatus();
        item.setStatus(newStatus);

        // Lưu vào database
        if (itemDAO != null) {
            itemDAO.update(item);
        }

        System.out.println("[Item Service] Cập nhật trạng thái item: " + itemId + 
                         " từ " + oldStatus.getDisplayName() + 
                         " sang " + newStatus.getDisplayName());
    }

    /**
     * Xóa item (soft delete - chỉ đánh dấu DELETED)
     * @param itemId ID của item
     */
    public void deleteItem(String itemId) {
        updateItemStatus(itemId, ItemStatus.DELETED);
        System.out.println("[Item Service] Xóa item (soft delete): " + itemId);
    }

    /**
     * Lấy item để mở đấu giá (kiểm tra điều kiện)
     * Item phải ở trạng thái DRAFT hoặc EXPIRED để mở đấu giá
     * @param itemId ID của item
     * @return Item nếu có thể mở đấu giá
     */
    public Item getItemForAuction(String itemId) {
        Item item = getItem(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Không tìm thấy item: " + itemId);
        }

        if (!item.getStatus().isEditable()) {
            throw new IllegalArgumentException(
                "Chỉ có thể mở đấu giá cho items ở trạng thái DRAFT hoặc EXPIRED, " +
                "item này ở trạng thái: " + item.getStatus().getDisplayName()
            );
        }

        return item;
    }

    /**
     * Kiểm tra xem item có thể được sửa không
     * @param itemId ID của item
     * @return true nếu item có thể sửa
     */
    public boolean canEditItem(String itemId) {
        Item item = getItem(itemId);
        return item != null && item.getStatus().isEditable();
    }

    /**
     * Kiểm tra xem item có thể mở đấu giá không
     * @param itemId ID của item
     * @return true nếu item có thể mở đấu giá
     */
    public boolean canAuctionItem(String itemId) {
        Item item = getItem(itemId);
        return item != null && (item.getStatus() == ItemStatus.DRAFT || item.getStatus() == ItemStatus.EXPIRED);
    }
}
