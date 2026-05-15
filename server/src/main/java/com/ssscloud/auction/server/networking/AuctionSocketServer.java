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
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.UserController;
import com.ssscloud.auction.server.controller.WatchlistController;
import com.ssscloud.auction.server.dao.AuctionDAO;
import com.ssscloud.auction.server.dao.BidTransactionDAO;
import com.ssscloud.auction.server.dao.ItemDAO;
import com.ssscloud.auction.server.dao.UserDAO;
import com.ssscloud.auction.server.dao.WatchlistDAO;
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

        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidTransactionDAO bidDAO = new BidTransactionDAO();
        WatchlistDAO watchlistDAO = new WatchlistDAO();

        AutoBidService autoBidService = new AutoBidService(auctionDAO, userDAO);
        BidService bidService = new BidService(auctionDAO, userDAO);
        ConcurrentBidManager.initialize(bidDAO, autoBidService, auctionDAO);

        UserService userService = new UserService(userDAO);
        UserController userController = new UserController(userService); // Naming: Clear descriptive names

        BidController bidController = new BidController(bidService, autoBidService, bidDAO);

        ItemService itemService = new ItemService(itemDAO);

        AuctionService auctionService = new AuctionService(auctionDAO, userService, itemService);
        AuctionController auctionController = new AuctionController(auctionService);

        WatchlistController watchlistController = new WatchlistController(watchlistDAO);

        NotificationService.getInstance().init(watchlistDAO);
        MessageHandler messageHandler = new MessageHandler(userController, auctionController, bidController, watchlistController);

        // Recover active auctions from the database and start the safety-net maintenance task
        recoverLiveAuctions(auctionDAO, auctionService);
        startAuctionCloser(auctionDAO, auctionService);

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
    private static void recoverLiveAuctions(AuctionDAO auctionDAO, AuctionService auctionService) throws Exception {
        try {
            List<Auction> liveAuctionList = new ArrayList<>(); // List naming suffix
            liveAuctionList.addAll(auctionDAO.findByStatus(AuctionStatus.OPEN));
            liveAuctionList.addAll(auctionDAO.findByStatus(AuctionStatus.RUNNING));

            LocalDateTime now = LocalDateTime.now();
            for (Auction auction : liveAuctionList) {
                String auctionId = auction.getAuctionConfig().getId(); // Id suffix

                if (auction.getAuctionConfig().getEndTime() != null
                        && auction.getAuctionConfig().getEndTime().isBefore(now)) {
                    // Finalize auctions that exceeded their conclusion time during server downtime.
                    auction.finish();
                    auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
                    logger.log(Level.INFO, "[Recovery] Successfully finalized overdue auctionId: " + auctionId);
                } else {
                    // Re-register active auctions and reschedule automatic closure.
                    AuctionRegistry.getInstance().register(auction);
                    auctionService.scheduleClose(auction);
                    logger.log(Level.INFO, "[Recovery] Re-registered active auctionId: " + auctionId);
                }
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Infrastructure failure during auction state recovery from database.", exception);
            throw exception;
        }
    }

    /**
     * Safety-net task: periodically scans for overdue auctions that might have been missed by 
     * standard scheduling mechanisms (e.g., edge cases during high load).
     */
    private static void startAuctionCloser(AuctionDAO auctionDAO, AuctionService auctionService) throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auction-closer");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                List<Auction> overdueAuctionList = new ArrayList<>(); // List naming suffix

                auctionDAO.findByStatus(AuctionStatus.RUNNING).stream()
                        .filter(a -> a.getAuctionConfig().getEndTime() != null
                                && a.getAuctionConfig().getEndTime().isBefore(now))
                        .forEach(overdueAuctionList::add);

                auctionDAO.findByStatus(AuctionStatus.OPEN).stream()
                        .filter(a -> a.getAuctionConfig().getEndTime() != null
                                && a.getAuctionConfig().getEndTime().isBefore(now))
                        .forEach(overdueAuctionList::add);

                for (Auction fromDB : overdueAuctionList) {
                    String auctionId = fromDB.getAuctionConfig().getId(); // Id suffix

                    // Prioritize objects in the AuctionRegistry as they have real-time observers attached.
                    Auction liveAuction = AuctionRegistry.getInstance().get(auctionId);
                    Auction target = (liveAuction != null) ? liveAuction : fromDB;

                    if (target.getStatus() == AuctionStatus.FINISHED) continue;

                    target.finish();
                    auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
                    AuctionRegistry.getInstance().remove(auctionId);
                    ChangeManager.getInstance().notify(target);
                    NotificationService.getInstance().notifyAuctionEnded(target); // Notify watchers
                    logger.log(Level.INFO, "[AuctionCloser] Safety-net finalizing auctionId: " + auctionId);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[AuctionCloser] Maintenance task encountered a scheduled check failure.", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
}