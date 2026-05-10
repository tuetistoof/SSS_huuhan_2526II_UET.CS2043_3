package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.dto.response.ItemDTO;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.model.base.Item;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.ItemDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.factory.ItemDTOFactory;
import com.ssscloud.auction.server.factory.ItemFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        UserDTO userDTO = userService.getByUserId(auction.getSellerId());
        ItemDTO itemDTO = itemService.getItemById(auction.getItemId());
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

    private void scheduleClose(Auction auction) {
        Thread closer = new Thread(() -> {
            while (true) {
                try {
                    LocalDateTime end = auction.getAuctionConfig().getEndTime();
                    long remaining = Duration.between(LocalDateTime.now(), end).toMillis();

                    if (remaining <= 0) {
                        AuctionStatus current = auction.getStatus();
                        if (current == AuctionStatus.OPEN || current == AuctionStatus.RUNNING) {
                            auction.finish();
                            auctionDAO.updateStatus(auction.getAuctionConfig().getId(), AuctionStatus.FINISHED);
                            AuctionRegistry.getInstance().remove(auction.getAuctionConfig().getId());
                            ChangeManager.getInstance().notify(auction);
                            logger.info("scheduleClose: đóng auction " + auction.getAuctionConfig().getId());
                        }
                        break;
                    }

                    Thread.sleep(Math.min(remaining, 1000)); // ngủ tối đa 1s để check lại, tránh sleep quá lâu khi có
                                                             // antisnipping

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.severe("scheduleClose lỗi: " + e.getMessage());
                    break;
                }
            }

        });
        closer.setDaemon(true);
        closer.setName("auction-closer-" + auction.getAuctionConfig().getId());
        closer.start();
    }
}
