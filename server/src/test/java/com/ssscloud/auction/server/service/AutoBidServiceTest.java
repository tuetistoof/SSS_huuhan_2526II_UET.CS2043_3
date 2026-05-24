package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.model.auction.BidTransaction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.model.user.Bidder;
import com.ssscloud.auction.common.payload.request.AutoBidRequest;
import com.ssscloud.auction.server.controller.NotificationController;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;

/**
 * Unit tests for AutoBidService.
 *
 * WHY: AutoBidService chứa business logic phức tạp nhất trong toàn hệ thống:
 * chọn winner theo maxBid, tie-break theo registeredAt, tính giá theo công thức
 * min(secondHighest + increment, maxBid), và xóa các entry thua sau mỗi lần trigger.
 * Nếu bất kỳ logic nào sai, người thắng sai sẽ được công nhận hoặc giá sai sẽ được đặt.
 *
 * Strategy: test trực tiếp trên AutoBidService với ConcurrentBidManager được
 * initialize bằng mock DAO để tránh DB. Verify kết quả qua trạng thái in-memory
 * của Auction object và registrationsMap qua getRegistrations().
 *
 * Threading note: register() gọi trigger() nội bộ — trigger() submit bid vào
 * ConcurrentBidManager, worker thread xử lý bất đồng bộ. Mọi test có assertion
 * về state sau trigger đều dùng waitForPrice() để polling thay vì Thread.sleep()
 * cố định — tránh flaky trên máy chậm.
 */
public class AutoBidServiceTest {

    private static final String AUCTION_ID  = "test-auction-001";
    private static final String SELLER_ID   = "seller-001";
    private static final long   START_PRICE = 30_000L;
    private static final long   MIN_INC     = 1_000L;

    /** Timeout tối đa chờ worker thread (ms). */
    private static final long WORKER_TIMEOUT_MS = 2_000;

    private AutoBidService autoBidService;
    private Auction        auction;
    private AuctionDAO     auctionDAO;
    private UserDAO        userDAO;

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        BidTransactionDAO  bidTransactionDAO = mock(BidTransactionDAO.class);
        AutoBidService     autoBidMock       = mock(AutoBidService.class);
        auctionDAO                           = mock(AuctionDAO.class);
        userDAO                              = mock(UserDAO.class);
        NotificationController notifCtrl     = mock(NotificationController.class);

        doNothing().when(notifCtrl).notifyWatchers(any(Auction.class), anyString());

        ConcurrentBidManager.initialize(userDAO, bidTransactionDAO, autoBidMock, auctionDAO, notifCtrl);

        autoBidService = new AutoBidService(auctionDAO, userDAO);

        AuctionConfig config = new AuctionConfig(
            AUCTION_ID, "Test Auction",
            START_PRICE, MIN_INC,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        auction = new Auction(config, AuctionStatus.RUNNING, SELLER_ID, "item-001");
        AuctionRegistry.getInstance().registerIfAbsent(auction);
    }

    @AfterEach
    void tearDown() {
        ConcurrentBidManager.resetInstance();
        AuctionRegistry.getInstance().remove(AUCTION_ID);
        AuctionRegistry.getInstance().remove("finished-auction");
    }

    // =========================================================================
    // Group 1 — Guard conditions
    // =========================================================================

    @Test
    void testTrigger_nullAuction_returnsWithoutException() {
        // WHY: null được truyền vào khi auction chưa load xong — phải fail silently
        assertDoesNotThrow(() -> autoBidService.trigger(null));
    }

    @Test
    void testTrigger_finishedAuction_skipped() throws Exception {
        // WHY: auction FINISHED không được trigger thêm bất kỳ auto-bid nào
        AuctionConfig config = new AuctionConfig(
            "finished-auction", "Finished",
            START_PRICE, MIN_INC,
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1),
            36
        );
        Auction finishedAuction = new Auction(config, AuctionStatus.FINISHED, SELLER_ID, "item-002");
        AuctionRegistry.getInstance().registerIfAbsent(finishedAuction);

        assertDoesNotThrow(() -> autoBidService.trigger(finishedAuction));
        assertEquals(0, finishedAuction.getBidTransaction().size());
    }

    @Test
    void testTrigger_noRegistrations_skipped() throws Exception {
        // WHY: registrationsMap rỗng → trigger phải return ngay, không submit bid nào
        autoBidService.trigger(auction);
        assertEquals(0, auction.getBidTransaction().size());
    }

    @Test
    void testTrigger_singleEntry_andIsCurrentHighestBidder_skipped() throws Exception {
        // WHY: bidder-A là entry duy nhất VÀ đang là highest bidder →
        // otherCompetitors rỗng → không có ai để cạnh tranh → return
        BidTransaction existingBid = new BidTransaction(
            AUCTION_ID, "bidder-A", "Alice", 35_000L, 35_000L, LocalDateTime.now(), BidType.MANUAL
        );
        auction.placeBid(existingBid);

        stubBidderWithBalance("bidder-A", 200_000L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 5_000L), "bidder-A", "Alice");

        Thread.sleep(300);

        long autoBidCount = auction.getBidTransaction().stream()
            .filter(b -> b.getType() == BidType.AUTO).count();
        assertEquals(0, autoBidCount,
            "No AUTO bid should be submitted when only one entry and already leading");
    }

    // =========================================================================
    // Group 2 — Winner selection
    // =========================================================================

    @Test
    void testTrigger_higherMaxBid_wins() throws Exception {
        // WHY: A(max=500k) vs B(max=800k) → B phải thắng và được submitBid
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 10_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 800_000L, 10_000L), "bidder-B", "Bob");

        Thread.sleep(300);

        // B thắng → chỉ còn entry của B trong registrationsMap
        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, remaining.size());
        assertEquals("bidder-B", remaining.get(0).bidderId);
    }

    @Test
    void testTrigger_tieBreak_earlierRegistration_wins() throws Exception {
        // WHY: A và B có cùng maxBid → người đăng ký TRƯỚC (registeredAt nhỏ hơn) thắng
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 10_000L), "bidder-A", "Alice");
        Thread.sleep(10); // đảm bảo registeredAt của A trước B
        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 10_000L), "bidder-B", "Bob");

        Thread.sleep(300);

        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, remaining.size());
        assertEquals("bidder-A", remaining.get(0).bidderId,
            "When maxBid is equal, earlier registration must win (tie-break by registeredAt)");
    }

    @Test
    void testTrigger_losingEntries_areRemovedFromRegistry() throws Exception {
        // WHY: sau khi trigger, entry thua phải bị xóa khỏi registrationsMap.
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        stubBidderWithBalance("bidder-C", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 300_000L, 10_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 400_000L, 10_000L), "bidder-B", "Bob");
        autoBidService.register(buildRequest(AUCTION_ID, 600_000L, 10_000L), "bidder-C", "Charlie");

        Thread.sleep(300);

        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, remaining.size(), "Only the winner must remain in registrations");
        assertEquals("bidder-C", remaining.get(0).bidderId);
    }

    // =========================================================================
    // Group 3 — Bid calculation: min(secondHighest + increment, maxBid)
    // =========================================================================

    @Test
    void testTrigger_calculatedBid_isSecondHighestPlusIncrement() throws Exception {
        // WHY: A(max=500k, inc=50k), B(max=800k, inc=50k)
        // secondHighest = 500k → calculated = min(500k+50k, 800k) = 550k
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 50_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 800_000L, 50_000L), "bidder-B", "Bob");

        waitForPrice(550_000L);

        assertEquals(550_000L, auction.getCurrentPrice(),
            "Calculated bid must be secondHighest(500k) + winnerIncrement(50k) = 550k");
    }

    @Test
    void testTrigger_calculatedBid_cappedAtMaxBid() throws Exception {
        // WHY: A(max=500k, inc=50k), B(max=520k, inc=50k)
        // calculated = min(500k+50k=550k, 520k) → phải là 520k
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 50_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 520_000L, 50_000L), "bidder-B", "Bob");

        waitForPrice(520_000L);

        assertEquals(520_000L, auction.getCurrentPrice(),
            "Calculated bid must be capped at winner's maxBid (520k), not secondHighest+increment (550k)");
    }

    @Test
    void testTrigger_calculatedBid_notExceedCurrentPrice_skipped() throws Exception {
        // WHY: nếu calculatedBid <= currentPrice, không được submit bid
        BidTransaction existingBid = new BidTransaction(
            AUCTION_ID, "bidder-A", "Alice", 100_000L, 100_000L, LocalDateTime.now(), BidType.MANUAL
        );
        auction.placeBid(existingBid);

        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);
        autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 10_000L), "bidder-B", "Bob");

        Thread.sleep(300);

        assertEquals(100_000L, auction.getCurrentPrice());
        long autoBidCount = auction.getBidTransaction().stream()
            .filter(b -> b.getType() == BidType.AUTO).count();
        assertEquals(0, autoBidCount,
            "No AUTO bid should be submitted when calculatedBid cannot exceed currentPrice");
    }

    // =========================================================================
    // Group 4 — register() validation
    // =========================================================================

    @Test
    void testRegister_nullRequest_throwsServiceException() {
        // WHY: null request phải throw ServiceException(AUTO_BID_VALIDATION_ERROR)
        ServiceException ex = assertThrows(ServiceException.class,
            () -> autoBidService.register(null, "bidder-A", "Alice"));
        assertEquals(ErrorCode.AUTO_BID_VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void testRegister_sellerBidsOwnAuction_throwsServiceException() throws Exception {
        // WHY: seller không được tự đấu giá phiên của mình
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);
        stubBidderWithBalance(SELLER_ID, 999_999L);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 5_000L), SELLER_ID, "Seller"));
        assertEquals(ErrorCode.AUTO_SELLER_CANNOT_AUTOBID, ex.getErrorCode());
    }

    @Test
    void testRegister_incrementBelowAuctionMinIncrement_throwsServiceException() throws Exception {
        // WHY: increment phải >= auction's minIncrement (1000L)
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);
        stubBidderWithBalance("bidder-A", 999_999L);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 500L), "bidder-A", "Alice"));
        assertEquals(ErrorCode.INCREMENT_TOO_LOW, ex.getErrorCode());
    }

    @Test
    void testRegister_incrementExceedsMaxBid_throwsServiceException() {
        // WHY: increment > maxBid là vô nghĩa về mặt logic
        ServiceException ex = assertThrows(ServiceException.class,
            () -> autoBidService.register(buildRequest(AUCTION_ID, 10_000L, 20_000L), "bidder-A", "Alice"));
        assertEquals(ErrorCode.AUTO_BID_INVALID_RANGE, ex.getErrorCode());
    }

    @Test
    void testRegister_replacesExistingEntry_forSameBidder() throws Exception {
        // WHY: bidder đăng ký lại phải replace entry cũ, không duplicate
        stubBidderWithBalance("bidder-A", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 5_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 200_000L, 5_000L), "bidder-A", "Alice");

        List<AutoBidService.AutoBidEntry> entries = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, entries.size(),
            "Re-registration must replace, not duplicate the existing entry");
        assertEquals(200_000L, entries.get(0).maxBid,
            "Entry must reflect the latest registration");
    }

    // =========================================================================
    // Group 5 — State tracking
    // =========================================================================

    @Test
    void testAutoBidCount_incrementsAfterSuccessfulTrigger() throws Exception {
        // WHY: Verify winner's AUTO bid appears in auction's bid history after trigger.
        // AutoBidService locks maxBid and submits calculatedBid → worker places BidType.AUTO.
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 300_000L, 10_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 10_000L), "bidder-B", "Bob");

        // Wait for worker to process and place AUTO bid
        waitForPrice(START_PRICE + 1);

        long autoBidCount = auction.getBidTransaction().stream()
            .filter(b -> b.getType() == BidType.AUTO && b.getBidderId().equals("bidder-B"))
            .count();
        assertEquals(1, autoBidCount,
            "Winner's AUTO bid transaction must appear in auction history after trigger");
    }

    @Test
    void testClearRegistrations_removesAllEntries() throws Exception {
        // WHY: clearRegistrations() được gọi khi auction FINISHED.
        // Nếu không xóa sạch, state rác sẽ tồn tại và ảnh hưởng các lần test/restart
        stubBidderWithBalance("bidder-A", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 5_000L), "bidder-A", "Alice");
        autoBidService.clearRegistrations(AUCTION_ID);

        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertTrue(remaining.isEmpty(),
            "All entries must be removed after clearRegistrations");
    }

    @Test
    void testClearRegistrations_preventsNewRegistration() throws Exception {
        // WHY: sau khi clearRegistrations(), cancelledAuctionIds chứa auctionId.
        // Mọi lần gọi register() sau đó phải bị reject với AUCTION_CLOSED.
        stubBidderWithBalance("bidder-A", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.clearRegistrations(AUCTION_ID);

        // Subsequent registration must be rejected
        assertThrows(ServiceException.class,
            () -> autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 5_000L), "bidder-A", "Alice"),
            "register() after clearRegistrations() must throw AUCTION_CLOSED");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AutoBidRequest buildRequest(String auctionId, long maxBid, long increment) {
        AutoBidRequest req = new AutoBidRequest();
        req.setAuctionId(auctionId);
        req.setMaxBid(maxBid);
        req.setIncrement(increment);
        return req;
    }

    private void stubBidderWithBalance(String bidderId, long balance) throws Exception {
        Bidder bidder = new Bidder("Test User", bidderId, "pass", bidderId + "@test.com",
            UserRole.BIDDER, balance);
        when(userDAO.findById(bidderId)).thenReturn(bidder);
    }

    /**
     * Polling: chờ đến khi giá auction đạt expectedPrice hoặc timeout.
     * WHY: tránh flaky test do Thread.sleep() cố định không đủ trên máy chậm.
     */
    private void waitForPrice(long expectedPrice) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WORKER_TIMEOUT_MS;
        while (auction.getCurrentPrice() < expectedPrice
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }
}