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
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;   

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.observer.ChangeManager;
import com.ssscloud.auction.server.controller.AuctionController;
import com.ssscloud.auction.server.controller.BidController;
import com.ssscloud.auction.server.controller.ItemController;
import com.ssscloud.auction.server.controller.UserController;
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

public class AuctionSocketServer {

    public static void setupLogger() {
        try {
            // Tạo file tên là "server.log" nằm ngay ở thư mục gốc project
            // Chữ 'true' có nghĩa là ghi nối tiếp vào cuối file (append), không xóa log cũ
            FileHandler fileHandler = new FileHandler("server.log", true);
            
            // Ép nó ghi dưới dạng Text dễ đọc (nếu không mặc định nó sẽ ghi ra file XML rất lằng nhằng)
            fileHandler.setFormatter(new SimpleFormatter());
            
            // Lấy Logger "tổ tiên" của toàn hệ thống và gắn bộ ghi file này vào
            Logger rootLogger = Logger.getLogger("");
            rootLogger.addHandler(fileHandler);
            
        } catch (Exception e) {
            System.err.println("Không thể khởi tạo file log: " + e.getMessage());
        }
    }

    private static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        setupLogger();

        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
        WatchlistDAO watchlistDAO = new WatchlistDAO();

        AutoBidService autoBidService = new AutoBidService(auctionDAO, userDAO);
        BidService bidService = new BidService(auctionDAO, userDAO);
        ConcurrentBidManager.initialize(bidTransactionDAO, autoBidService, auctionDAO);

        UserService userService = new UserService(userDAO);
        UserController userCtrl = new UserController(userService);

        BidController bidCtrl = new BidController(bidService, autoBidService, bidTransactionDAO);

        ItemService itemService = new ItemService(itemDAO);
        ItemController itemCtrl = new ItemController(itemDAO);

        AuctionService auctionService = new AuctionService(auctionDAO, userService, itemService);
        AuctionController auctionCtrl = new AuctionController(auctionService);

        NotificationService.getInstance().init(watchlistDAO);

        MessageHandler messageHandler = new MessageHandler(userCtrl, auctionCtrl, bidCtrl, itemCtrl);

        // Phục hồi auction còn sống sau restart + bật safety net 30s
        recoverLiveAuctions(auctionDAO, auctionService);
        startAuctionCloser(auctionDAO, auctionService);

        System.out.println("[Server] Khởi động port 5000...");

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientHandler(clientSocket, messageHandler));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Khi server restart: load lại các auction OPEN/RUNNING từ DB,
     * đăng ký vào Registry và schedule lại timer đóng.
     */
    private static void recoverLiveAuctions(AuctionDAO auctionDAO, AuctionService auctionService) {
        List<Auction> live = new ArrayList<>();
        live.addAll(auctionDAO.findByStatus(AuctionStatus.OPEN));
        live.addAll(auctionDAO.findByStatus(AuctionStatus.RUNNING));

        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : live) {
            if (auction.getAuctionConfig().getEndTime() != null
                    && auction.getAuctionConfig().getEndTime().isBefore(now)) {
                // Quá hạn trong lúc server tắt → đóng luôn
                auction.finish();
                auctionDAO.updateStatus(auction.getAuctionConfig().getId(), AuctionStatus.FINISHED);
                System.out.println("[Recovery] Đóng auction quá hạn: " + auction.getAuctionConfig().getId());
            } else {
                // Còn hạn → đăng ký lại và schedule
                AuctionRegistry.getInstance().register(auction);
                auctionService.scheduleClose(auction);
                System.out.println("[Recovery] Khôi phục auction: " + auction.getAuctionConfig().getId());
            }
        }
    }

    /**
     * Safety net: quét mỗi 30s để bắt những auction bị sót
     * (không đi qua scheduleClose — trường hợp hiếm).
     */
    private static void startAuctionCloser(AuctionDAO auctionDAO, AuctionService auctionService) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auction-closer");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                List<Auction> overdue = new ArrayList<>();

                auctionDAO.findByStatus(AuctionStatus.RUNNING).stream()
                        .filter(a -> a.getAuctionConfig().getEndTime() != null
                                && a.getAuctionConfig().getEndTime().isBefore(now))
                        .forEach(overdue::add);

                auctionDAO.findByStatus(AuctionStatus.OPEN).stream()
                        .filter(a -> a.getAuctionConfig().getEndTime() != null
                                && a.getAuctionConfig().getEndTime().isBefore(now))
                        .forEach(overdue::add);

                for (Auction fromDB : overdue) {
                    String id = fromDB.getAuctionConfig().getId();

                    // Ưu tiên object trong Registry vì nó có observer được attach
                    Auction live = AuctionRegistry.getInstance().get(id);
                    Auction target = (live != null) ? live : fromDB;

                    if (target.getStatus() == AuctionStatus.FINISHED) continue;

                    target.finish();
                    auctionDAO.updateStatus(id, AuctionStatus.FINISHED);
                    AuctionRegistry.getInstance().remove(id);
                    ChangeManager.getInstance().notify(target);
                    NotificationService.getInstance().notifyAuctionEnded(target);
                    System.out.println("[AuctionCloser] Safety net đóng: " + id);
                }
            } catch (Exception e) {
                System.err.println("[AuctionCloser] Lỗi: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
}