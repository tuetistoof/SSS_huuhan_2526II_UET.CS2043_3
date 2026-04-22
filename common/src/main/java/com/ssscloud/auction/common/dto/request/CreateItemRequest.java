package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;
import java.util.List;


/**
 * Request để tạo item mới
 */
public class CreateItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private String itemType;  // ART, ELECTRONIC, VEHICLE
    private String creator;
    private List<String> imageUrls;

    public CreateItemRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    @Override
    public String toString() {
        return "CreateItemRequest{" +
                "name='" + name + '\'' +
                ", itemType='" + itemType + '\'' +
                ", creator='" + creator + '\'' +
                '}';
    }
}
