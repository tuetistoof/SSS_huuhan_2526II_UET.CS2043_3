package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Request để tạo item mới
 */
public class CreateItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String description;
    private String itemType;  // ART, ELECTRONIC, VEHICLE
    private long basePrice;
    private LocalDate manufacturingDate;
    private String creator;
    private List<String> imageUrls;

    public CreateItemRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public long getBasePrice() { return basePrice; }
    public void setBasePrice(long basePrice) { this.basePrice = basePrice; }

    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    @Override
    public String toString() {
        return "CreateItemRequest{" +
                "title='" + title + '\'' +
                ", itemType='" + itemType + '\'' +
                ", basePrice=" + basePrice +
                ", creator='" + creator + '\'' +
                '}';
    }
}
