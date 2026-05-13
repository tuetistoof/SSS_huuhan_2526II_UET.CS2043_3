package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ssscloud.auction.common.dto.request.PlaceBidRequest;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.InvalidBidException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;

public class BidServiceTest {

    private AuctionDAO auctionDAO;
    private UserDAO userDAO;
    private BidService bidService;
    private Auction mockAuction;
    private Bidder mockBidder;

    @BeforeEach
    void setUp() {
        auctionDAO = Mockito.mock(AuctionDAO.class);
        userDAO = Mockito.mock(UserDAO.class);
        bidService = new BidService(auctionDAO, userDAO);

        AuctionConfig config = new AuctionConfig(
            "auction-config-1",
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

        AuctionRegistry.getInstance().remove("auction-config-1");
    }

    // --- Validation tests ---

    @Test
    void testNullRequest() {
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(null, "bidder123", "Kphong"));
    }

    @Test
    void testBlankAuctionId() {
        PlaceBidRequest req = new PlaceBidRequest("", 50000L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "bidder123", "Kphong"));
    }

    @Test
    void testBlankBidderId() {
        PlaceBidRequest req = new PlaceBidRequest("auction-config-1", 50000L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "", "Kphong"));
    }

    @Test
    void testNegativeBidAmount() {
        PlaceBidRequest req = new PlaceBidRequest("auction-config-1", -1000L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "bidder123", "Kphong"));
    }

    @Test
    void testZeroBidAmount() {
        PlaceBidRequest req = new PlaceBidRequest("auction-config-1", 0L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "bidder123", "Kphong"));
    }

    // --- Business logic tests ---

    @Test
    void testAuctionNotFound() {
        when(auctionDAO.findByAuctionId("notfound")).thenReturn(null);

        PlaceBidRequest req = new PlaceBidRequest("notfound", 50000L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "bidder123", "Kphong"));
    }

    @Test
    void testSellerCannotBidOnOwnAuction() {
        when(auctionDAO.findByAuctionId("auction-config-1")).thenReturn(mockAuction);

        PlaceBidRequest req = new PlaceBidRequest("auction-config-1", 50000L);
        // seller123 là người tạo auction, không được đặt giá
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "seller123", "seller"));
    }

    @Test
    void testUserNotBidder() {
        when(auctionDAO.findByAuctionId("auction-config-1")).thenReturn(mockAuction);

        // Trả về null → không phải Bidder instance
        when(userDAO.findById("bidder123")).thenReturn(null);

        PlaceBidRequest req = new PlaceBidRequest("auction-config-1", 50000L);
        assertThrows(IllegalArgumentException.class, () ->
            bidService.placeBid(req, "bidder123", "Kphong"));
    }

    @Test
    void testInsufficientBalance() {
        when(auctionDAO.findByAuctionId("auction-config-1")).thenReturn(mockAuction);

        // Bidder chỉ có 10000 nhưng đặt 50000
        Bidder poorBidder = new Bidder(
            "Poor", "poor", "123456", "poor@gmail.com",
            UserRole.BIDDER, 10000L
        );
        when(userDAO.findById("poorBidder")).thenReturn(poorBidder);

        PlaceBidRequest req = new PlaceBidRequest("auction-config-1", 50000L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "poorBidder", "poor"));
    }
}