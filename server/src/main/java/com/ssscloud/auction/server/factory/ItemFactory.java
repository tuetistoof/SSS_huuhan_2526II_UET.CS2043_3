package com.ssscloud.auction.server.factory;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.model.Electronic;
import com.ssscloud.auction.common.model.Art;
import com.ssscloud.auction.common.model.Vehicle;


import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
public class ItemFactory {
    private static final Map<String, Supplier<? extends Item>> registry = new HashMap<>();
    static {
        registry.put("ART", Art::new);
        registry.put("ELECTRONIC", Electronic::new);
        registry.put("VEHICLE", Vehicle::new);
    }
    //hỗ trợ sau này muốn thêm 1 category nào nữa, thì dùng register()
    public static void register(String type, Supplier<? extends Item> supplier){
        registry.put(type.toUpperCase().trim(), supplier);
    }

    public static Item createItem(String type){
        return registry.get(type).get();
    }
    public static Item createItem(CreateAuctionRequest request, String type){
        Item item = createItem(type);

        item.setName(request.getTitle());
        item.setDescription(request.getDescription());
        //item.setStartingPrice(request.getStartingPrice());
        return item;
    }
}
