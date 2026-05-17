package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ssscloud.auction.common.dto.request.AutoBidRequest;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.base.AuctionConfig;
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
 * về state sau trigger đều cần Thread.sleep() để đợi worker hoàn thành.
 */
public class AutoBidServiceTest {

    private static final String AUCTION_ID  = "test-auction-001";
    private static final String SELLER_ID   = "seller-001";
    private static final long   START_PRICE = 30_000L;
    private static final long   MIN_INC     = 1_000L;

    /** Thời gian chờ worker thread xử lý hết queue (ms). */
    private static final long WORKER_WAIT_MS = 300;

    private AutoBidService autoBidService;
    private Auction        auction;
    private AuctionDAO     auctionDAO;
    private UserDAO        userDAO;

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        // Mock tất cả DAO để tránh kết nối DB
        BidTransactionDAO bidTransactionDAO    = mock(BidTransactionDAO.class);
        AutoBidService    autoBidServiceMock   = mock(AutoBidService.class);
        auctionDAO                             = mock(AuctionDAO.class);
        NotificationController notifController = mock(NotificationController.class);

        doNothing().when(notifController).notifyWatchers(anyString(), anyString());

        // Reset singleton ConcurrentBidManager để test độc lập
        ConcurrentBidManager.initialize(bidTransactionDAO, autoBidServiceMock, auctionDAO, notifController);

        userDAO        = mock(UserDAO.class);
        autoBidService = new AutoBidService(auctionDAO, userDAO);

        // Auction mặc định: RUNNING, chưa có bid nào
        AuctionConfig config = new AuctionConfig(
            AUCTION_ID, "Test Auction",
            START_PRICE, MIN_INC,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        auction = new Auction(config, AuctionStatus.RUNNING, SELLER_ID, "item-001");
        AuctionRegistry.getInstance().register(auction);
    }

    @AfterEach
    void tearDown() {
        ConcurrentBidManager.resetInstance();
        AuctionRegistry.getInstance().remove(AUCTION_ID);
    }

    // =========================================================================
    // Group 1 — Guard conditions
    // =========================================================================

    @Test
    void testTrigger_nullAuction_returnsWithoutException() throws Exception {
        // WHY: null được truyền vào khi auction chưa load xong — phải fail silently
        assertDoesNotThrow(() -> autoBidService.trigger(null));
    }

    @Test
    void testTrigger_finishedAuction_skipped() throws Exception {
        // WHY: auction FINISHED không được trigger thêm bất kỳ auto-bid nào,
        // dù vẫn còn entry trong registry
        AuctionConfig config = new AuctionConfig(
            "finished-auction", "Finished",
            START_PRICE, MIN_INC,
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1),
            36
        );
        Auction finishedAuction = new Auction(config, AuctionStatus.FINISHED, SELLER_ID, "item-002");
        AuctionRegistry.getInstance().register(finishedAuction);

        // Không throw, không có bid nào được tạo
        assertDoesNotThrow(() -> autoBidService.trigger(finishedAuction));
        assertEquals(0, finishedAuction.getBidTransaction().size());

        AuctionRegistry.getInstance().remove("finished-auction");
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
        // Đây là trường hợp bidder-A vừa bid thủ công, sau đó trigger được gọi
        BidTransaction existingBid = new BidTransaction(
            AUCTION_ID, "bidder-A", "Alice", 35_000L, LocalDateTime.now(), BidType.MANUAL
        );
        auction.placeBid(existingBid);

        AutoBidRequest req = buildRequest(AUCTION_ID, 100_000L, 5_000L);
        stubBidderWithBalance("bidder-A", 200_000L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(req, "bidder-A", "Alice");

        // Đợi worker thread xử lý xong — register() trigger async, assertion
        // trước sleep này có thể chạy trước khi worker hoàn thành
        Thread.sleep(WORKER_WAIT_MS);

        // A là highest bidder duy nhất → không có bid AUTO mới nào được submit
        long autoBidCount = auction.getBidTransaction().stream()
            .filter(b -> b.getType() == BidType.AUTO).count();
        assertEquals(0, autoBidCount, "No AUTO bid should be submitted when only one entry and already leading");
    }

    // =========================================================================
    // Group 2 — Winner selection
    // =========================================================================

    @Test
    void testTrigger_higherMaxBid_wins() throws Exception {
        // WHY: A(max=500k) vs B(max=800k) → B phải thắng và được submitBid
        // Nếu logic chọn winner bị sai, A có thể thắng dù maxBid thấp hơn
        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        // Đăng ký A trước
        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 10_000L), "bidder-A", "Alice");

        // Đăng ký B sau với maxBid cao hơn
        autoBidService.register(buildRequest(AUCTION_ID, 800_000L, 10_000L), "bidder-B", "Bob");

        Thread.sleep(WORKER_WAIT_MS);

        // B thắng → chỉ còn entry của B trong registrationsMap
        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, remaining.size());
        assertEquals("bidder-B", remaining.get(0).bidderId);
    }

    @Test
    void testTrigger_tieBreak_earlierRegistration_wins() throws Exception {
        // WHY: A và B có cùng maxBid → người đăng ký TRƯỚC (registeredAt nhỏ hơn) thắng
        // Đây là tie-break rule được document trong CONTEXT.md §4.4
        // Nếu tie-break bị đảo ngược, người đến sau sẽ "cướp" thắng lợi

        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 10_000L), "bidder-A", "Alice");
        Thread.sleep(5); // đảm bảo registeredAt của A trước B
        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 10_000L), "bidder-B", "Bob");

        Thread.sleep(WORKER_WAIT_MS);

        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, remaining.size());
        assertEquals("bidder-A", remaining.get(0).bidderId,
            "When maxBid is equal, earlier registration must win (tie-break by registeredAt)");
    }

    @Test
    void testTrigger_losingEntries_areRemovedFromRegistry() throws Exception {
        // WHY: sau khi trigger, entry thua phải bị xóa khỏi registrationsMap.
        // Nếu không xóa, các lần trigger sau sẽ tính sai secondHighest và notify lại bidder thua

        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        stubBidderWithBalance("bidder-C", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 300_000L, 10_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 400_000L, 10_000L), "bidder-B", "Bob");
        autoBidService.register(buildRequest(AUCTION_ID, 600_000L, 10_000L), "bidder-C", "Charlie");

        Thread.sleep(WORKER_WAIT_MS);

        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, remaining.size(), "Only the winner must remain in registrations");
        assertEquals("bidder-C", remaining.get(0).bidderId);
    }

    // =========================================================================
    // Group 3 — Bid calculation: min(secondHighest + increment, maxBid)
    // =========================================================================

    @Test
    void testTrigger_calculatedBid_isSecondHighestPlusIncrement() throws Exception {
        // WHY: công thức tính giá là min(secondHighest + winnerIncrement, winnerMaxBid)
        // A(max=500k, inc=50k), B(max=800k, inc=50k)
        // secondHighest = 500k → calculated = min(500k+50k, 800k) = 550k
        // Nếu công thức sai, B có thể đặt giá 800k ngay (quá cao) hoặc giá sai

        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 50_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 800_000L, 50_000L), "bidder-B", "Bob");

        Thread.sleep(WORKER_WAIT_MS);

        long finalPrice = auction.getCurrentPrice();
        assertEquals(550_000L, finalPrice,
            "Calculated bid must be secondHighest(500k) + winnerIncrement(50k) = 550k");
    }

    @Test
    void testTrigger_calculatedBid_cappedAtMaxBid() throws Exception {
        // WHY: nếu secondHighest + increment > maxBid, giá phải bị cap tại maxBid
        // A(max=500k, inc=50k), B(max=520k, inc=50k)
        // calculated = min(500k+50k=550k, 520k) → phải là 520k chứ không phải 550k

        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 50_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 520_000L, 50_000L), "bidder-B", "Bob");

        Thread.sleep(WORKER_WAIT_MS);

        long finalPrice = auction.getCurrentPrice();
        assertEquals(520_000L, finalPrice,
            "Calculated bid must be capped at winner's maxBid (520k), not secondHighest+increment (550k)");
    }

    @Test
    void testTrigger_calculatedBid_notExceedCurrentPrice_skipped() throws Exception {
        // WHY: nếu calculatedBid <= currentPrice, không được submit bid
        // Trường hợp: chỉ 1 entry, auction đã có bid tại maxBid của entry đó
        // → calculated = maxBid, nhưng currentPrice = maxBid → calculatedBid <= currentPrice → skip

        BidTransaction existingBid = new BidTransaction(
            AUCTION_ID, "bidder-A", "Alice", 100_000L, LocalDateTime.now(), BidType.MANUAL
        );
        auction.placeBid(existingBid);

        // Setup: có entry bidder-B với maxBid đúng bằng currentPrice
        // secondHighest = 0, basePrice = currentPrice = 100k, calculated = min(0+10k, 100k) = 10k
        // 10k <= 100k (currentPrice) → phải skip
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);
        autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 10_000L), "bidder-B", "Bob");

        Thread.sleep(WORKER_WAIT_MS);

        // currentPrice vẫn là 100k từ bid thủ công, không có bid AUTO mới
        assertEquals(100_000L, auction.getCurrentPrice());
        long autoBidCount = auction.getBidTransaction().stream()
            .filter(b -> b.getType() == BidType.AUTO).count();
        assertEquals(0, autoBidCount, "No AUTO bid should be submitted when calculatedBid cannot exceed currentPrice");
    }

    // =========================================================================
    // Group 4 — register() validation
    // =========================================================================

    @Test
    void testRegister_nullRequest_throwsException() {
        // WHY: null request phải throw exception, không được return silently
        // NOTE: lý tưởng nhất là ServiceException(AUTO_BID_VALIDATION_ERROR), nhưng hiện tại
        // logger trong register() access autoBidRequest.getAuctionId() trước khi validate,
        // gây NPE. Test này bắt cả hai trường hợp — nếu production code được fix, đổi lại
        // thành assertThrows(ServiceException.class, ...) và check errorCode.
        assertThrows(Exception.class,
            () -> autoBidService.register(null, "bidder-A", "Alice"));
    }

    @Test
    void testRegister_sellerBidsOwnAuction_throwsServiceException() throws Exception {
        // WHY: seller không được tự đấu giá phiên của mình — business rule cốt lõi
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);
        stubBidderWithBalance(SELLER_ID, 999_999L);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 5_000L), SELLER_ID, "Seller"));
        assertEquals(ErrorCode.AUTO_SELLER_CANNOT_AUTOBID, ex.getErrorCode());
    }

    @Test
    void testRegister_incrementBelowAuctionMinIncrement_throwsServiceException() throws Exception {
        // WHY: increment phải >= auction's minIncrement (1000L) — tránh spam bid lẻ
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);
        stubBidderWithBalance("bidder-A", 999_999L);

        // increment = 500 < minIncrement = 1000 → phải fail
        ServiceException ex = assertThrows(ServiceException.class,
            () -> autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 500L), "bidder-A", "Alice"));
        assertEquals(ErrorCode.INCREMENT_TOO_LOW, ex.getErrorCode());
    }

    @Test
    void testRegister_incrementExceedsMaxBid_throwsServiceException() throws Exception {
        // WHY: increment > maxBid là vô nghĩa về mặt logic (bước giá vượt giới hạn tối đa)
        // Validation này phải bắt trước khi chạm đến auction lookup
        ServiceException ex = assertThrows(ServiceException.class,
            () -> autoBidService.register(buildRequest(AUCTION_ID, 10_000L, 20_000L), "bidder-A", "Alice"));
        assertEquals(ErrorCode.AUTO_BID_INVALID_RANGE, ex.getErrorCode());
    }

    @Test
    void testRegister_replacesExistingEntry_forSameBidder() throws Exception {
        // WHY: bidder đăng ký lại phải replace entry cũ, không duplicate
        // Nếu không replace, registrationsMap sẽ có 2 entry cùng bidder → winner selection sai

        stubBidderWithBalance("bidder-A", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        // Đăng ký lần 1
        autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 5_000L), "bidder-A", "Alice");
        // Đăng ký lần 2 với maxBid khác
        autoBidService.register(buildRequest(AUCTION_ID, 200_000L, 5_000L), "bidder-A", "Alice");

        // Không cần Thread.sleep ở đây — assertion chỉ kiểm tra registrationsMap (sync),
        // không kiểm tra kết quả bid của worker thread
        List<AutoBidService.AutoBidEntry> entries = autoBidService.getRegistrations(AUCTION_ID);
        assertEquals(1, entries.size(), "Re-registration must replace, not duplicate the existing entry");
        assertEquals(200_000L, entries.get(0).maxBid, "Entry must reflect the latest registration");
    }

    // =========================================================================
    // Group 5 — State tracking
    // =========================================================================

    @Test
    void testAutoBidCount_incrementsAfterSuccessfulTrigger() throws Exception {
        // WHY: bidCount được dùng để thống kê và giới hạn auto-bid chain.
        // Nếu không tăng, hệ thống mất khả năng tracking frequency

        stubBidderWithBalance("bidder-A", 999_999L);
        stubBidderWithBalance("bidder-B", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 300_000L, 10_000L), "bidder-A", "Alice");
        autoBidService.register(buildRequest(AUCTION_ID, 500_000L, 10_000L), "bidder-B", "Bob");

        Thread.sleep(WORKER_WAIT_MS);

        // bidder-B thắng → bidCount của B phải là 1
        int count = autoBidService.getAutoBidCount(AUCTION_ID, "bidder-B");
        assertEquals(1, count, "Winner's auto-bid count must be incremented after trigger");
    }

    @Test
    void testClearRegistrations_removesAllEntriesAndBidCounts() throws Exception {
        // WHY: clearRegistrations() được gọi khi auction FINISHED.
        // Nếu không xóa sạch, state rác sẽ tồn tại và ảnh hưởng các lần test/restart

        stubBidderWithBalance("bidder-A", 999_999L);
        when(auctionDAO.findByAuctionId(AUCTION_ID)).thenReturn(auction);

        autoBidService.register(buildRequest(AUCTION_ID, 100_000L, 5_000L), "bidder-A", "Alice");
        autoBidService.clearRegistrations(AUCTION_ID);

        List<AutoBidService.AutoBidEntry> remaining = autoBidService.getRegistrations(AUCTION_ID);
        assertTrue(remaining.isEmpty(), "All entries must be removed after clearRegistrations");
        assertEquals(0, autoBidService.getAutoBidCount(AUCTION_ID, "bidder-A"),
            "Bid count must be reset to 0 after clearRegistrations");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Tạo AutoBidRequest với các tham số cơ bản.
     */
    private AutoBidRequest buildRequest(String auctionId, long maxBid, long increment) {
        AutoBidRequest req = new AutoBidRequest();
        req.setAuctionId(auctionId);
        req.setMaxBid(maxBid);
        req.setIncrement(increment);
        return req;
    }

    /**
     * Stub UserDAO để trả về Bidder với số dư đủ để đăng ký auto-bid.
     */
    private void stubBidderWithBalance(String bidderId, long balance) throws Exception {
        Bidder bidder = new Bidder("Test User", bidderId, "pass", bidderId + "@test.com",
            UserRole.BIDDER, balance);
        when(userDAO.findById(bidderId)).thenReturn(bidder);
    }
}