package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.dto.response.ItemDTO;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.factory.ItemDTOFactory;
import com.ssscloud.auction.server.factory.ItemFactory;
import com.ssscloud.auction.server.util.AuctionRegistry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * AuctionService — business logic tạo phiên đấu giá.
 *
 * Luồng createAuction():
 * 1. ItemFactory.create(request, sellerId) → Item (Art/Vehicle/Electronic)
 * 2. ItemDAO.save*(item) → persist item + subtype table
 * 3. Build AuctionConfig + Auction(OPEN)
 * 4. AuctionDAO.saveAuction(auction) → persist
 * 5. scheduleClose(auction) → Timer tự đổi OPEN→FINISHED lúc endTime
 * 6. Return AuctionDTO
 */
public class AuctionService {

    private static final Logger logger = Logger.getLogger(AuctionService.class.getName());

    private final AuctionDAO auctionDAO;
    private final UserService userService;
    private final ItemService itemService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public AuctionService(AuctionDAO auctionDAO, UserService userService, ItemService itemService) {
        this.auctionDAO = auctionDAO;
        this.userService = userService;
        this.itemService = itemService;
    }

    public AuctionDTO createAuction(CreateAuctionRequest request, String sellerId) {
        // tạo item bằng factory
        Item item;
        try {
            item = ItemFactory.createItem(request, sellerId);
        } catch (IllegalArgumentException e) {
            logger.warning("ItemFactory lỗi: " + e.getMessage());
            return null;
        }
        boolean itemSaved = itemService.saveItem(item);
        if (!itemSaved) {
            logger.severe("Không lưu được item: " + item.getName());
            return null;
        }
        LocalDateTime startTime = (request.getStartTime() != null)
                ? request.getStartTime()
                : LocalDateTime.now();

        AuctionConfig config = new AuctionConfig(
                request.getName(),
                request.getStartPrice(),
                request.getMinIncrement(),
                startTime,
                request.getEndTime(),
                36);

        Auction auction = new Auction(config, AuctionStatus.OPEN, sellerId, item.getId());
        boolean auctionSaved = auctionDAO.saveAuction(auction);
        if (!auctionSaved) {
            logger.severe("Không lưu được auction: " + config.getName());
            return null;
        }
        com.ssscloud.auction.server.util.AuctionRegistry.getInstance().register(auction);

        scheduleClose(auction);

        logger.info("Tạo auction thành công: " + config.getId() + " - " + config.getName());

        return toDTO(auction, userService.getByUserId(sellerId), ItemDTOFactory.toDTO(item));
    }

    // TO_DOS: sau này sẽ bổ sung chức năng lấy danh sách phiên đấu giá

    // public List<AuctionDTO> getAllAuctions() {
    // return auctionDAO.findAll().stream()
    // .map(this::toDTO)
    // .collect(Collectors.toList());
    // }

    public AuctionDTO getAuctionById(String auctionId) {
        Auction auction = auctionDAO.findByAuctionId(auctionId);
        if (auction == null) throw new InvalidBidException("Auction không tồn tại: " + auctionId);
        UserDTO userDTO = userService.getByUserId(auction.getSellerId());
        ItemDTO itemDTO = itemService.getItemById(auction.getItemId());
        if (itemDTO == null) throw new InvalidBidException("Item không tồn tại: " + auction.getItemId());
        return toDTO(auction, userDTO, itemDTO);
    }

    public List<AuctionDisplayInfoDTO> getMyAuctions(String sellerId) {
        return auctionDAO.findSellerAuction(sellerId);
    }

    public List<AuctionDisplayInfoDTO> getActiveAuctions() {
        return auctionDAO.findActiveAuctions();
    }

    // HELPERS

    private AuctionDTO toDTO(Auction auction, UserDTO userDTO, ItemDTO itemDTO) {
        AuctionDTO auctionDTO = new AuctionDTO();

        auctionDTO.setId(auction.getAuctionConfig().getId());
        auctionDTO.setName(auction.getAuctionConfig().getName());
        auctionDTO.setMinIncrement(auction.getAuctionConfig().getMinIncrement());
        auctionDTO.setStartTime(auction.getAuctionConfig().getStartTime());
        auctionDTO.setEndTime(auction.getAuctionConfig().getEndTime());
        auctionDTO.setStatus(auction.getStatus());

        auctionDTO.setUserDTO(userDTO);
        auctionDTO.setItemDTO(itemDTO);

        auctionDTO.setCurrentPrice(auction.getCurrentPrice());
        auctionDTO.setHighestBidderName(auction.getHighestBidderName());
        auctionDTO.setBidCount(auction.getBidCount());

        return auctionDTO;
    }


    public void scheduleClose(Auction auction) {
        LocalDateTime end = auction.getAuctionConfig().getEndTime();
        long delayMs = Duration.between(LocalDateTime.now(), end).toMillis();
        if (delayMs < 0) delayMs = 0;
        String auctionId = auction.getAuctionConfig().getId();

        scheduler.schedule(() -> {
            // Kiểm tra lại — có thể anti-sniping đã gia hạn sau khi schedule
            if (LocalDateTime.now().isBefore(auction.getAuctionConfig().getEndTime())) {
                scheduleClose(auction); // reschedule với endTime mới
                return;
            }
            AuctionStatus current = auction.getStatus();
            if (current == AuctionStatus.OPEN || current == AuctionStatus.RUNNING) {
                auction.finish();
                auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
                AuctionRegistry.getInstance().remove(auctionId);
                ConcurrentBidManager.getInstance().shutdown(auctionId); 
                ChangeManager.getInstance().notify(auction);
                NotificationService.getInstance().notifyAuctionEnded(auction);
                logger.info("scheduleClose: đóng auction " + auctionId);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

}
