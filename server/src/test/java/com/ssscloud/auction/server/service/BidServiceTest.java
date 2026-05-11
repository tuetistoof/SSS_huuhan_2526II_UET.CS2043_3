package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

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

public class BidServiceTest {

    private AuctionDAO auctionDAO;
    private UserDAO userDAO;
    private BidService bidService;
    private Auction mockAuction;

    @BeforeEach
    void setUp() {
        auctionDAO = Mockito.mock(AuctionDAO.class);
        userDAO = Mockito.mock(UserDAO.class);
        bidService = new BidService(auctionDAO, userDAO);

        AuctionConfig config = new AuctionConfig();
        mockAuction = new Auction(config, AuctionStatus.RUNNING, "seller123", "item123");
    }

    @Test
    void testRequestNull() {
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
        PlaceBidRequest req = new PlaceBidRequest("auction123", 50000L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "", "Kphong"));
    }

    @Test
    void testNegativeBidAmount() {
        PlaceBidRequest req = new PlaceBidRequest("auction123", -1000L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "bidder123", "Kphong"));
    }

    @Test
    void testAuctionNotFound() {
        when(auctionDAO.findByAuctionId("khongtontai")).thenReturn(null);

        PlaceBidRequest req = new PlaceBidRequest("khongtontai", 50000L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "bidder123", "Kphong"));
    }

    @Test
    void testSellerCannotBidOnOwnAuction() {
        when(auctionDAO.findByAuctionId("auction123")).thenReturn(mockAuction);

        PlaceBidRequest req = new PlaceBidRequest("auction123", 50000L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "seller123", "seller"));
    }

    @Test
    void testAmountExceedsAccountBalance() {
        when(auctionDAO.findByAuctionId("auction123")).thenReturn(mockAuction);

        Bidder bidder = new Bidder("Kphong", "Kphong", "123456", "kphong@gmail.com", UserRole.BIDDER);
        bidder.setAccountBalance(10000L);
        when(userDAO.findById("bidder123")).thenReturn(bidder);

        PlaceBidRequest req = new PlaceBidRequest("auction123", 50000L);
        assertThrows(InvalidBidException.class, () ->
            bidService.placeBid(req, "bidder123", "Kphong"));
    }
}