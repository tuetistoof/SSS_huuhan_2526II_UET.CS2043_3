package com.ssscloud.auction.common.dto.request;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Request để cập nhật item (chỉ cho DRAFT hoặc EXPIRED)
 */
public class UpdateItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String description;
    private long basePrice;
    private LocalDate manufacturingDate;
    private String creator;
    private List<String> imageUrls;

    public UpdateItemRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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
        return "UpdateItemRequest{" +
                "title='" + title + '\'' +
                ", basePrice=" + basePrice +
                '}';
    }
}
