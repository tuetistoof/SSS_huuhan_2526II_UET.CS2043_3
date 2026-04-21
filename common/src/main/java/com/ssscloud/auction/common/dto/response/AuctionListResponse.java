package com.ssscloud.auction.common.dto.response;

import java.io.Serializable;
import java.util.List;

/**
 * DTO dùng để trả về danh sách các phiên đấu giá
 */
public class AuctionListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<AuctionDTO> auctions;      // Danh sách các phiên đấu giá
    private int totalPages;                 // Tổng số trang
    private int currentPage;                // Trang hiện tại
    private long totalElements;             // Tổng số phiên đấu giá
    private boolean hasNext;                // Có trang tiếp theo không

    public AuctionListResponse(List<AuctionDTO> auctions) {
        this.auctions = auctions;
    }

    // Constructor tiện ích
    public AuctionListResponse(List<AuctionDTO> auctions, int currentPage, int totalPages, long totalElements) {
        this.auctions = auctions;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.hasNext = currentPage < totalPages - 1;
    }

    // Getter & Setter
    public List<AuctionDTO> getAuctions() {
        return auctions;
    }

    public void setAuctions(List<AuctionDTO> auctions) {
        this.auctions = auctions;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    @Override
    public String toString() {
        return "AuctionListResponse{" +
                "totalElements=" + totalElements +
                ", currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                ", auctionsSize=" + (auctions != null ? auctions.size() : 0) +
                '}';
    }
}