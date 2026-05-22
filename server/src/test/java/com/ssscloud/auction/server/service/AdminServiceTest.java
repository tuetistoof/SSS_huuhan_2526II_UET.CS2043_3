package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.model.auction.BidTransaction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.payload.response.DTO.AdminDisplayDTO;
import com.ssscloud.auction.common.payload.response.request.AdminMetrics;
import com.ssscloud.auction.server.dao.AdminDAO;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.util.AuctionRegistry;

/**
 * Unit tests for AdminService.
 *
 * WHY: AdminService chứa các guard quan trọng cho thao tác cancel auction —
 * nếu sai, admin có thể cancel auction không tồn tại, hoặc auction đã kết thúc,
 * hoặc cancel mà không dọn sạch state (registry, auto-bid) gây data rác.
 *
 * Strategy: mock AdminDAO, AuctionDAO, AutoBidService để tránh DB.
 * Verify kết quả qua AuctionRegistry in-memory và verify các mock được gọi đúng.
 *
 * --- CHANGES vs trước ---
 *
 * FIX Bug A: Các test Group 4-6 trước đây fail vì chúng verify side effects
 *   (auctionDAO.updateStatus, registry remove, status CANCELED) ngay sau khi
 *   cancelAuction() return — trong khi doCancel() chạy async sau 10 giây.
 *
 *   Với AdminService mới (doCancel() chạy đồng bộ), không cần await/latch nữa.
 *   Tất cả verify chạy trực tiếp sau cancelAuction() và sẽ pass đúng.
 *
 *   Các test Group 4-6 đã ĐƯỢC XÓA CountDownLatch và awaitCancelCompletion() helper.
 *
 * FIX Bug C: Thêm Group 9 — double-cancel test:
 *   Verify rằng gọi cancelAuction() lần 2 trong khi lần 1 đang chạy sẽ throw
 *   ServiceException(AUCTION_CLOSED) thay vì schedule thêm 1 doCancel().
 *   Test này dùng mock để simulate doCancel() blocking đủ lâu để thread 2 vào.
 */
public class AdminServiceTest {

    private static final String AUCTION_ID = "admin-test-auction-001";
    private static final String SELLER_ID  = "seller-001";
    private static final String REASON     = "Test cancel reason";
    private static final String BIDDER_ID  = "bidder-001";
    private static final String BIDDER2_ID = "bidder-002";

    private AdminDAO       adminDAO;
    private AuctionDAO     auctionDAO;
    private AutoBidService autoBidService;
    private UserDAO        userDAO;
    private AdminService   adminService;
    private Auction        auction;

    @BeforeEach
    void setUp() {
        adminDAO       = mock(AdminDAO.class);
        auctionDAO     = mock(AuctionDAO.class);
        autoBidService = mock(AutoBidService.class);
        userDAO        = mock(UserDAO.class);

        adminService = new AdminService(adminDAO, auctionDAO, autoBidService, userDAO);

        AuctionConfig config = new AuctionConfig(
            AUCTION_ID, "Test Auction",
            30_000L, 1_000L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        auction = new Auction(config, AuctionStatus.RUNNING, SELLER_ID, "item-001");
        AuctionRegistry.getInstance().registerIfAbsent(auction);

        try {
            ConcurrentBidManager.initialize(userDAO, null, autoBidService, auctionDAO, null);
        } catch (Exception e) {
            fail("Failed to initialize ConcurrentBidManager: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        AuctionRegistry.getInstance().remove(AUCTION_ID);
        ConcurrentBidManager.resetInstance();
    }

    // =========================================================================
    // Group 1 — getAuctions
    // =========================================================================

    @Test
    void testGetAuctions_noFilter_returnsAll() throws Exception {
        // WHY: filter == null phải trả toàn bộ auction, không bị giới hạn status
        List<AdminDisplayDTO> mockList = List.of(
            new AdminDisplayDTO(AUCTION_ID, "Test", "seller", 30_000L, AuctionStatus.RUNNING,
                LocalDateTime.now().plusHours(2))
        );
        when(adminDAO.findAllAuctions(null)).thenReturn(mockList);

        List<AdminDisplayDTO> result = adminService.getAuctions(null);

        assertEquals(1, result.size());
        verify(adminDAO).findAllAuctions(null);
    }

    @Test
    void testGetAuctions_withFilter_passesFilterToDAO() throws Exception {
        // WHY: filter RUNNING phải được truyền xuống DAO đúng — không bị bỏ qua
        when(adminDAO.findAllAuctions(AuctionStatus.RUNNING)).thenReturn(List.of());

        adminService.getAuctions(AuctionStatus.RUNNING);

        verify(adminDAO).findAllAuctions(AuctionStatus.RUNNING);
    }

    @Test
    void testGetAuctions_daoThrows_propagatesException() throws Exception {
        // WHY: nếu DB lỗi, exception phải được rethrow để caller biết — không được nuốt
        when(adminDAO.findAllAuctions(any())).thenThrow(new RuntimeException("DB down"));

        assertThrows(Exception.class, () -> adminService.getAuctions(null));
    }

    // =========================================================================
    // Group 2 — getMetrics
    // =========================================================================

    @Test
    void testGetMetrics_returnsCorrectValues() throws Exception {
        // WHY: 3 con số trên metric card phải đúng — sai thì dashboard hiển thị sai
        AdminMetrics mockMetrics = new AdminMetrics(5, 12, 30);
        when(adminDAO.getMetrics()).thenReturn(mockMetrics);

        AdminMetrics result = adminService.getMetrics();

        assertEquals(5,  result.getRunningCount());
        assertEquals(12, result.getEndedCount());
        assertEquals(30, result.getTotalUsers());
    }

    @Test
    void testGetMetrics_daoThrows_propagatesException() throws Exception {
        // WHY: DB lỗi không được return null hay AdminMetrics rỗng — phải throw
        when(adminDAO.getMetrics()).thenThrow(new RuntimeException("DB down"));

        assertThrows(Exception.class, () -> adminService.getMetrics());
    }

    // =========================================================================
    // Group 3 — cancelAuction: guard conditions
    // =========================================================================

    @Test
    void testCancelAuction_nullAuctionId_throwsServiceException() {
        // WHY: auctionId null phải fail ngay tại validation, không chạm DB
        ServiceException ex = assertThrows(ServiceException.class,
            () -> adminService.cancelAuction(null, REASON));

        assertEquals(ErrorCode.INVALID_AUCTION_ID, ex.getErrorCode());
        verifyNoInteractions(auctionDAO);
    }

    @Test
    void testCancelAuction_blankAuctionId_throwsServiceException() {
        // WHY: auctionId blank (khoảng trắng) cũng phải bị reject
        ServiceException ex = assertThrows(ServiceException.class,
            () -> adminService.cancelAuction("   ", REASON));

        assertEquals(ErrorCode.INVALID_AUCTION_ID, ex.getErrorCode());
    }

    @Test
    void testCancelAuction_auctionNotInRegistry_throwsServiceException() {
        // WHY: auction không tồn tại trong AuctionRegistry = đã kết thúc hoặc không hợp lệ
        ServiceException ex = assertThrows(ServiceException.class,
            () -> adminService.cancelAuction("non-existent-id", REASON));

        assertEquals(ErrorCode.AUCTION_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(auctionDAO);
    }

    @Test
    void testCancelAuction_alreadyFinished_throwsServiceException() throws Exception {
        // WHY: auction đã FINISHED không được cancel — status isActive() == false
        AuctionConfig config = new AuctionConfig(
            "finished-001", "Finished Auction",
            30_000L, 1_000L,
            LocalDateTime.now().minusHours(3),
            LocalDateTime.now().minusHours(1),
            36
        );
        Auction finishedAuction = new Auction(config, AuctionStatus.FINISHED, SELLER_ID, "item-002");
        AuctionRegistry.getInstance().registerIfAbsent(finishedAuction);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> adminService.cancelAuction("finished-001", REASON));

        assertEquals(ErrorCode.AUCTION_CLOSED, ex.getErrorCode());
        verifyNoInteractions(auctionDAO);

        AuctionRegistry.getInstance().remove("finished-001");
    }

    // =========================================================================
    // Group 4 — cancelAuction: happy path
    //
    // FIX Bug A: doCancel() bây giờ chạy ĐỒNG BỘ trong cancelAuction().
    // Không cần CountDownLatch hay awaitCancelCompletion() helper nữa.
    // Verify ngay sau cancelAuction() return là đủ và đúng.
    // =========================================================================

    @Test
    void testCancelAuction_success_updatesStatusInDB() throws Exception {
        // WHY: DB phải được cập nhật CANCELED — nếu không, sau khi server restart
        // auction sẽ hiện lại như chưa bị cancel
        adminService.cancelAuction(AUCTION_ID, REASON);

        // FIX: không cần await — verify chạy ngay sau return vì doCancel() đồng bộ
        verify(auctionDAO).updateStatus(AUCTION_ID, AuctionStatus.CANCELED);
    }

    @Test
    void testCancelAuction_success_removesFromRegistry() throws Exception {
        // WHY: sau cancel auction không được còn trong AuctionRegistry —
        // nếu còn thì bidder vẫn có thể gửi bid vào auction đã bị cancel
        adminService.cancelAuction(AUCTION_ID, REASON);

        // FIX: verify ngay — không cần latch
        assertNull(AuctionRegistry.getInstance().getLiveAuction(AUCTION_ID),
            "Auction must be removed from AuctionRegistry after cancel");
    }

    @Test
    void testCancelAuction_success_clearsAutoBidRegistrations() throws Exception {
        // WHY: auto-bid entry phải được dọn — nếu không AutoBidService có thể
        // trigger bid trên auction đã CANCELED.
        // Note: clearRegistrations() được gọi 2 lần:
        //   1. Trong cancelAuction() trước khi doCancel() — softClose phase
        //   2. Trong cleanupCanceledAuction() sau DB update — cleanup phase
        // atLeastOnce() đảm bảo bước cleanup chạy
        adminService.cancelAuction(AUCTION_ID, REASON);

        verify(autoBidService, atLeastOnce()).clearRegistrations(AUCTION_ID);
    }

    @Test
    void testCancelAuction_success_updatesAuctionStatusInMemory() throws Exception {
        // WHY: object Auction in-memory phải có status CANCELED ngay lập tức —
        // tránh race condition nếu có thread khác đọc status trước khi xóa khỏi registry
        adminService.cancelAuction(AUCTION_ID, REASON);

        // FIX: verify ngay — không cần latch
        assertEquals(AuctionStatus.CANCELED, auction.getStatus(),
            "Auction in-memory status must be CANCELED after cancelAuction()");
    }

    @Test
    void testCancelAuction_nullReason_doesNotThrow() throws Exception {
        // WHY: reason có thể null (admin bấm confirm mà không nhập lý do) —
        // không được để NPE, broadcastAuctionCanceled phải handle null reason
        assertDoesNotThrow(() -> adminService.cancelAuction(AUCTION_ID, null));
    }

    @Test
    void testCancelAuction_dbUpdateFails_throwsException() throws Exception {
        // WHY: nếu DB lỗi khi updateStatus, phải throw và không tiếp tục các bước sau
        doThrow(new RuntimeException("DB error"))
            .when(auctionDAO).updateStatus(any(), any());

        assertThrows(Exception.class,
            () -> adminService.cancelAuction(AUCTION_ID, REASON));

        // FIX: kiểm tra ngay sau throw — không cần await
        // Registry phải còn auction vì cancel chưa hoàn tất
        assertNotNull(AuctionRegistry.getInstance().getLiveAuction(AUCTION_ID),
            "Auction must remain in registry if DB update fails");

        // Status in-memory không được thay đổi vì doCancel() đã rollback
        assertEquals(AuctionStatus.RUNNING, auction.getStatus(),
            "Auction status must remain RUNNING if DB update fails");
    }

    @Test
    void testCancelAuction_openAuction_succeeds() throws Exception {
        // WHY: auction ở trạng thái OPEN (chưa có bid nào) cũng phải cancel được —
        // isActive() = true với cả OPEN
        AuctionConfig config = new AuctionConfig(
            "open-001", "Open Auction",
            20_000L, 500L,
            LocalDateTime.now().plusMinutes(5),
            LocalDateTime.now().plusHours(1),
            30
        );
        Auction openAuction = new Auction(config, AuctionStatus.OPEN, SELLER_ID, "item-003");
        AuctionRegistry.getInstance().registerIfAbsent(openAuction);

        adminService.cancelAuction("open-001", REASON);

        // FIX: verify ngay — không cần latch
        assertEquals(AuctionStatus.CANCELED, openAuction.getStatus(),
            "OPEN auction must be CANCELED after cancelAuction()");
        assertNull(AuctionRegistry.getInstance().getLiveAuction("open-001"),
            "OPEN auction must be removed from registry after cancel");

        AuctionRegistry.getInstance().remove("open-001"); // cleanup nếu test fail trước đó
    }

    // =========================================================================
    // Group 5 — refundWinner: balance unlock
    // =========================================================================

    @Test
    void testCancelAuction_withAutoBid_refundsLockAmountNotCurrentPrice() throws Exception {
        // Simulate auto-bid: lockAmount = maxBid = 200_000, nhưng calculatedBid = 130_000
        long maxBid = 200_000L;
        long calculatedBid = 130_000L;

        CountDownLatch bidProcessed = new CountDownLatch(1);
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(anyString(), anyLong())).thenAnswer(inv -> {
            bidProcessed.countDown();
            return true;
        });

        ConcurrentBidManager.getInstance().submitBid(
            auction, BIDDER_ID, "bidder-user",
            calculatedBid, maxBid, BidType.AUTO
        );

        assertTrue(bidProcessed.await(3, TimeUnit.SECONDS));

        reset(userDAO);
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(anyString(), anyLong())).thenReturn(true);

        // FIX: không cần CountDownLatch để chờ cancel — doCancel() đồng bộ
        adminService.cancelAuction(AUCTION_ID, REASON);

        // Phải unlock maxBid (200_000), không phải calculatedBid (130_000)
        verify(userDAO).unlockBidderBalance(BIDDER_ID, maxBid);
        verify(userDAO, never()).unlockBidderBalance(BIDDER_ID, calculatedBid);
    }

    @Test
    void testCancelAuction_withActiveBid_revertsSellerPendingBalance() throws Exception {
        // WHY: khi cancel, seller phải bị trừ lại pending balance bằng winningBid.
        auction.placeBid(new BidTransaction(
            AUCTION_ID, BIDDER_ID, "bidder-user",
            50_000L, 50_000L, LocalDateTime.now().minusMinutes(10), BidType.MANUAL
        ));

        // FIX: không cần latch — verify ngay
        adminService.cancelAuction(AUCTION_ID, REASON);

        verify(userDAO).updatePendingBalance(SELLER_ID, -50_000L);
    }

    @Test
    void testCancelAuction_noBids_doesNotCallUnlock() throws Exception {
        // WHY: auction chưa có bid → winnerId = null → refundWinner() phải skip hoàn toàn.
        adminService.cancelAuction(AUCTION_ID, REASON);

        verify(userDAO, never()).unlockBidderBalance(any(), anyLong());
        verify(userDAO, never()).updatePendingBalance(any(), anyLong());
    }

    @Test
    void testCancelAuction_withMultipleBids_refundsOnlyCurrentWinner() throws Exception {
        // WHY: refundWinner chỉ hoàn tiền cho winner hiện tại (highest bidder).
        auction.placeBid(new BidTransaction(AUCTION_ID, BIDDER_ID,  "bidder1", 50_000L, 50_000L, LocalDateTime.now(), BidType.MANUAL));
        auction.placeBid(new BidTransaction(AUCTION_ID, BIDDER2_ID, "bidder2", 55_000L, 55_000L, LocalDateTime.now(), BidType.MANUAL));

        // FIX: không cần latch
        adminService.cancelAuction(AUCTION_ID, REASON);

        verify(userDAO, times(1)).unlockBidderBalance(BIDDER2_ID, 55_000L);
        verify(userDAO, never()).unlockBidderBalance(eq(BIDDER_ID), anyLong());
    }

    // =========================================================================
    // Group 6 — cancelAuction: ConcurrentBidManager.shutdown() được gọi
    // =========================================================================

    @Test
    void testCancelAuction_success_shutsDownBidWorker() throws Exception {
        // WHY: sau cancel, closedAuctions phải chứa auctionId này.
        // Verify gián tiếp: submitBid() với cùng auctionId phải throw.

        // FIX: không cần latch — verify ngay sau cancel
        adminService.cancelAuction(AUCTION_ID, REASON);

        AuctionConfig config = new AuctionConfig(
            AUCTION_ID, "Test Auction",
            30_000L, 1_000L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        Auction sameIdAuction = new Auction(config, AuctionStatus.RUNNING, SELLER_ID, "item-001");

        assertThrows(Exception.class,
            () -> ConcurrentBidManager.getInstance()
                    .submitBid(sameIdAuction, BIDDER_ID, "bidder-user", 40_000L, 40_000L, BidType.MANUAL),
            "submitBid sau cancel/shutdown phải throw — closedAuctions guard phải active");
    }

    // =========================================================================
    // Group 7 — cancelAuction: trạng thái không hợp lệ
    // =========================================================================

    @Test
    void testCancelAuction_alreadyCanceled_throwsServiceException() throws Exception {
        // WHY: cancel auction đã CANCELED phải bị reject — tránh double-refund
        AuctionConfig config = new AuctionConfig(
            "canceled-001", "Canceled Auction",
            30_000L, 1_000L,
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1),
            36
        );
        Auction canceledAuction = new Auction(config, AuctionStatus.CANCELED, SELLER_ID, "item-004");
        AuctionRegistry.getInstance().registerIfAbsent(canceledAuction);
        ServiceException ex = assertThrows(ServiceException.class,
            () -> adminService.cancelAuction("canceled-001", REASON));

        assertEquals(ErrorCode.AUCTION_CLOSED, ex.getErrorCode());
        AuctionRegistry.getInstance().remove("canceled-001");
    }

    @Test
    void testCancelAuction_paidAuction_throwsServiceException() throws Exception {
        // WHY: auction đã PAID không thể cancel — tiền đã chuyển, không có gì để refund
        AuctionConfig config = new AuctionConfig(
            "paid-001", "Paid Auction",
            30_000L, 1_000L,
            LocalDateTime.now().minusHours(3),
            LocalDateTime.now().minusHours(2),
            36
        );
        Auction paidAuction = new Auction(config, AuctionStatus.PAID, SELLER_ID, "item-005");
        AuctionRegistry.getInstance().registerIfAbsent(paidAuction);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> adminService.cancelAuction("paid-001", REASON));

        assertEquals(ErrorCode.AUCTION_CLOSED, ex.getErrorCode());
        verifyNoInteractions(auctionDAO);

        AuctionRegistry.getInstance().remove("paid-001");
    }

    // =========================================================================
    // Group 8 — cancelAuction: reason edge cases
    // =========================================================================

    @Test
    void testCancelAuction_blankReason_doesNotThrow() throws Exception {
        // WHY: blank reason phải được chấp nhận — broadcastAuctionCanceled handle blank
        assertDoesNotThrow(() -> adminService.cancelAuction(AUCTION_ID, "   "));
    }

    @Test
    void testCancelAuction_refundWinnerFails_throwsException() throws Exception {
        // WHY: nếu DB lỗi khi refundWinner, phải throw — không được nuốt exception
        // vì tiền bidder chưa được hoàn
        auction.placeBid(new BidTransaction(
            AUCTION_ID, BIDDER_ID, "bidder-user",
            50_000L, 50_000L, LocalDateTime.now().minusMinutes(5), BidType.MANUAL
        ));

        doThrow(new RuntimeException("DB error"))
            .when(userDAO).unlockBidderBalance(anyString(), anyLong());

        assertThrows(Exception.class,
            () -> adminService.cancelAuction(AUCTION_ID, REASON));
    }

    // =========================================================================
    // Group 9 — cancelAuction: double-cancel guard (FIX Bug C)
    // =========================================================================

    /**
     * FIX Bug C: Verify rằng gọi cancelAuction() lần 2 với cùng auctionId
     * trong khi lần 1 đang xử lý sẽ bị reject ngay lập tức với AUCTION_CLOSED,
     * thay vì chạy thêm một doCancel() nữa.
     *
     * Strategy: mock auctionDAO.updateStatus() để block lần đầu đủ lâu
     * (dùng CountDownLatch), sau đó chạy cancel lần 2 từ thread khác và
     * expect nó throw ServiceException ngay.
     */
    @Test
    void testCancelAuction_doubleCancel_secondCallThrows() throws Exception {
        // Latch để điều phối: lần 1 block ở updateStatus, lần 2 chạy và bị reject
        CountDownLatch firstCancelStarted  = new CountDownLatch(1);
        CountDownLatch secondCancelTested  = new CountDownLatch(1);

        doAnswer(inv -> {
            firstCancelStarted.countDown();         // báo lần 1 đã vào updateStatus
            secondCancelTested.await(3, TimeUnit.SECONDS); // đợi lần 2 được test xong
            return null;
        }).when(auctionDAO).updateStatus(eq(AUCTION_ID), eq(AuctionStatus.CANCELED));

        // Thread 1: chạy cancel lần 1 — sẽ block tại updateStatus
        Thread cancelThread1 = new Thread(() -> {
            try {
                adminService.cancelAuction(AUCTION_ID, REASON);
            } catch (Exception ignored) { /* lần 1 có thể throw sau khi thread 2 signal */ }
        });
        cancelThread1.start();

        // Đợi lần 1 vào updateStatus (đã qua guard và đang trong doCancel)
        assertTrue(firstCancelStarted.await(3, TimeUnit.SECONDS),
            "Thread 1 must reach updateStatus within 3s");

        // Thread chính: thử cancel lần 2 — phải throw ngay vì cancelInProgress đã chứa auctionId
        ServiceException ex = assertThrows(ServiceException.class,
            () -> adminService.cancelAuction(AUCTION_ID, REASON),
            "Second cancel call must throw immediately while first is in progress");

        assertEquals(ErrorCode.AUCTION_CLOSED, ex.getErrorCode(),
            "Second cancel must fail with AUCTION_CLOSED");

        // verify: auctionDAO.updateStatus chỉ được gọi đúng 1 lần (không double-cancel)
        // Lúc này lần 1 vẫn block — chưa gọi lần nào hoàn tất, nhưng đang gọi
        // Unblock lần 1 và chờ xong
        secondCancelTested.countDown();
        cancelThread1.join(5000);

        // updateStatus phải chỉ được gọi đúng 1 lần — không có lần gọi thứ 2
        verify(auctionDAO, times(1)).updateStatus(AUCTION_ID, AuctionStatus.CANCELED);
    }

    @Test
    void testCancelAuction_afterSuccessfulCancel_secondCallThrowsNotFound() throws Exception {
        // WHY: sau khi cancel xong, auction bị xóa khỏi registry.
        // Gọi lần 2 phải throw AUCTION_NOT_FOUND (không còn trong registry)
        // thay vì AUCTION_CLOSED hay double-cancel.
        adminService.cancelAuction(AUCTION_ID, REASON);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> adminService.cancelAuction(AUCTION_ID, REASON));

        assertEquals(ErrorCode.AUCTION_NOT_FOUND, ex.getErrorCode(),
            "After successful cancel, auction no longer in registry — must throw NOT_FOUND");
    }
}