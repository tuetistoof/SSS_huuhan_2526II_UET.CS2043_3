package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.dto.response.ItemDTO;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceExceptions;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuctionService manages the business logic for auction lifecycle operations.
 *
 * The createAuction execution flow:
 * 1. ItemFactory.create(request, sellerId) -> Item (Art/Vehicle/Electronic)
 * 2. ItemDAO.save*(item) -> Persist item and subtype records
 * 3. Construct AuctionConfig and initialize Auction with OPEN status
 * 4. AuctionDAO.saveAuction(auction) -> Persist auction record
 * 5. scheduleClose(auction) -> Initialize scheduler to transition OPEN to FINISHED status
 * 6. Return AuctionDTO.
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

    // --- PUBLIC METHODS ---

    public AuctionDTO createAuction(CreateAuctionRequest createAuctionRequest, String sellerId) throws ServiceExceptions {
        logger.log(Level.INFO, "Initiating auction creation process for sellerId: " + sellerId);
        
        validateCreateAuctionRequest(createAuctionRequest, sellerId); // Step 1: Validate input data

        Item item = createItemFromFactory(createAuctionRequest, sellerId); // Step 2: Create item using factory
        
        itemService.saveItem(item); // Step 3: Persist item data; throws ServiceExceptions if failed

        LocalDateTime startTime = (createAuctionRequest.getStartTime() != null)
                ? createAuctionRequest.getStartTime()
                : LocalDateTime.now();

        AuctionConfig auctionConfig = new AuctionConfig(
                createAuctionRequest.getName(),
                createAuctionRequest.getStartPrice(),
                createAuctionRequest.getMinIncrement(),
                startTime,
                createAuctionRequest.getEndTime(),
                36); // Default anti-sniping extension duration

        Auction auction = new Auction(auctionConfig, AuctionStatus.OPEN, sellerId, item.getId());
        
        boolean isAuctionSaved = auctionDAO.saveAuction(auction);
        if (!isAuctionSaved) {
            logger.log(Level.SEVERE, "Critical failure: Unable to persist auction record for name: " + auctionConfig.getName());
            throw new ServiceExceptions(ErrorCode.AUCTION_CREATION_FAILED, "Failed to persist the auction to the database: " + auctionConfig.getName());
        }

        AuctionRegistry.getInstance().register(auction); // Step 5: Register auction in the registry

        scheduleClose(auction); // Step 6: Schedule automatic closure
        logger.log(Level.INFO, "Auction successfully created and registered with ID: " + auctionConfig.getId());

        UserDTO sellerDto = userService.getByUserId(sellerId);
        ItemDTO itemDto = ItemDTOFactory.toDTO(item);
        
        return toAuctionDto(auction, sellerDto, itemDto);
    }

    public AuctionDTO getAuctionById(String auctionId) throws ServiceExceptions {
        logger.log(Level.INFO, "Retrieving auction details for auctionId: " + auctionId);
        validateAuctionId(auctionId);
        
        Auction auction = auctionDAO.findByAuctionId(auctionId);
        if (auction == null) {
            throw new ServiceExceptions(ErrorCode.AUCTION_NOT_FOUND, "Resource not found: The auction with ID " + auctionId + " does not exist.");
        }

        UserDTO sellerDto = userService.getByUserId(auction.getSellerId());
        ItemDTO itemDto = itemService.getItemById(auction.getItemId());
        
        if (itemDto == null) {
            throw new ServiceExceptions(ErrorCode.ITEM_NOT_FOUND, "Data integrity error: The item associated with auction " + auctionId + " was not found.");
        }

        return toAuctionDto(auction, sellerDto, itemDto);
    }

    public List<AuctionDisplayInfoDTO> getMyAuctions(String sellerId) throws ServiceExceptions {
        logger.log(Level.INFO, "Retrieving auction list for sellerId: " + sellerId);
        if (sellerId == null || sellerId.isBlank()) {
            throw new ServiceExceptions(ErrorCode.INVALID_DATA, "Operation failed: The seller identifier is mandatory to retrieve auctions.");
        }
        List<AuctionDisplayInfoDTO> myAuctionsList = auctionDAO.findSellerAuction(sellerId);
        return myAuctionsList;
    }

    public List<AuctionDisplayInfoDTO> getActiveAuctions() throws ServiceExceptions {
        logger.log(Level.INFO, "Retrieving all currently active auctions.");
        List<AuctionDisplayInfoDTO> activeAuctionsList = auctionDAO.findActiveAuctions();
        return activeAuctionsList;
    }

    public void scheduleClose(Auction auction) {
        String auctionId = auction.getAuctionConfig().getId();
        LocalDateTime endTime = auction.getAuctionConfig().getEndTime();
        
        long delayMilliseconds = Duration.between(LocalDateTime.now(), endTime).toMillis();
        if (delayMilliseconds < 0) {
            delayMilliseconds = 0;
        }

        scheduler.schedule(() -> {
            // Re-verify conclusion time in case anti-sniping has extended the duration
            if (LocalDateTime.now().isBefore(auction.getAuctionConfig().getEndTime())) {
                scheduleClose(auction); // Reschedule with updated end time
                return;
            }

            AuctionStatus currentStatus = auction.getStatus();
            if (currentStatus == AuctionStatus.OPEN || currentStatus == AuctionStatus.RUNNING) {
                auction.finish();
                auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
                AuctionRegistry.getInstance().remove(auctionId);
                ConcurrentBidManager.getInstance().shutdown(auctionId); 
                ChangeManager.getInstance().notify(auction);
                NotificationService.getInstance().notifyAuctionEnded(auction);
                logger.log(Level.INFO, "Auction has been automatically concluded: " + auctionId);
            }
        }, delayMilliseconds, TimeUnit.MILLISECONDS);
    }

    // --- PRIVATE HELPERS ---

    private AuctionDTO toAuctionDto(Auction auction, UserDTO sellerDto, ItemDTO itemDto) {
        AuctionDTO auctionDto = new AuctionDTO();

        auctionDto.setId(auction.getAuctionConfig().getId());
        auctionDto.setName(auction.getAuctionConfig().getName());
        auctionDto.setMinIncrement(auction.getAuctionConfig().getMinIncrement());
        auctionDto.setStartTime(auction.getAuctionConfig().getStartTime());
        auctionDto.setEndTime(auction.getAuctionConfig().getEndTime());
        auctionDto.setStatus(auction.getStatus());

        auctionDto.setUserDTO(sellerDto);
        auctionDto.setItemDTO(itemDto);

        auctionDto.setCurrentPrice(auction.getCurrentPrice());
        auctionDto.setHighestBidderName(auction.getHighestBidderName());
        auctionDto.setBidCount(auction.getBidCount());

        return auctionDto;
    }

    private void validateCreateAuctionRequest(CreateAuctionRequest createAuctionRequest, String sellerId) throws ServiceExceptions {
        if (createAuctionRequest == null) {
            throw new ServiceExceptions(ErrorCode.INVALID_DATA, "The auction request payload cannot be null.");
        }
        if (sellerId == null || sellerId.isBlank()) {
            throw new ServiceExceptions(ErrorCode.MISSING_BIDDER_ID, "The seller identifier is required for auction creation.");
        }
    }

    private void validateAuctionId(String auctionId) throws ServiceExceptions {
        if (auctionId == null || auctionId.isBlank()) {
            throw new ServiceExceptions(ErrorCode.MISSING_AUCTION_ID, "The auction identification is required to perform this operation.");
        }
    }

    private Item createItemFromFactory(CreateAuctionRequest createAuctionRequest, String sellerId) throws ServiceExceptions {
        try {
            return ItemFactory.createItem(createAuctionRequest, sellerId);
        } catch (IllegalArgumentException illegalArgumentException) {
            logger.log(Level.SEVERE, "ItemFactory encountered a creation failure: " + illegalArgumentException.getMessage(), illegalArgumentException);
            throw new ServiceExceptions(ErrorCode.INVALID_ITEM_DATA, "Validation failure: The provided item data is invalid: " + illegalArgumentException.getMessage());
        }
    }
}
