package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.auction.BidTransaction;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.model.user.Bidder;
import com.ssscloud.auction.common.model.user.Seller;
import com.ssscloud.auction.common.payload.request.PlaceBidRequest;
import com.ssscloud.auction.server.controller.NotificationController;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;

public class BidServiceTest {

    private static final String AUCTION_ID = "auction-config-1";

    private AuctionDAO auctionDAO;
    private UserDAO userDAO;
    private BidService bidService;
    private Auction mockAuction;
    private Bidder mockBidder;

    @BeforeEach
    void setUp() throws Exception {
        auctionDAO = Mockito.mock(AuctionDAO.class);
        userDAO = Mockito.mock(UserDAO.class);
        bidService = new BidService(auctionDAO, userDAO);

        when(userDAO.lockBidderBalance(any(), anyLong())).thenReturn(true);
        when(userDAO.unlockBidderBalance(any(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(any(), anyLong())).thenReturn(true);

        AuctionRegistry.initialize(auctionDAO);

        // Initialize ConcurrentBidManager singleton với mock DAO để các test
        // gọi đến submitBid() không bị NPE (singleton chưa được khởi tạo)
        BidTransactionDAO bidTransactionDAO = mock(BidTransactionDAO.class);
        AutoBidService autoBidService = mock(AutoBidService.class);
        NotificationController notifController = mock(NotificationController.class);
        doNothing().when(notifController).notifyWatchers(any(Auction.class), anyString());
        ConcurrentBidManager.initialize(userDAO, bidTransactionDAO, autoBidService, auctionDAO, notifController);

        AuctionConfig config = new AuctionConfig(
            AUCTION_ID,
            "Test Auction",
            30000L,
            1000L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            30
        );

        mockAuction = new Auction(config, AuctionStatus.RUNNING, "seller123", "item123");

        mockBidder = new Bidder(
            "Kphong", "Kphong", "123456", "kphong@gmail.com",
            UserRole.BIDDER, 100000L
        );

        // Remove khỏi Registry để buộc BidService dùng auctionDAO mock
        AuctionRegistry.getInstance().remove(AUCTION_ID);
    }

    @AfterEach
    void tearDown() {
        try {
            ConcurrentBidManager.getInstance().shutdown(AUCTION_ID);
        } catch (Exception ignored) {}
        ConcurrentBidManager.resetInstance();
        AuctionRegistry.getInstance().remove(AUCTION_ID);
    }

    // --- Validation Tests ---

    @Test
    void testNullRequest() {
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(null, "bidder123", "Kphong"));
    }

    @Test
    void testBlankAuctionId() {
        PlaceBidRequest placeBidRequest = new PlaceBidRequest("", 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "bidder123", "Kphong"));
    }

    @Test
    void testBlankBidderId() {
        PlaceBidRequest placeBidRequest = new PlaceBidRequest(AUCTION_ID, 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "", "Kphong"));
    }

    @Test
    void testNegativeBidAmount() {
        PlaceBidRequest placeBidRequest = new PlaceBidRequest(AUCTION_ID, -1000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "bidder123", "Kphong"));
    }

    @Test
    void testZeroBidAmount() {
        PlaceBidRequest placeBidRequest = new PlaceBidRequest(AUCTION_ID, 0L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "bidder123", "Kphong"));
    }

    // --- Business Logic Tests ---

    @Test
    void testAuctionNotFound() throws Exception {
        when(auctionDAO.findByAuctionId("notfound")).thenReturn(null);

        PlaceBidRequest placeBidRequest = new PlaceBidRequest("notfound", 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "bidder123", "Kphong"));
    }

    @Test
    void testSellerCannotBidOnOwnAuction() throws Exception {
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(mockAuction);

        PlaceBidRequest placeBidRequest = new PlaceBidRequest(AUCTION_ID, 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "seller123", "seller"));
    }

    @Test
    void testUserNotBidder() throws Exception {
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(mockAuction);

        Seller seller = new Seller("seller", "seller", "123456", "seller@gmail.com", UserRole.SELLER);
        when(userDAO.findById("sellerId")).thenReturn(seller);

        PlaceBidRequest placeBidRequest = new PlaceBidRequest(AUCTION_ID, 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "sellerId", "seller"));
    }

    @Test
    void testInsufficientBalance() throws Exception {
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(mockAuction);

        Bidder poorBidder = new Bidder(
            "Poor", "poor", "123456", "poor@gmail.com",
            UserRole.BIDDER, 10000L
        );
        when(userDAO.findById("poorBidder")).thenReturn(poorBidder);

        PlaceBidRequest placeBidRequest = new PlaceBidRequest(AUCTION_ID, 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "poorBidder", "poor"));
    }

    @Test
    void testBidAmountIncrementTooLow() throws Exception {
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(mockAuction);
        when(userDAO.findById("bidder123")).thenReturn(mockBidder);

        // Place a baseline bid first
        mockAuction.placeBid(new BidTransaction(
            AUCTION_ID, "bidder-pre", "pre",
            30000L, 30000L, LocalDateTime.now(), BidType.MANUAL
        ));

        // startPrice=30000, minIncrement=1000 → cần >= 31000, gửi 30500 → fail
        PlaceBidRequest placeBidRequest = new PlaceBidRequest(AUCTION_ID, 30500L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "bidder123", "Kphong"));
    }

    @Test
    void testFinishedAuction_rejectsBid() throws Exception {
        // WHY: auction ở trạng thái FINISHED (isEnded() == true) phải bị reject ngay
        // tại retrieveAndValidateAuction() với AUCTION_CLOSED.
        AuctionConfig config = new AuctionConfig(
            "finished-auction", "Finished Auction",
            30000L, 1000L,
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1),
            30
        );
        Auction finishedAuction = new Auction(config, AuctionStatus.FINISHED, "seller123", "item123");

        when(auctionDAO.findByAuctionId("finished-auction")).thenReturn(finishedAuction);
        AuctionRegistry.getInstance().remove("finished-auction");

        PlaceBidRequest request = new PlaceBidRequest("finished-auction", 50000L);
        ServiceException ex = assertThrows(ServiceException.class, () ->
            bidService.placeBid(request, "bidder123", "Kphong"));
        assertEquals(ErrorCode.AUCTION_CLOSED, ex.getErrorCode());
    }

    @Test
    void testBidAtExactMinimumIncrement_isAccepted() throws Exception {
        // WHY: validatePlaceBidTerms() dùng điều kiện strict less than:
        //   bidAmount - currentPrice < minIncrement → reject
        // Suy ra: bidAmount == currentPrice + minIncrement phải được CHẤP NHẬN.
        // Setup: startPrice=30000, minIncrement=1000, không có bid trước
        // → currentPrice = 30000, bid hợp lệ tối thiểu = 30000 + 1000 = 31000
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(mockAuction);
        when(userDAO.findById("bidder123")).thenReturn(mockBidder); // balance=100000 đủ

        PlaceBidRequest request = new PlaceBidRequest(AUCTION_ID, 31000L);
        assertDoesNotThrow(() -> bidService.placeBid(request, "bidder123", "Kphong"));
    }
}