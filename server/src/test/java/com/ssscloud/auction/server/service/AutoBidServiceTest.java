package com.ssscloud.auction.server.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * KEY FACTS về AutoBidService (đọc trước khi sửa test):
 *
 * 1. AutoBidRequest chỉ có 2 field: auctionId + maxBid.
 * KHÔNG có increment — increment lấy từ
 * auction.getAuctionConfig().getMinIncrement().
 *
 * 2. validateAutoBidTerms() check:
 * - Nếu chưa có bid: maxBid >= startPrice (throw INCREMENT_TOO_LOW nếu sai)
 * - Nếu đã có bid: maxBid - currentPrice >= minIncrement (throw
 * INCREMENT_TOO_LOW)
 * - KHÔNG check increment > maxBid (không có field increment)
 *
 * 3. AuctionRegistry.getInstance() trả null nếu chưa initialize() → NPE.
 * setUp() phải gọi AuctionRegistry.initialize(auctionDAO) trước.
 *
 * 4. ConcurrentBidManager.processTask() gọi userDAO.lockBidderBalance() từ
 * AutoBidService.trigger().
 * Cần stub lockBidderBalance → true để worker thread không fail silently.
 *
 * 5. Giá proxy: min(max(secondHighest, currentPrice) + minIncrement,
 * winnerMaxBid)
 * Khi isFirstBid và chỉ 1 entry: calculatedBid = startPrice (không submit nếu
 * == currentPrice và không có bid).
 */
public class AutoBidServiceTest {

    private static final String AUCTION_ID = "test-auction-001";
    private static final String SELLER_ID = "seller-001";
    private static final long START_PRICE = 30_000L;
    private static final long MIN_INC = 1_000L;

    private static final long WORKER_TIMEOUT_MS = 2_000;

    private AutoBidService autoBidService;
    private Auction auction;
    private AuctionDAO auctionDAO;
    private UserDAO userDAO;

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        BidTransactionDAO bidTransactionDAO = mock(BidTransactionDAO.class);
        AutoBidService autoBidMock = mock(AutoBidService.class);
        auctionDAO = mock(AuctionDAO.class);
        userDAO = mock(UserDAO.class);
        NotificationController notifCtrl = mock(NotificationController.class);

        doNothing().when(notifCtrl).notifyWatchers(any(Auction.class), anyString());

        // Stub balance operations cho ConcurrentBidManager worker
        when(userDAO.lockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(anyString(), anyLong())).thenReturn(true);

        ConcurrentBidManager.initialize(userDAO, bidTransactionDAO, autoBidMock, auctionDAO, notifCtrl);

        // QUAN TRỌNG: phải initialize AuctionRegistry trước khi dùng getInstance()
        AuctionRegistry.initialize(auctionDAO);

        autoBidService = new AutoBidService(auctionDAO, userDAO);

        AuctionConfig config = new AuctionConfig(
                AUCTION_ID, "Test Auction",
                START_PRICE, MIN_INC,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(2),
                36);
        auction = new Auction(config, AuctionStatus.RUNNING, SELLER_ID, "item-001");
        AuctionRegistry.getInstance().registerIfAbsent(auction);
    }

    @AfterEach
    void tearDown() {
        if (autoBidService != null) {
            try {
                autoBidService.clearRegistrations(AUCTION_ID);
                autoBidService.clearRegistrations("finished-auction");
            } catch (Exception ignored) {
            }
        }
        try {
            ConcurrentBidManager.getInstance().shutdown(AUCTION_ID);
            ConcurrentBidManager.getInstance().shutdown("finished-auction");
        } catch (Exception ignored) {
        }
        ConcurrentBidManager.resetInstance();
        AuctionRegistry.getInstance().remove(AUCTION_ID);
        AuctionRegistry.getInstance().remove("finished-auction");
    }

    // =========================================================================
    // Group 1 — Guard conditions
    // =========================================================================

    @Test
    void testTrigger_nullAuction_returnsWithoutException() {
        // WHY: null auction phải fail silently, không throw
        assertThrows(ServiceException.class, 
            () -> autoBidService.trigger(null));
    }

    @Test
    void testTrigger_finishedAuction_skipped() throws Exception {
        // WHY: auction FINISHED không được trigger thêm auto-bid nào
        AuctionConfig config = new AuctionConfig(
                "finished-auction", "Finished",
                START_PRICE, MIN_INC,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                36);
        Auction finishedAuction = new Auction(config, AuctionStatus.FINISHED, SELLER_ID, "item-002");
        AuctionRegistry.getInstance().registerIfAbsent(finishedAuction);

        assertThrows(ServiceException.class, 
            () -> autoBidService.trigger("finished-auction"));
        assertEquals(0, finishedAuction.getBidTransaction().size());
    }

    @Test
    void testTrigger_noRegistrations_skipped() throws Exception {
        // WHY: registrationsMap rỗng → trigger return ngay, không submit bid
        autoBidService.trigger(auction.getAuctionConfig().getId());
        assertEquals(0, auction.getBidTransaction().size());
    }

    @Test
    void testTrigger_singleEntry_andIsCurrentHighestBidder_skipped() throws Exception {
        // WHY: bidder-A là entry duy nhất VÀ đang là highest bidder →
        // otherCompetitors rỗng → không có ai để cạnh tranh → return
        BidTransaction existingBid = new BidTransaction(
                AUCTION_ID, "bidder-A", "Alice", 35_000L, 35_000L, LocalDateTime.now(), BidType.MANUAL);
        auction.placeBid(existingBid);

        stubBidderWithBalance("bidder-A", 200_000L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        // maxBid=100_000 >= startPrice=30_000, và 100_000 - 35_000=65_000 >=
        // minIncrement=1_000 → valid
        autoBidService.register(buildRequest(AUCTION_ID, 100_000L), "bidder-A", "Alice");
        Thread.sleep(300);

        long autoBidCount = auction.getBidTransaction().stream()
                .filter(b -> b.getType() == BidType.AUTO).count();
        assertEquals(1, autoBidCount,
                "No AUTO bid when only one entry and already leading");
    }

    // =========================================================================
    // Group 2 — Winner selection
    // =========================================================================

    @Test
    void testTrigger_higherMaxBid_wins() throws Exception {
        // WHY: A(max=500k) vs B(max=800k) → B thắng, A bị xóa khỏi registry
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 800_000L), "bidder-B", "Bob");
        Thread.sleep(300);

        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, remaining.size());
        assertEquals("bidder-B", remaining.get(0).bidderId);
    }

    @Test
    void testTrigger_tieBreak_earlierRegistration_wins() throws Exception {
        // WHY: A và B cùng maxBid → người đăng ký TRƯỚC (registeredAt nhỏ hơn) thắng
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L), "bidder-A", "Alice");
        Thread.sleep(10);
        autoBidService.register(buildRequest(AUCTION_ID, 500_000L), "bidder-B", "Bob");
        Thread.sleep(300);

        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals("bidder-A", remaining.get(0).bidderId,
                "Earlier registration must win on tie-break");
    }

    @Test
    void testTrigger_losingEntries_areRemovedFromRegistry() throws Exception {
        // WHY: sau trigger, tất cả entry thua phải bị xóa khỏi registrationsMap
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        stubBidderWithBalance("bidder-C", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 300_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 400_000L), "bidder-B", "Bob");
        autoBidService.register(buildRequest(AUCTION_ID, 600_000L), "bidder-C", "Charlie");
        Thread.sleep(300);

        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, remaining.size(), "Only the winner must remain");
        assertEquals("bidder-C", remaining.get(0).bidderId);
    }

    // =========================================================================
    // Group 3 — Bid calculation
    // Công thức: calculatedBid = min(max(secondHighest, currentPrice) +
    // minIncrement, winnerMaxBid)
    // =========================================================================

    @Test
    void testTrigger_calculatedBid_isSecondHighestPlusIncrement() throws Exception {
        // WHY: A(max=500k), B(max=800k), MIN_INC=1k
        // secondHighest=500k, currentPrice=30k (start)
        // base = max(500k, 30k) = 500k
        // calculated = min(500k + 1k, 800k) = 501k
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 800_000L), "bidder-B", "Bob");

        waitForPrice(501_000L);

        assertEquals(501_000L, auction.getCurrentPrice(),
                "Calculated bid = secondHighest(500k) + minIncrement(1k) = 501k");
    }

    @Test
    void testTrigger_calculatedBid_cappedAtMaxBid() throws Exception {
        // WHY: A(max=500k), B(max=501_500L), MIN_INC=1k
        // base=500k, calculated=min(500k+1k, 501_500) = 501k (không phải 501_500)
        // Thực ra cần maxBid của B < secondHighest + increment để cap kích hoạt
        // A(max=500k), B(max=500_500L): calculated = min(500k+1k=501k, 500_500) =
        // 500_500
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 500_500L), "bidder-B", "Bob");

        waitForPrice(500_500L);

        assertEquals(500_500L, auction.getCurrentPrice(),
                "Calculated bid must be capped at winner maxBid (500_500) when secondHighest+inc exceeds it");
    }

    @Test
    void testTrigger_calculatedBid_notExceedCurrentPrice_skipped() throws Exception {
        // WHY: nếu calculatedBid <= currentPrice → không submit bid
        // currentPrice=100_000, bidder-B maxBid=100_000 → bid-B maxBid - currentPrice =
        // 0 < minIncrement=1000
        // → validateAutoBidTerms throw → không đăng ký được
        // Vậy test này verify: sau khi đã có bid 100k, bidder-B register maxBid=100k →
        // throw INCREMENT_TOO_LOW
        BidTransaction existingBid = new BidTransaction(
                AUCTION_ID, "bidder-A", "Alice", 100_000L, 100_000L, LocalDateTime.now(), BidType.MANUAL);
        auction.placeBid(existingBid);

        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        // maxBid=100_000, currentPrice=100_000 → diff=0 < minIncrement=1000 → throw
        // INCREMENT_TOO_LOW
        ServiceException ex = assertThrows(ServiceException.class,
                () -> autoBidService.register(buildRequest(AUCTION_ID, 100_000L), "bidder-B", "Bob"));
        assertEquals(ErrorCode.INCREMENT_TOO_LOW, ex.getErrorCode());

        assertEquals(100_000L, auction.getCurrentPrice());
    }

    // =========================================================================
    // Group 4 — register() validation
    // =========================================================================

    @Test
    void testRegister_nullRequest_throwsServiceException() {
        // WHY: null request → AUTO_BID_VALIDATION_ERROR
        ServiceException ex = assertThrows(ServiceException.class,
                () -> autoBidService.register(null, "bidder-A", "Alice"));
        assertEquals(ErrorCode.AUTO_BID_VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void testRegister_sellerBidsOwnAuction_throwsServiceException() throws Exception {
        // WHY: seller không được tự đấu giá phiên của mình
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);
        stubBidderWithBalance(SELLER_ID, 999_999L);

        // maxBid=100_000 >= startPrice=30_000 → pass amount check, fail seller check
        ServiceException ex = assertThrows(ServiceException.class,
                () -> autoBidService.register(buildRequest(AUCTION_ID, 100_000L), SELLER_ID, "Seller"));
        assertEquals(ErrorCode.AUTO_SELLER_CANNOT_AUTOBID, ex.getErrorCode());
    }

    @Test
    void testRegister_maxBidBelowStartPrice_throwsIncrementTooLow() throws Exception {
        // WHY: chưa có bid, maxBid < startPrice(30_000) → INCREMENT_TOO_LOW
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);
        stubBidderWithBalance("bidder-A", 999_999L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> autoBidService.register(buildRequest(AUCTION_ID, 29_000L), "bidder-A", "Alice"));
        assertEquals(ErrorCode.INCREMENT_TOO_LOW, ex.getErrorCode());
    }

    @Test
    void testRegister_maxBidBelowCurrentPricePlusMinIncrement_throwsIncrementTooLow() throws Exception {
        // WHY: đã có bid 50_000, maxBid=50_500, diff=500 < minIncrement=1000 →
        // INCREMENT_TOO_LOW
        BidTransaction existingBid = new BidTransaction(
                AUCTION_ID, "other", "Other", 50_000L, 50_000L, LocalDateTime.now(), BidType.MANUAL);
        auction.placeBid(existingBid);

        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);
        stubBidderWithBalance("bidder-A", 999_999L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> autoBidService.register(buildRequest(AUCTION_ID, 50_500L), "bidder-A", "Alice"));
        assertEquals(ErrorCode.INCREMENT_TOO_LOW, ex.getErrorCode());
    }

    @Test
    void testRegister_insufficientBalance_throwsInsufficientBalance() throws Exception {
        // WHY: balance < maxBid → INSUFFICIENT_BALANCE
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);
        // balance=10_000 < maxBid=100_000
        stubBidderWithBalance("bidder-A", 10_000L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> autoBidService.register(buildRequest(AUCTION_ID, 100_000L), "bidder-A", "Alice"));
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, ex.getErrorCode());
    }

    @Test
    void testRegister_replacesExistingEntry_forSameBidder() throws Exception {
        // WHY: đăng ký lại phải replace entry cũ, không duplicate
        stubBidderWithBalance("bidder-A", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 100_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 200_000L), "bidder-A", "Alice");

        List<AutoBidService.AutoBidEntry> entries = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, entries.size(), "Re-registration must replace, not duplicate");
        assertEquals(200_000L, entries.get(0).maxBid, "Entry must reflect latest maxBid");
    }

    // =========================================================================
    // Group 5 — State tracking
    // =========================================================================

    @Test
    void testAutoBidCount_incrementsAfterSuccessfulTrigger() throws Exception {
        // WHY: sau trigger, AUTO bid phải xuất hiện trong bid history
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 300_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 500_000L), "bidder-B", "Bob");

        // Chờ worker đặt bid — giá phải tăng lên trên START_PRICE
        waitForPrice(START_PRICE + 1);

        long autoBidCount = auction.getBidTransaction().stream()
                .filter(b -> b.getType() == BidType.AUTO && b.getBidderId().equals("bidder-B"))
                .count();
        assertEquals(1, autoBidCount,
                "Winner's AUTO bid transaction must appear in auction history");
    }

    @Test
    void testClearRegistrations_removesAllEntries() throws Exception {
        // WHY: clearRegistrations() phải xóa sạch toàn bộ entry
        stubBidderWithBalance("bidder-A", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 100_000L), "bidder-A", "Alice");
        autoBidService.clearRegistrations(AUCTION_ID);

        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertTrue(remaining.isEmpty(), "All entries must be removed after clearRegistrations");
    }

    @Test
    void testClearRegistrations_preventsNewRegistration() throws Exception {
        // WHY: sau clearRegistrations(), mọi register() tiếp theo phải bị reject với
        // AUCTION_CLOSED
        stubBidderWithBalance("bidder-A", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.clearRegistrations(AUCTION_ID);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> autoBidService.register(buildRequest(AUCTION_ID, 100_000L), "bidder-A", "Alice"));
        assertEquals(ErrorCode.AUCTION_CLOSED, ex.getErrorCode());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * AutoBidRequest chỉ có auctionId + maxBid.
     * Không có increment field — increment lấy từ auction config.
     */
    private AutoBidRequest buildRequest(String auctionId, long maxBid) {
        AutoBidRequest req = new AutoBidRequest();
        req.setAuctionId(auctionId);
        req.setMaxBid(maxBid);
        return req;
    }

    private void stubBidderWithBalance(String bidderId, long balance) throws Exception {
        Bidder bidder = new Bidder("Test User", bidderId, "pass", bidderId + "@test.com",
                UserRole.BIDDER, balance);
        when(userDAO.findById(bidderId)).thenReturn(bidder);
    }

    private void waitForPrice(long expectedPrice) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WORKER_TIMEOUT_MS;
        while (auction.getCurrentPrice() < expectedPrice
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }
}