package com.ssscloud.auction.server.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.model.auction.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.server.controller.AdminController;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.NotificationController;
import com.ssscloud.auction.server.controller.QueryController;
import com.ssscloud.auction.server.controller.UserController;
import com.ssscloud.auction.server.dao.AdminDAO;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
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

    /**
     * Configures the global logging system to output to a file in the project root.
     */
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
        AuctionService auctionService       = new AuctionService(auctionDAO, userDAO, userService, itemService, notificationService, autoBidService);
        AdminService adminService           = new AdminService(adminDAO, auctionDAO, autoBidService, userDAO); // [ADMIN]

        // ── Controllers ───────────────────────────────────────────────────
        BidController bidController                 = new BidController(bidService, autoBidService, bidDAO, auctionDAO);
        UserController userController               = new UserController(userService);
        AuctionController auctionController         = new AuctionController(auctionService);
        NotificationController notificationController = new NotificationController(notificationService, notificationDAO);
        QueryController queryController             = new QueryController(queryDAO);
        AdminController adminController             = new AdminController(adminService); // [ADMIN]

        
        ConcurrentBidManager.initialize(userDAO, bidDAO, autoBidService, auctionDAO, notificationController);

        MessageHandler messageHandler = new MessageHandler(
                userController,
                auctionController,
                bidController,
                queryController,
                notificationController,
                adminController); // [ADMIN]

        // Recover active auctions from the database and start the safety-net maintenance task
        recoverLiveAuctions(auctionDAO, auctionService, queryDAO);
        startAuctionCloser(auctionDAO, auctionService, notificationService, autoBidService);

        logger.log(Level.INFO, "[Server] Initializing main networking listener on port 5000...");

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                workerPool.execute(new ClientHandler(clientSocket, messageHandler));
            }
        } catch (IOException ioException) {
            logger.log(Level.SEVERE, "Socket Initialization Error: Main listener loop terminated unexpectedly.", ioException);
            throw ioException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Critical unexpected error in server main loop.", exception);
            throw exception;
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
                    auctionService.scheduleCloseById(auctionId, info.getEndTime());
                    logger.log(Level.INFO, "[Recovery] Scheduled close for auctionId: " + auctionId);
                }
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Infrastructure failure during auction state recovery.", exception);
            throw exception;
        }
    }

    /**
     * Safety-net: cứ 30s scan DB tìm auction overdue bị sót.
     * FIX VĐ 3: gọi clearRegistrations + settle đầy đủ.
     */
    private static void startAuctionCloser(AuctionDAO auctionDAO, AuctionService auctionService,
                                            NotificationService notificationService,
                                            AutoBidService autoBidService) throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auction-closer");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                List<Auction> overdueAuctionList = new ArrayList<>();

                auctionDAO.findByStatus(AuctionStatus.RUNNING).stream()
                        .filter(a -> a.getAuctionConfig().getEndTime() != null
                                && a.getAuctionConfig().getEndTime().isBefore(now))
                        .forEach(overdueAuctionList::add);

                auctionDAO.findByStatus(AuctionStatus.OPEN).stream()
                        .filter(a -> a.getAuctionConfig().getEndTime() != null
                                && a.getAuctionConfig().getEndTime().isBefore(now))
                        .forEach(overdueAuctionList::add);

                for (Auction fromDB : overdueAuctionList) {
                    String auctionId = fromDB.getAuctionConfig().getId();

                    Auction liveAuction = AuctionRegistry.getInstance().get(auctionId);
                    Auction target = (liveAuction != null) ? liveAuction : fromDB;

                    if (target.getStatus() == AuctionStatus.FINISHED) continue;

                    synchronized (target) {
                        if (target.getStatus() == AuctionStatus.FINISHED) continue; // double-check
                        target.finish();
                    }

                    auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
                    AuctionRegistry.getInstance().remove(auctionId);
                    ConcurrentBidManager.getInstance().shutdown(auctionId);
                    autoBidService.clearRegistrations(auctionId); // FIX VĐ 3

                    auctionService.settleAuctionBalancesPublic(target); // FIX VĐ 4

                    ChangeManager.getInstance().notify(target);
                    notificationService.notifyAuctionEnded(target);
                    logger.log(Level.INFO, "[AuctionCloser] Safety-net finalized auctionId: " + auctionId);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[AuctionCloser] Scheduled check failure.", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
}