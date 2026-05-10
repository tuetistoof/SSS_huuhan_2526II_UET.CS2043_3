package com.ssscloud.auction.common.dto.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO để trả về thông tin item
 */
public class ItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String sellerId;
    private String creator;
    private String description;
    private String itemType;
    private List<String> imageUrls;

    public ItemDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    @Override
    public String toString() {
        return "ItemDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", itemType='" + itemType + '\'' +
                '}';
    }
}
