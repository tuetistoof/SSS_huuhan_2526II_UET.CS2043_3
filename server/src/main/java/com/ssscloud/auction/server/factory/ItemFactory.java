package com.ssscloud.auction.server.factory;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.model.Electronic;
import com.ssscloud.auction.common.model.Art;
import com.ssscloud.auction.common.model.Vehicle;
import com.ssscloud.auction.common.enums.ItemType;
import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.exception.NotFoundException;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class ItemFactory {
    private static final Logger logger = Logger.getLogger(ItemFactory.class.getName());
    
    private static final Map<ItemType, Supplier<? extends Item>> registry = new HashMap<>();
    private static final Map<String, ItemType> typeMapping = new HashMap<>();
    
    static {
        // Đăng ký các loại item
        registry.put(ItemType.ART, Art::new);
        registry.put(ItemType.ELECTRONIC, Electronic::new);
        registry.put(ItemType.VEHICLE, Vehicle::new);
        
        // Tạo mapping từ String sang ItemType
        for (ItemType type : ItemType.values()) {
            typeMapping.put(type.name(), type);
            typeMapping.put(type.name().toLowerCase(), type);
        }
    }

    /**
     * Đăng ký loại item mới (hỗ trợ mở rộng tại runtime)
     * @param type Loại item cần đăng ký
     * @param supplier Supplier để tạo instance
     */
    public static void register(ItemType type, Supplier<? extends Item> supplier) {
        if (type == null || supplier == null) {
            throw new IllegalArgumentException("Type và supplier không thể null");
        }
        registry.put(type, supplier);
        typeMapping.put(type.name(), type);
        typeMapping.put(type.name().toLowerCase(), type);
        logger.info("Đã đăng ký loại item: " + type.name());
    }

    /**
     * Tạo item dựa trên ItemType enum
     * @param type Loại item cần tạo
     * @return Item instance mới
     * @throws NotFoundException nếu loại item không tồn tại
     */
    public static Item createItem(ItemType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type không thể null");
        }
        
        Supplier<? extends Item> supplier = registry.get(type);
        if (supplier == null) {
            throw new NotFoundException("Loại item không được hỗ trợ: " + type.name());
        }
        
        logger.fine("Tạo item loại: " + type.name());
        return supplier.get();
    }

    /**
     * Tạo item dựa trên String (để hỗ trợ tương thích ngược)
     * @param typeString Tên loại item dưới dạng String
     * @return Item instance mới
     * @throws NotFoundException nếu loại item không tồn tại
     */
    public static Item createItem(String typeString) {
        if (typeString == null || typeString.trim().isEmpty()) {
            throw new IllegalArgumentException("Type string không thể null hoặc rỗng");
        }
        
        ItemType type = parseItemType(typeString);
        return createItem(type);
    }

    /**
     * Tạo item từ CreateAuctionRequest
     * @param request Request chứa thông tin item
     * @param typeString Loại item
     * @return Item instance được populate từ request
     */
    public static Item createItem(CreateAuctionRequest request, String typeString) {
        if (request == null) {
            throw new IllegalArgumentException("Request không thể null");
        }
        
        Item item = createItem(typeString);
        
        // Populate dữ liệu từ request
        if (request.getTitle() != null) {
            item.setName(request.getTitle());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        
        logger.fine("Tạo item từ request: " + item.getName());
        return item;
    }

    /**
     * Kiểm tra xem loại item có được hỗ trợ không
     * @param typeString Tên loại item
     * @return true nếu loại item được hỗ trợ
     */
    public static boolean isSupported(String typeString) {
        if (typeString == null || typeString.trim().isEmpty()) {
            return false;
        }
        ItemType type = typeMapping.get(typeString.toUpperCase().trim());
        return type != null && registry.containsKey(type);
    }

    /**
     * Lấy danh sách tất cả các loại item được hỗ trợ
     * @return Set chứa tên các loại item
     */
    public static Set<String> getSupportedTypes() {
        Set<String> types = new HashSet<>();
        for (ItemType type : registry.keySet()) {
            types.add(type.name());
        }
        return Collections.unmodifiableSet(types);
    }

    /**
     * Chuyển đổi String sang ItemType enum
     * @param typeString Tên loại item
     * @return ItemType tương ứng
     * @throws NotFoundException nếu không tìm thấy loại
     */
    private static ItemType parseItemType(String typeString) {
        String normalized = typeString.toUpperCase().trim();
        ItemType type = typeMapping.get(normalized);
        
        if (type == null) {
            throw new NotFoundException(
                String.format("Loại item '%s' không tồn tại. Các loại được hỗ trợ: %s", 
                    typeString, getSupportedTypes())
            );
        }
        
        return type;
    }
}
