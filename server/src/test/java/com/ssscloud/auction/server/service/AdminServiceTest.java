package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.server.service.ConcurrentBidManager;
import com.ssscloud.auction.common.dto.response.AdminDisplayDTO;
import com.ssscloud.auction.common.dto.response.AdminMetrics;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
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
    private static final String REASON     = "Vi phạm điều khoản";

    private AdminService  adminService;
    private AdminDAO      adminDAO;
    private AuctionDAO    auctionDAO;
    private AutoBidService autoBidService;
    private UserDAO       userDAO;
    private Auction        auction;

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    private static final String BIDDER_ID  = "bidder-001";
    private static final String BIDDER2_ID = "bidder-002";


    @BeforeEach
    void setUp() {
         adminDAO       = mock(AdminDAO.class);
        auctionDAO     = mock(AuctionDAO.class);
        autoBidService = mock(AutoBidService.class);
        userDAO        = mock(UserDAO.class);
    
        adminService = new AdminService(adminDAO, auctionDAO, autoBidService, userDAO);
    
        // Auction mặc định: RUNNING, đăng ký vào AuctionRegistry
        AuctionConfig config = new AuctionConfig(
            AUCTION_ID, "Test Auction",
            30_000L, 1_000L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(2),
            36
        );
        auction = new Auction(config, AuctionStatus.RUNNING, SELLER_ID, "item-001");
        AuctionRegistry.getInstance().register(auction);
    
        // Khởi tạo ConcurrentBidManager với mock DAO để cancelAuction → shutdown()
        // không NPE khi gọi ConcurrentBidManager.getInstance().shutdown(auctionId)
        try {
            ConcurrentBidManager.initialize(userDAO, null, autoBidService, auctionDAO, null);
        } catch (Exception e) {
            fail("Failed to initialize ConcurrentBidManager with mock DAOs: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        // Dọn sạch AuctionRegistry sau mỗi test để tránh ảnh hưởng chéo
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
        // Không được cancel auction không còn active trong memory
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
    
        // Status in-memory không được thay đổi vì cancel() chưa được gọi
        assertEquals(AuctionStatus.RUNNING, auction.getStatus(),
            "Auction status must remain RUNNING if DB update fails");
    
        // clearRegistrations không được gọi nếu chưa qua bước đó
        verify(autoBidService, never()).clearRegistrations(any());

        // WHY: refundWinner() nằm SAU updateStatus() trong luồng — nếu DB throw thì
        // refundWinner() không được chạy, tránh unlock balance của auction chưa bị cancel.
        verify(userDAO, never()).unlockBidderBalance(any(), anyLong());
        verify(userDAO, never()).updatePendingBalance(any(), anyLong());
    }

    @Test
    void testCancelAuction_openAuction_succeeds() throws Exception {
        // WHY: auction ở trạng thái OPEN (chưa có bid nào) cũng phải cancel được —
        // không chỉ RUNNING. isActive() = true với cả OPEN.
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
        // cancelAuction() đã gọi AuctionRegistry.remove() — không cần cleanup thủ công ở đây
    }
    
    
    // ── Group 5 — refundWinner: balance unlock ────────────────────────────────────
    
    @Test
    void testCancelAuction_withActiveBid_refundsWinner() throws Exception {
        // WHY: khi auction bị cancel mà đã có bid, winner (người đang giữ giá cao nhất)
        // phải được unlock balance — userDAO.unlockBidderBalance() phải được gọi đúng số tiền.
        BidTransaction bid = new BidTransaction(
            AUCTION_ID, BIDDER_ID, "bidder-user",
            50_000L, 40_000L, LocalDateTime.now().minusMinutes(10),
            BidType.MANUAL
        );
        auction.placeBid(bid);  // auction.getHighestBidderId() = BIDDER_ID, getCurrentPrice() = 50_000
    
        adminService.cancelAuction(AUCTION_ID, REASON);
    
        verify(userDAO).unlockBidderBalance(BIDDER_ID, 50_000L);
    }
    
    @Test
    void testCancelAuction_withActiveBid_revertsSellerPendingBalance() throws Exception {
        // WHY: khi auction bị cancel, seller phải được trừ lại pending balance đúng số tiền —
        // vì khi bid thắng seller được cộng pending, cancel thì phải hoàn lại.
        BidTransaction bid = new BidTransaction(
            AUCTION_ID, BIDDER_ID, "bidder-user",
            50_000L, 45_000L, LocalDateTime.now().minusMinutes(10),
            BidType.MANUAL
        );
        auction.placeBid(bid);
    
        adminService.cancelAuction(AUCTION_ID, REASON);
    
        verify(userDAO).updatePendingBalance(SELLER_ID, -50_000L);
    }
    
    @Test
    void testCancelAuction_noBids_doesNotCallUnlock() throws Exception {
        // WHY: nếu auction chưa có bid nào thì winnerId = null và currentPrice = startPrice
        // (không phải giá bid). refundWinner() phải bỏ qua — không được gọi unlockBidderBalance.
        // Đây là kiểm tra guard condition trong refundWinner().
        adminService.cancelAuction(AUCTION_ID, REASON);
    
        verify(userDAO, never()).unlockBidderBalance(any(), anyLong());
        verify(userDAO, never()).updatePendingBalance(any(), anyLong());
    }
    
    
    // ── Group 6 — cancelAuction: ConcurrentBidManager.shutdown() được gọi ────────
    
    @Test
    void testCancelAuction_success_shutsDownBidWorker() throws Exception {
        // WHY: sau khi cancel, ConcurrentBidManager phải đánh dấu auctionId là closed
        // để submitBid() tiếp theo bị chặn. Cách verify gián tiếp: submit một bid sau cancel,
        // kiểm tra nó throw ServiceException thay vì được enqueue.
        adminService.cancelAuction(AUCTION_ID, REASON);
    
        // Sau shutdown(), submitBid vào cùng auctionId phải throw (auction is closed)
        // Dùng auction mới với cùng ID để tránh NPE từ status check trong submitBid
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
                    .submitBid(sameIdAuction, BIDDER_ID, "bidder-user", 40_000L, 30_000L, BidType.MANUAL),
            "submitBid after cancel/shutdown must throw — closedAuctions guard must be active");
    }
    
    
    // ── Group 7 — cancelAuction: CANCELED auction bị cancel lần 2 ────────────────
    
    @Test
    void testCancelAuction_alreadyCanceled_throwsServiceException() throws Exception {
        // WHY: cancel auction đã CANCELED (isActive() = false) phải bị reject —
        // tránh double-refund hoặc double-clear.
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
    
    
    // ── Group 8 — cancelAuction: PAID auction ────────────────────────────────────
    
    @Test
    void testCancelAuction_paidAuction_throwsServiceException() throws Exception {
        // WHY: auction đã PAID (đã thanh toán xong) không thể cancel —
        // tiền đã chuyển, không có gì để refund.
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
        verifyNoInteractions(auctionDAO);
    
        AuctionRegistry.getInstance().remove("paid-001");
    }
    
    
    // ── Group 9 — cancelAuction: blank reason ────────────────────────────────────
    
    @Test
    void testCancelAuction_blankReason_doesNotThrow() throws Exception {
        // WHY: reason "" (chuỗi rỗng, khác null) cũng phải được chấp nhận —
        // broadcastAuctionCanceled phải handle cả null lẫn blank.
        assertDoesNotThrow(() -> adminService.cancelAuction(AUCTION_ID, "   "));
    }


    // ── Group 10 — race condition: cancelAuction ‖ clearRegistrations ─────────────

    @Test
    void testCancelAuction_success_clearRegistrationsCalledAfterStatusUpdate() throws Exception {
        // WHY: thứ tự quan trọng — clearRegistrations() phải được gọi SAU khi auction đã
        // bị đánh dấu CANCELED (auction.cancel()) và đã bị xóa khỏi registry.
        // Nếu đảo ngược, AutoBidService.trigger() có thể fire thêm bid trên auction đã cancel.
        //
        // Verify thứ tự dùng InOrder: updateStatus → (cancel in-memory) → clearRegistrations
        org.mockito.InOrder inOrder = inOrder(auctionDAO, autoBidService);

        adminService.cancelAuction(AUCTION_ID, REASON);

        inOrder.verify(auctionDAO).updateStatus(AUCTION_ID, AuctionStatus.CANCELED);
        inOrder.verify(autoBidService).clearRegistrations(AUCTION_ID);
    }

    @Test
    void testCancelAuction_success_registryRemovedBeforeClearRegistrations() throws Exception {
        // WHY: AuctionRegistry.remove() phải xảy ra trước clearRegistrations().
        // Nếu clearRegistrations() chạy trước, AutoBidService.trigger() (được gọi bởi
        // register() đồng thời) sẽ thấy registrationsMap rỗng nhưng registry vẫn còn
        // auction → có thể submit bid vào auction sắp bị xóa.
        //
        // Dùng custom answer để capture thời điểm clearRegistrations() được gọi,
        // sau đó kiểm tra registry đã không còn auction đó.
        org.mockito.stubbing.Answer<Void> captureRegistryState = invocation -> {
            // Tại thời điểm clearRegistrations() chạy, registry phải đã không còn auction
            assertNull(
                AuctionRegistry.getInstance().getLiveAuction(AUCTION_ID),
                "AuctionRegistry must be cleared BEFORE clearRegistrations() is called"
            );
            return null;
        };
        doAnswer(captureRegistryState).when(autoBidService).clearRegistrations(AUCTION_ID);

        adminService.cancelAuction(AUCTION_ID, REASON);

        verify(autoBidService).clearRegistrations(AUCTION_ID);
    }
}