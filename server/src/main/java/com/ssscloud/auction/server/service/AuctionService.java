package com.ssscloud.auction.server.service;

import com.ssscloud.auction.common.dto.request.CreateAuctionRequest;
import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.BidderDisplayDTO;
import com.ssscloud.auction.common.dto.response.ItemDTO;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
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
    private static final Logger logger = Logger.getLogger(AuctionService.class.getName()); // Logging Standards: Declared first

    private final AuctionDAO auctionDAO;
    private final UserService userService;
    private final ItemService itemService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final NotificationService notificationService;


    public AuctionService(AuctionDAO auctionDAO, UserService userService, ItemService itemService, NotificationService notificationService) {
        this.auctionDAO = auctionDAO;
        this.userService = userService;
        this.itemService = itemService;
        this.notificationService = notificationService;
    }

    // --- PUBLIC METHODS ---

    public AuctionDTO createAuction(CreateAuctionRequest request, String sellerId) throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Initiating auction creation process for sellerId: " + sellerId);
            
            validateCreateAuctionRequest(request, sellerId); // Step 1: Validate input data
    
            Item item = createItemFromFactory(request, sellerId); // Step 2: Create item using factory
            
            itemService.saveItem(item); // Step 3: Persist item data; throws ServiceExceptions if failed
    
            LocalDateTime startTime = (request.getStartTime() != null)
                    ? request.getStartTime()
                    : LocalDateTime.now();
    
            AuctionConfig auctionConfig = new AuctionConfig(
                    request.getName(),
                    request.getStartPrice(),
                    request.getMinIncrement(),
                    startTime,
                    request.getEndTime(),
                    36); // Default anti-sniping extension duration
    
            Auction auction = new Auction(auctionConfig, AuctionStatus.OPEN, sellerId, item.getId());
            
            boolean isAuctionSaved = auctionDAO.saveAuction(auction);
            if (!isAuctionSaved) {
                logger.log(Level.SEVERE, "Critical failure: Unable to persist auction record for name: " + auctionConfig.getName());
                throw new ServiceException(ErrorCode.AUCTION_CREATION_FAILED, "Failed to persist the auction to the database: " + auctionConfig.getName());
            }
    
            AuctionRegistry.getInstance().register(auction); 
    
            scheduleClose(auction); 
            logger.log(Level.INFO, "Auction successfully created and registered with ID: {0}", auctionConfig.getId());
    
            UserDTO sellerDto = userService.getByUserId(sellerId);
            ItemDTO itemDto = ItemDTOFactory.toDto(item);
            
            return toAuctionDto(auction, sellerDto, itemDto);
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionService.createAuction", exception);
            throw exception;
        }
    }

    public AuctionDTO getAuctionById(String auctionId) throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Retrieving auction details for auctionId: " + auctionId);
            validateAuctionId(auctionId);
            
            Auction auction = auctionDAO.findByAuctionId(auctionId);
            if (auction == null) {
                throw new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "Resource not found: The auction with ID " + auctionId + " does not exist.");
            }
    
            UserDTO sellerDto = userService.getByUserId(auction.getSellerId());
            ItemDTO itemDto = itemService.getItemById(auction.getItemId());
            
            if (itemDto == null) {
                throw new ServiceException(ErrorCode.ITEM_NOT_FOUND, "Data integrity error: The item associated with auction " + auctionId + " was not found.");
            }
    
            return toAuctionDto(auction, sellerDto, itemDto);
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionService.getAuctionById: " + exception.getMessage(), exception);
            throw exception;
        }
    }


    public void scheduleClose(Auction auction) throws Exception {
        String auctionId = auction.getAuctionConfig().getId();
        LocalDateTime endTime = auction.getAuctionConfig().getEndTime();
        
        long delayMilliseconds = Duration.between(LocalDateTime.now(), endTime).toMillis();
        if (delayMilliseconds < 0) {
            delayMilliseconds = 0;
        }

        scheduler.schedule(() -> {
            try {
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
                    notificationService.notifyAuctionEnded(auction);
                    logger.log(Level.INFO, "Auction has been automatically concluded: " + auctionId);
                }
            } catch (ServiceException serviceException) {
                logger.log(Level.WARNING, "Business logic error in scheduled auction closure for auctionId: " + auctionId, serviceException);
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error during scheduled auction closure for auctionId: " + auctionId, exception);
                // Background tasks should not rethrow to prevent thread termination unless managed
            }
        }, delayMilliseconds, TimeUnit.MILLISECONDS);
    }

    // --- PRIVATE HELPERS ---

    private AuctionDTO toAuctionDto(Auction auction, UserDTO sellerDto, ItemDTO itemDto) {
        AuctionDTO auctionDto = new AuctionDTO();

        auctionDto.setId(auction.getAuctionConfig().getId());
        auctionDto.setName(auction.getAuctionConfig().getName());
        auctionDto.setStartPrice(auction.getAuctionConfig().getStartPrice());
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

    private void validateCreateAuctionRequest(CreateAuctionRequest request, String sellerId) throws ServiceException {
        if (request == null) {
            throw new ServiceException(ErrorCode.INVALID_DATA, "The auction request payload cannot be null.");
        }
        if (sellerId == null || sellerId.isEmpty()) {
            throw new ServiceException(ErrorCode.MISSING_BIDDER_ID, "The seller identifier is required for auction creation.");
        }
    }

    private void validateAuctionId(String auctionId) throws ServiceException {
        if (auctionId == null || auctionId.isBlank()) {
            throw new ServiceException(ErrorCode.MISSING_AUCTION_ID, "The auction identification is required to perform this operation.");
        }
    }

    private Item createItemFromFactory(CreateAuctionRequest request, String sellerId) throws ServiceException, Exception {
        try {
            return ItemFactory.createItem(request, sellerId);
        } catch (IllegalArgumentException illegalArgumentException) {
            logger.log(Level.SEVERE, "ItemFactory encountered a creation failure: " + illegalArgumentException.getMessage(), illegalArgumentException);
            throw new ServiceException(ErrorCode.INVALID_ITEM_DATA, "Validation failure: The provided item data is invalid: " + illegalArgumentException.getMessage());
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error during item creation via ItemFactory: " + exception.getMessage(), exception);
            throw exception; // Naming Convention: English log and rethrow
        }
    }
}
