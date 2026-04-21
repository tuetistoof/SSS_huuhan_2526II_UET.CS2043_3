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
import com.ssscloud.auction.server.dao.AuctionDAO;

public class AuctionService {
    
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private final AuctionDAO auctionDAO;

    public AuctionService(AuctionDAO auctionDAO){
        this.auctionDAO = auctionDAO;
    }

    // Khởi tạo auction mới
    public AuctionDTO creatAuction(CreateAuctionRequest req, String sellerId){
        if (req.getTitle() == null || req.getTitle().isBlank())
            throw new IllegalArgumentException("Tiêu đề không được trống");
        if (req.getStartingPrice() <= 0)
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
        if (req.getEndTime() == null || req.getEndTime().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Thời gian kết thúc không hợp lệ");

        String id = UUID.randomUUID().toString();
        AuctionConfig config = new AuctionConfig(id, sellerId, (long) req.getStartingPrice(), calculateMinIncrement((long) req.getStartingPrice()), LocalDateTime.now(), req.getEndTime(), 60, req.getDescription());
        Auction auction = new Auction(config, null, sellerId, id); 

        activeAuctions.put(id, auction);

        System.out.println("[Auction Service] Tạo phiên: " + id + " | " + req.getTitle());
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

        activeAuctions.remove(auctionID);

        System.out.println("[Aution Service] Kết thúc phiên: " + auctionID + " | Người thắng: " + auction.getHighestBidderName() + " | Giá cuối: " + auction.getCurrentPrice());

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
