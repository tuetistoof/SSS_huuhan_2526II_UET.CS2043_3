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
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.common.dto.response.AdminDisplayDTO;
import com.ssscloud.auction.common.dto.response.AdminMetrics;
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
        AuctionRegistry.getInstance().register(auction);

        try {
            ConcurrentBidManager.initialize(userDAO, null, autoBidService, auctionDAO, null);
        } catch (Exception e) {
            fail("Failed to initialize ConcurrentBidManager: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        AuctionRegistry.getInstance().remove(AUCTION_ID);
        // Reset closedAuctions để không rò sang test khác
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
        AuctionRegistry.getInstance().register(finishedAuction);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> adminService.cancelAuction("finished-001", REASON));

        assertEquals(ErrorCode.AUCTION_CLOSED, ex.getErrorCode());
        verifyNoInteractions(auctionDAO);

        AuctionRegistry.getInstance().remove("finished-001");
    }

    // =========================================================================
    // Group 4 — cancelAuction: happy path
    // =========================================================================

    @Test
    void testCancelAuction_success_updatesStatusInDB() throws Exception {
        // WHY: DB phải được cập nhật CANCELED — nếu không, sau khi server restart
        // auction sẽ hiện lại như chưa bị cancel
        adminService.cancelAuction(AUCTION_ID, REASON);

        verify(auctionDAO).updateStatus(AUCTION_ID, AuctionStatus.CANCELED);
    }

    @Test
    void testCancelAuction_success_removesFromRegistry() throws Exception {
        // WHY: sau cancel auction không được còn trong AuctionRegistry —
        // nếu còn thì bidder vẫn có thể gửi bid vào auction đã bị cancel
        adminService.cancelAuction(AUCTION_ID, REASON);

        assertNull(AuctionRegistry.getInstance().getLiveAuction(AUCTION_ID),
            "Auction must be removed from AuctionRegistry after cancel");
    }

    @Test
    void testCancelAuction_success_clearsAutoBidRegistrations() throws Exception {
        // WHY: auto-bid entry phải được dọn — nếu không AutoBidService có thể
        // trigger bid trên auction đã CANCELED
        adminService.cancelAuction(AUCTION_ID, REASON);

        verify(autoBidService).clearRegistrations(AUCTION_ID);
    }

    @Test
    void testCancelAuction_success_updatesAuctionStatusInMemory() throws Exception {
        // WHY: object Auction in-memory phải có status CANCELED ngay lập tức —
        // tránh race condition nếu có thread khác đọc status trước khi xóa khỏi registry
        adminService.cancelAuction(AUCTION_ID, REASON);

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

        // Registry phải còn auction vì cancel chưa hoàn tất
        assertNotNull(AuctionRegistry.getInstance().getLiveAuction(AUCTION_ID),
            "Auction must remain in registry if DB update fails");

        // Status in-memory không được thay đổi vì auction.cancel() chưa được gọi
        assertEquals(AuctionStatus.RUNNING, auction.getStatus(),
            "Auction status must remain RUNNING if DB update fails");

        // clearRegistrations không được gọi nếu exception xảy ra trước bước đó
        verify(autoBidService, never()).clearRegistrations(any());
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
        AuctionRegistry.getInstance().register(openAuction);

        adminService.cancelAuction("open-001", REASON);

        assertEquals(AuctionStatus.CANCELED, openAuction.getStatus(),
            "OPEN auction must be CANCELED after cancelAuction()");
        assertNull(AuctionRegistry.getInstance().getLiveAuction("open-001"),
            "OPEN auction must be removed from registry after cancel");

        AuctionRegistry.getInstance().remove("open-001");
    }

    // =========================================================================
    // Group 5 — refundWinner: balance unlock
    // =========================================================================

    @Test
    void testCancelAuction_withAutoBid_refundsLockAmountNotCurrentPrice() throws Exception {
        // Simulate auto-bid: lockAmount = maxBid = 200_000, nhưng calculatedBid = 130_000
        // Submit qua ConcurrentBidManager để populate committedWinnerLockAmount
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
            calculatedBid, maxBid, BidType.AUTO  // bidAmount != lockAmount
        );

        assertTrue(bidProcessed.await(3, TimeUnit.SECONDS));

        // Reset mock để verify sạch
        reset(userDAO);
        when(userDAO.unlockBidderBalance(anyString(), anyLong())).thenReturn(true);
        when(userDAO.updatePendingBalance(anyString(), anyLong())).thenReturn(true);

        adminService.cancelAuction(AUCTION_ID, REASON);

        // Phải unlock maxBid (200_000), không phải calculatedBid (130_000)
        verify(userDAO).unlockBidderBalance(BIDDER_ID, maxBid);
        verify(userDAO, never()).unlockBidderBalance(BIDDER_ID, calculatedBid);
    }

    @Test
    void testCancelAuction_withActiveBid_revertsSellerPendingBalance() throws Exception {
        // WHY: khi cancel, seller phải bị trừ lại pending balance bằng winningBid.
        // refundWinner() gọi updatePendingBalance(sellerId, -winningBid).
        auction.placeBid(new BidTransaction(
            AUCTION_ID, BIDDER_ID, "bidder-user",
            50_000L, 50_000L, LocalDateTime.now().minusMinutes(10), BidType.MANUAL
        ));

        adminService.cancelAuction(AUCTION_ID, REASON);

        verify(userDAO).updatePendingBalance(SELLER_ID, -50_000L);
    }

    @Test
    void testCancelAuction_noBids_doesNotCallUnlock() throws Exception {
        // WHY: auction chưa có bid → winnerId = null → refundWinner() phải skip hoàn toàn.
        // Không được gọi unlockBidderBalance hay updatePendingBalance khi chưa có ai bid.
        adminService.cancelAuction(AUCTION_ID, REASON);

        verify(userDAO, never()).unlockBidderBalance(any(), anyLong());
        verify(userDAO, never()).updatePendingBalance(any(), anyLong());
    }

    @Test
    void testCancelAuction_withMultipleBids_refundsOnlyCurrentWinner() throws Exception {
        // WHY: refundWinner chỉ hoàn tiền cho winner hiện tại (highest bidder).
        // Các bidder đã bị outbid trước đó đã được unlock trong processTask rồi —
        // cancel không được unlock lại họ (double-unlock).
        auction.placeBid(new BidTransaction(AUCTION_ID, BIDDER_ID, "bidder1", 50_000L, 50_000L, LocalDateTime.now(), BidType.MANUAL));
        auction.placeBid(new BidTransaction(AUCTION_ID, BIDDER2_ID, "bidder2", 55_000L, 55_000L, LocalDateTime.now(), BidType.MANUAL));

        adminService.cancelAuction(AUCTION_ID, REASON);

        // Chỉ unlock BIDDER2 (winner hiện tại) với giá 55_000
        verify(userDAO, times(1)).unlockBidderBalance(BIDDER2_ID, 55_000L);
        // BIDDER1 không được unlock lại ở đây
        verify(userDAO, never()).unlockBidderBalance(eq(BIDDER_ID), anyLong());
    }

    // =========================================================================
    // Group 6 — cancelAuction: ConcurrentBidManager.shutdown() được gọi
    // =========================================================================

    @Test
    void testCancelAuction_success_shutsDownBidWorker() throws Exception {
        // WHY: sau cancel, closedAuctions phải chứa auctionId này.
        // Verify gián tiếp: submitBid() với cùng auctionId phải throw.
        adminService.cancelAuction(AUCTION_ID, REASON);

        // Tạo lại auction object với cùng ID (auction gốc đã bị cancel)
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
        AuctionRegistry.getInstance().register(canceledAuction);

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
        AuctionRegistry.getInstance().register(paidAuction);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> adminService.cancelAuction("paid-001", REASON));

        assertEquals(ErrorCode.AUCTION_CLOSED, ex.getErrorCode());
        // auctionDAO không được chạm trước khi qua guard isActive()
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
}