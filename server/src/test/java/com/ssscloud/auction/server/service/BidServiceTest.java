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
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.Seller;
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

        // Remove from Registry to force auctionDAO mock usage
        AuctionRegistry.getInstance().remove("auction-config-1");
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
        PlaceBidRequest placeBidRequest = new PlaceBidRequest("auction-config-1", 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "", "Kphong"));
    }

    @Test
    void testNegativeBidAmount() {
        PlaceBidRequest placeBidRequest = new PlaceBidRequest("auction-config-1", -1000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "bidder123", "Kphong"));
    }

    @Test
    void testZeroBidAmount() {
        PlaceBidRequest placeBidRequest = new PlaceBidRequest("auction-config-1", 0L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "bidder123", "Kphong"));
    }

    // --- Business Logic Tests ---

    @Test
    void testAuctionNotFound() throws Exception{
        when(auctionDAO.findByAuctionId("notfound")).thenReturn(null);

        PlaceBidRequest placeBidRequest = new PlaceBidRequest("notfound", 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "bidder123", "Kphong"));
    }

    @Test
    void testSellerCannotBidOnOwnAuction() throws Exception{
        when(auctionDAO.findByAuctionId("auction-config-1")).thenReturn(mockAuction);

        PlaceBidRequest placeBidRequest = new PlaceBidRequest("auction-config-1", 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "seller123", "seller"));
    }

    @Test
    void testUserNotBidder() throws Exception{
        when(auctionDAO.findByAuctionId("auction-config-1")).thenReturn(mockAuction);

        Seller seller = new Seller("seller", "seller", "123456", "seller@gmail.com", UserRole.SELLER);
        when(userDAO.findById("sellerId")).thenReturn(seller);

        PlaceBidRequest placeBidRequest = new PlaceBidRequest("auction-config-1", 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "sellerId", "seller"));
    }

    @Test
    void testInsufficientBalance() throws Exception{
        when(auctionDAO.findByAuctionId("auction-config-1")).thenReturn(mockAuction);

        Bidder poorBidder = new Bidder(
            "Poor", "poor", "123456", "poor@gmail.com",
            UserRole.BIDDER, 10000L
        );
        when(userDAO.findById("poorBidder")).thenReturn(poorBidder);

        PlaceBidRequest placeBidRequest = new PlaceBidRequest("auction-config-1", 50000L);
        assertThrows(ServiceException.class, () ->
            bidService.placeBid(placeBidRequest, "poorBidder", "poor"));
    }
}