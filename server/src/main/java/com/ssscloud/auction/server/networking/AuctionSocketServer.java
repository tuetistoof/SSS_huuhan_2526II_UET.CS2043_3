    package com.ssscloud.auction.server.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.server.controller.AdminController;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.NotificationController;
import com.ssscloud.auction.server.controller.QueryController;
import com.ssscloud.auction.server.controller.UserController;
import com.ssscloud.auction.server.dao.AdminDAO;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.dao.DatabaseConnection;
import com.ssscloud.auction.server.dao.ItemDAO;
import com.ssscloud.auction.server.dao.NotificationDAO;
import com.ssscloud.auction.server.dao.QueryDAO;
import com.ssscloud.auction.server.dao.QueryDAO.AuctionScheduleInfo;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.service.AdminService;
import com.ssscloud.auction.server.service.AuctionService;
import com.ssscloud.auction.server.service.AutoBidService;
import com.ssscloud.auction.server.service.BidService;
import com.ssscloud.auction.server.service.ConcurrentBidManager;
import com.ssscloud.auction.server.service.ItemService;
import com.ssscloud.auction.server.service.NotificationService;
import com.ssscloud.auction.server.service.UserService;
import com.ssscloud.auction.server.util.AuctionRegistry;

/**
 * AuctionSocketServer is the primary entry point for the auction system backend.
 * It initializes infrastructure services, recovers auction states from persistence,
 * and manages the main socket listener loop.
 */
public class AuctionSocketServer {
    private static final Logger logger = Logger.getLogger(AuctionSocketServer.class.getName()); // Logging Standards: First Attribute

    private static final ExecutorService workerPool = Executors.newCachedThreadPool(); // Naming: Descriptive Attributes

    // --- PUBLIC METHODS ---

    public static void setupLogger() throws Exception {
        try {
            // Create "server.log" in the root directory. Enable append mode to preserve historical data.
            FileHandler fileHandler = new FileHandler("server.log", true);

            // Enforce plain text formatting for better technical readability.
            fileHandler.setFormatter(new SimpleFormatter());

            // Attach the file handler to the root logger for comprehensive system monitoring.
            Logger rootLogger = Logger.getLogger("");
            rootLogger.addHandler(fileHandler);

        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Log Initialization Failure: Unable to instantiate the server log file.", exception);
            throw exception;
        }
    }
    
    public static void main(String[] args) throws Exception {
        setupLogger();

        // ── DAOs ──────────────────────────────────────────────────────────
        UserDAO userDAO                     = new UserDAO();
        ItemDAO itemDAO                     = new ItemDAO();
        AuctionDAO auctionDAO               = new AuctionDAO();
        BidTransactionDAO bidDAO            = new BidTransactionDAO();
        NotificationDAO notificationDAO     = new NotificationDAO();
        QueryDAO queryDAO                   = new QueryDAO();
        AdminDAO adminDAO                   = new AdminDAO();         // [ADMIN]

        // ── Services ──────────────────────────────────────────────────────
        AutoBidService autoBidService       = new AutoBidService(auctionDAO, userDAO);
        BidService bidService               = new BidService(auctionDAO, userDAO);
        UserService userService             = new UserService(userDAO);
        ItemService itemService             = new ItemService(itemDAO);
        NotificationService notificationService = new NotificationService(queryDAO, notificationDAO);
        AuctionService auctionService       = new AuctionService(auctionDAO, queryDAO, userDAO, userService, itemService, notificationService, autoBidService);
        AdminService adminService           = new AdminService(adminDAO, auctionDAO, autoBidService, userDAO); // [ADMIN]

        // ── Controllers ───────────────────────────────────────────────────
        BidController bidController                 = new BidController(bidService, autoBidService, bidDAO, auctionDAO);
        UserController userController               = new UserController(userService);
        AuctionController auctionController         = new AuctionController(auctionService);
        NotificationController notificationController = new NotificationController(notificationService, notificationDAO);
        QueryController queryController             = new QueryController(queryDAO);
        AdminController adminController             = new AdminController(adminService); // [ADMIN]

        AuctionRegistry.initialize(auctionDAO);        
        ConcurrentBidManager.initialize(userDAO, bidDAO, autoBidService, auctionDAO, notificationController);

        MessageHandler messageHandler = new MessageHandler(
                userController,
                auctionController,
                bidController,
                queryController,
                notificationController,
                adminController); // [ADMIN]

        recoverLiveAuctions(auctionDAO, auctionService, queryDAO);
        auctionService.startAuctionCloser(); //1p query db để kiểm tra có miss không
        logger.log(Level.INFO, "[Server] Initializing main networking listener on port 5000...");
        
        ServerSocket serverSocket = new ServerSocket(5000);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {  //shutdown
            logger.info("[Shutdown] SIGTERM nhận được, bắt đầu graceful shutdown...");

            try { serverSocket.close(); } catch (Exception ignored) {} //ngừng nhận connection mới

            workerPool.shutdown(); //clienthanlder ko nhận thêm thread
            try { workerPool.awaitTermination(10, TimeUnit.SECONDS); }
            catch (InterruptedException ignored) {}

            auctionService.shutdownScheduler(); //xử lí nốt cái thread còn tồn đọng

            try { //đóng db pool (bắt buộc nằm trong khối try catch)
                DatabaseConnection.getInstance().close();
            } catch (Exception e) {
                e.printStackTrace();
            } 
            logger.info("[Shutdown] Hoàn tất.");
        }, "shutdown-hook"));

        try { //vòng lặp chạy chính
            while (true) {
                Socket clientSocket = serverSocket.accept();
                workerPool.execute(new ClientHandler(clientSocket, messageHandler));
            }
        } catch (IOException ioException) {
            if (!serverSocket.isClosed()) { // phân biệt shutdown bình thường vs lỗi thật
                logger.log(Level.SEVERE, "Socket Initialization Error: Main listener loop terminated unexpectedly.", ioException);
                throw ioException;
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Critical unexpected error in server main loop.", exception);
            throw exception;
        }  finally {
            try { serverSocket.close(); } catch (Exception ignored) {}
        
        }
    }

    // --- PRIVATE METHODS ---

    /**
     * On server restart, reloads all auctions with OPEN or RUNNING status from the database,
     * registers them into the AuctionRegistry, and schedules the closing timers.
     */
    /**
 * Khi server restart: query nhẹ auctionId+endTime (không load full Auction),
 * không registerIfAbsent (Registry chỉ add khi user SUBSCRIBE_AUCTION).
 * Auction overdue → updateStatus + settle qua DB.
 * Auction còn hạn  → chỉ schedule timer.
 */
    private static void recoverLiveAuctions(AuctionDAO auctionDAO, AuctionService auctionService,
                                            QueryDAO queryDAO) throws Exception {
        try {
            List<AuctionScheduleInfo> scheduleInfoList = queryDAO.findActiveScheduleInfos();
            LocalDateTime now = LocalDateTime.now();

            for (AuctionScheduleInfo info : scheduleInfoList) {
                String auctionId = info.getAuctionId();

                if (info.getEndTime() != null && info.getEndTime().isBefore(now)) {
                    // Overdue trong lúc server down
                    auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);

                    // Load full để có bid data cho settle
                    Auction overdue = auctionDAO.findByAuctionId(auctionId);
                    if (overdue != null) {
                        auctionService.settleAuctionBalancesPublic(overdue); // FIX VĐ 4
                    }
                    logger.log(Level.INFO, "[Recovery] Finalized + settled overdue auctionId: " + auctionId);
                } else {
                    // Còn hạn — chỉ schedule timer, KHÔNG register Registry  (FIX VĐ 1)
                    auctionService.scheduleClose(auctionId, info.getEndTime());
                    logger.log(Level.INFO, "[Recovery] Scheduled close for auctionId: " + auctionId);
                }
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Infrastructure failure during auction state recovery.", exception);
            throw exception;
        }
    }

}