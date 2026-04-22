package com.ssscloud.auction.server.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionListResponse;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.enums.ItemStatus;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.service.ItemService;

public class AuctionService {
    
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private final AuctionDAO auctionDAO;
    private final ItemService itemService;

    public AuctionService(AuctionDAO auctionDAO, ItemService itemService){
        this.auctionDAO = auctionDAO;
        this.itemService = itemService;
    }

    // Khởi tạo auction mới từ item đã tạo
    public AuctionDTO creatAuction(CreateAuctionRequest req, String sellerId){
        if (req.getTitle() == null || req.getTitle().isBlank())
            throw new IllegalArgumentException("Tiêu đề không được trống");
        if (req.getStartingPrice() <= 0)
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
        if (req.getEndTime() == null || req.getEndTime().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Thời gian kết thúc không hợp lệ");
        if (req.getItemId() == null || req.getItemId().isBlank())
            throw new IllegalArgumentException("ItemId không được trống");

        // Lấy item từ ItemService
        Item item = itemService.getItem(req.getItemId());
        if (item == null) {
            throw new IllegalArgumentException("Không tìm thấy item: " + req.getItemId());
        }

        // Kiểm tra item có thể mở đấu giá không (phải ở DRAFT hoặc EXPIRED)
        if (!itemService.canAuctionItem(req.getItemId())) {
            throw new IllegalArgumentException(
                "Chỉ có thể mở đấu giá cho items ở trạng thái DRAFT hoặc EXPIRED, " +
                "item này ở trạng thái: " + item.getStatus().getDisplayName()
            );
        }

        // Tạo ID cho auction
        String auctionId = UUID.randomUUID().toString();

        // Tạo AuctionConfig
        AuctionConfig config = new AuctionConfig(
            auctionId,
            req.getTitle(),
            req.getStartingPrice(),
            req.getMinIncrement() > 0 ? req.getMinIncrement() : calculateMinIncrement(req.getStartingPrice()),
            LocalDateTime.now(),
            req.getEndTime(),
            60,
            req.getDescription()
        );
        
        // Tạo Auction
        Auction auction = new Auction(config, null, sellerId, req.getItemId());

        // Cập nhật status item thành AUCTIONING
        itemService.updateItemStatus(req.getItemId(), ItemStatus.AUCTIONING);

        // Lưu vào trong-memory store
        activeAuctions.put(auctionId, auction);

        System.out.println("[Auction Service] Tạo phiên: " + auctionId + 
                         " | Sản phẩm: " + item.getName() + 
                         " | Loại: " + item.getType().name() +
                         " | Item ID: " + req.getItemId());
        
        return toDTO(auction);
    }

    //Tìm kiếm theo Id;
    public Auction getActiveAuctions(String auctionID) {
        return activeAuctions.get(auctionID);
    }

    //Danh sách các phiên đang hoạt động dưới dạng DTO
    public AuctionListResponse getAllActiveAuctions(){
        List<AuctionDTO> list = new ArrayList<>();
        for (Auction a: activeAuctions.values()){
            list.add(toDTO(a));
        }

        return new AuctionListResponse(list);
    }

    //Bắt đầu phiên
    public void startAuction(String auctionID){
        Auction auction = requireAuction(auctionID);
        auction.start();
        System.out.println("[Auction Service] Bắt đầu phiên: " + auctionID);
    }

    //Kết thúc phiên
    public void endAuction(String auctionID){
        Auction auction = requireAuction(auctionID);
        auction.finish();

        ConcurrentBidManager.getInstance().removeLock(auctionID);

        // Cập nhật status item dựa trên kết quả đấu giá
        String itemId = auction.getItemId();
        ItemStatus newStatus;
        
        if (auction.getHighestBidderName() != null && !auction.getHighestBidderName().isEmpty()) {
            // Có người thắng → SOLD
            newStatus = ItemStatus.SOLD;
        } else {
            // Chưa có lượt đấu → EXPIRED
            newStatus = ItemStatus.EXPIRED;
        }
        
        itemService.updateItemStatus(itemId, newStatus);

        activeAuctions.remove(auctionID);

        System.out.println("[Auction Service] Kết thúc phiên: " + auctionID + 
                         " | Người thắng: " + auction.getHighestBidderName() + 
                         " | Giá cuối: " + auction.getCurrentPrice() +
                         " | Cập nhật item status: " + newStatus.getDisplayName());

    }

    public Auction requireAuction(String auctionID){
        Auction auction = activeAuctions.get(auctionID);
        if (auction == null)
            throw new IllegalArgumentException("Không tìm thấy phiên: " + auctionID);
        return auction;
    }
    //Tự động tính bước giá = 1% của giá khởi điẻm, giá sàn = 10000đ
    private long calculateMinIncrement(long startPrice) {
        long increment = startPrice / 100;
        return Math.max(increment, 10_000L);
    }

    private AuctionDTO toDTO(Auction a) {
        AuctionDTO dto = new AuctionDTO();
        AuctionConfig cfg = a.getAuctionConfig();
        dto.setId(cfg.getId());
        dto.setName(cfg.getName());
        dto.setDescription(cfg.getDescription());
        dto.setStartingPrice(cfg.getStartPrice());
        dto.setCurrentPrice(a.getCurrentPrice());
        dto.setEndTime(cfg.getEndTime());
        dto.setStatus(a.getStatus());
        dto.setHighestBidderName(a.getHighestBidderName());
        dto.setBidCount(a.getBidTransaction().size());
        dto.setMinIncrement(cfg.getMinIncrement());
        return dto;
    }

}
