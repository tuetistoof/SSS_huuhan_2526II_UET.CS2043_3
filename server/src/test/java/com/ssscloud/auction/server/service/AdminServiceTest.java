package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ssscloud.auction.common.dto.response.AdminAuctionView;
import com.ssscloud.auction.common.dto.response.AdminMetrics;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.base.AuctionConfig;
import com.ssscloud.auction.server.dao.AdminDAO;
import com.ssscloud.auction.server.dao.AuctionDAO;
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
    private Auction        auction;

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        adminDAO       = mock(AdminDAO.class);
        auctionDAO     = mock(AuctionDAO.class);
        autoBidService = mock(AutoBidService.class);

        adminService = new AdminService(adminDAO, auctionDAO, autoBidService);

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
    }

    @AfterEach
    void tearDown() {
        AuctionRegistry.getInstance().remove(AUCTION_ID);
    }

    // =========================================================================
    // Group 1 — getAuctions
    // =========================================================================

    @Test
    void testGetAuctions_noFilter_returnsAll() throws Exception {
        // WHY: filter == null phải trả toàn bộ auction, không bị giới hạn status
        List<AdminAuctionView> mockList = List.of(
            new AdminAuctionView(AUCTION_ID, "Test", "seller", 30_000L, AuctionStatus.RUNNING,
                LocalDateTime.now().plusHours(2))
        );
        when(adminDAO.findAllAuctions(null)).thenReturn(mockList);

        List<AdminAuctionView> result = adminService.getAuctions(null);

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
        // WHY: nếu DB lỗi khi updateStatus, phải throw exception và không tiếp tục
        // xóa registry hay clear auto-bid — tránh state không nhất quán
        doThrow(new RuntimeException("DB error"))
            .when(auctionDAO).updateStatus(any(), any());

        assertThrows(Exception.class,
            () -> adminService.cancelAuction(AUCTION_ID, REASON));

        // Registry vẫn còn auction vì cancel chưa hoàn tất
        assertNotNull(AuctionRegistry.getInstance().getLiveAuction(AUCTION_ID),
            "Auction must remain in registry if DB update fails");
    }
}