package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.model.base.AuctionConfig;

public class AuctionDAO extends BaseDAO {

    // ── Save ──────────────────────────────────────────────────────────────────

    public boolean saveAuction(Auction auction) {
        // entity lưu id + name của auction_config
        String sqlEntity      = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlAuctionConfig = "INSERT INTO auction_config (id, start_price, min_increment, start_time, end_time, extend_second) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlAuction     = "INSERT INTO auction (id, status, seller_id, item_id) VALUES (?, ?, ?, ?)";

        Connection        conn            = null;
        PreparedStatement psEntity        = null;
        PreparedStatement psAuctionConfig = null;
        PreparedStatement psAuction       = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 1. entity — lưu name của auction vào đây
            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString(1, auction.getAuctionConfig().getId());
            psEntity.setString(2, auction.getAuctionConfig().getName());
            psEntity.executeUpdate();

            // 2. auction_config — không có cột name
            psAuctionConfig = conn.prepareStatement(sqlAuctionConfig);
            psAuctionConfig.setString(1, auction.getAuctionConfig().getId());
            psAuctionConfig.setLong(2,   auction.getAuctionConfig().getStartPrice());
            psAuctionConfig.setLong(3,   auction.getAuctionConfig().getMinIncrement());
            psAuctionConfig.setObject(4, auction.getAuctionConfig().getStartTime());
            psAuctionConfig.setObject(5, auction.getAuctionConfig().getEndTime());
            psAuctionConfig.setInt(6,    auction.getAuctionConfig().getExtendSecond());
            psAuctionConfig.executeUpdate();

            // 3. auction
            psAuction = conn.prepareStatement(sqlAuction);
            psAuction.setString(1, auction.getAuctionConfig().getId());
            psAuction.setString(2, auction.getStatus().name());
            psAuction.setString(3, auction.getSellerId());
            psAuction.setString(4, auction.getItemId());
            psAuction.executeUpdate();

            // 4. bid transactions (nếu có)
            BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
            for (BidTransaction bt : auction.getBidTransaction()) {
                bidTransactionDAO.saveBidTransaction(conn, bt);
            }

            conn.commit();
            logger.info("Đã lưu auction: " + auction.getAuctionConfig().getName());
            return true;

        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("Auction đã tồn tại: " + auction.getAuctionConfig().getName() + " - " + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Lỗi saveAuction: " + auction.getAuctionConfig().getName() + " - " + e.getMessage());
            safelyRollback(conn);
            return false;
        } finally {
            resetAutocommit(conn);
            closeResource(psEntity, psAuctionConfig, psAuction);
            closeConnect(conn);
        }
    }

    // ── Find by seller ────────────────────────────────────────────────────────

    public List<Auction> findBySellerId(String sellerId) {
        // FIX: JOIN entity e ON a.id = e.id để lấy e.name (tên auction)
        // Code gốc dùng ac.name nhưng auction_config không có cột name
        String sql =
            "SELECT a.id AS auction_id, a.status, a.seller_id, a.item_id, " +
            "       e.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
            "       b.bidder_id, b.bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +
            "LEFT JOIN bid_transaction b ON a.id = b.auction_id " +
            "WHERE a.seller_id = ? " +
            "ORDER BY b.bid_time DESC";

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;
        Map<String, Auction> auctionMap = new LinkedHashMap<>();

        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, sellerId);
            rs = ps.executeQuery();

            while (rs.next()) {
                String auctionId = rs.getString("auction_id");
                Auction auction = auctionMap.get(auctionId);
                if (auction == null) {
                    auction = mapResultSetToAuction(rs);
                    auctionMap.put(auctionId, auction);
                }
                if (rs.getString("bidder_id") != null) {
                    auction.getBidTransaction().add(mapResultSetToBid(rs));
                }
            }
            return new ArrayList<>(auctionMap.values());

        } catch (SQLException e) {
            logger.severe("Lỗi findBySellerId [" + sellerId + "]: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }

    // ── Find by auction id ────────────────────────────────────────────────────

    public Auction findByAuctionId(String id) {
        // Giữ nguyên — đã có JOIN entity e ON a.id = e.id, dùng e.name đúng
        String sql =
            "SELECT a.id AS auction_id, a.status, a.seller_id, a.item_id, " +
            "       e.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
            "       b.bidder_id, b.bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +
            "LEFT JOIN bid_transaction b ON a.id = b.auction_id " +
            "WHERE a.id = ? " +
            "ORDER BY b.bid_time DESC";

        Connection        conn    = null;
        PreparedStatement ps      = null;
        ResultSet         rs      = null;
        Auction           auction = null;

        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();

            while (rs.next()) {
                if (auction == null) {
                    auction = mapResultSetToAuction(rs);
                }
                if (rs.getString("bidder_id") != null) {
                    auction.getBidTransaction().add(mapResultSetToBid(rs));
                }
            }
            return auction; // null nếu không tìm thấy

        } catch (SQLException e) {
            logger.severe("Lỗi findByAuctionId [" + id + "]: " + e.getMessage());
            return null;
        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }

    // ── Find by status ────────────────────────────────────────────────────────

    public List<Auction> findByStatus(AuctionStatus status) {
        // FIX: thêm JOIN entity e ON a.id = e.id, đổi ac.name → e.name
        String sql =
            "SELECT a.id AS auction_id, a.status, a.seller_id, a.item_id, " +
            "       e.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
            "       b.bidder_id, b.bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +
            "LEFT JOIN bid_transaction b ON a.id = b.auction_id " +
            "WHERE a.status = ? " +
            "ORDER BY b.bid_time DESC";

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;
        Map<String, Auction> auctionMap = new LinkedHashMap<>();

        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, status.name());
            rs = ps.executeQuery();

            while (rs.next()) {
                String auctionId = rs.getString("auction_id");
                Auction auction = auctionMap.get(auctionId);
                if (auction == null) {
                    auction = mapResultSetToAuction(rs);
                    auctionMap.put(auctionId, auction);
                }
                if (rs.getString("bidder_id") != null) {
                    auction.getBidTransaction().add(mapResultSetToBid(rs));
                }
            }
            return new ArrayList<>(auctionMap.values());

        } catch (SQLException e) {
            logger.severe("Lỗi findByStatus [" + status.name() + "]: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }

    // ── Find seller auctions (DTO) ────────────────────────────────────────────

    public List<AuctionDisplayInfoDTO> findSellerAuction(String sellerId) {
        // entity e ON a.id = e.id → e.name = tên auction (đã đúng trong code gốc)
        String sql =
            "SELECT a.id, " +
            "       e.name AS auction_name, ac.end_time, " +
            "       u.username AS seller_username, " +
            "       ei.name AS item_name, i.type AS item_type, " +
            "       COALESCE(last_bid.bid_amount, ac.start_price) AS current_price, " +
            "       GROUP_CONCAT(img.image_url SEPARATOR ', ') AS image_url " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +           // tên auction
            "JOIN user u ON a.seller_id = u.id " +
            "JOIN item i ON a.item_id = i.id " +
            "JOIN entity ei ON i.id = ei.id " +          // tên item — alias riêng
            "LEFT JOIN item_image_url img ON a.item_id = img.item_id " +
            "LEFT JOIN ( " +
            "    SELECT b1.auction_id, b1.bid_amount FROM bid_transaction b1 " +
            "    WHERE b1.bid_time = (SELECT MAX(b2.bid_time) FROM bid_transaction b2 " +
            "                         WHERE b2.auction_id = b1.auction_id) " +
            ") AS last_bid ON last_bid.auction_id = a.id " +
            "WHERE a.seller_id = ? " +
            "GROUP BY a.id, e.name, ac.end_time, u.username, ei.name, i.type, ac.start_price, last_bid.bid_amount";

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, sellerId);
            rs = ps.executeQuery();

            List<AuctionDisplayInfoDTO> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapResultSetToDisplayDTO(rs));
            }
            return result;

        } catch (SQLException e) {
            logger.severe("Lỗi findSellerAuction: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }

    // ── Find active auctions (DTO) ────────────────────────────────────────────

    public List<AuctionDisplayInfoDTO> findActiveAuctions() {
        String sql =
            "SELECT a.id, " +
            "       e.name AS auction_name, ac.end_time, " +
            "       u.username AS seller_username, " +
            "       ei.name AS item_name, i.type AS item_type, " +
            "       COALESCE(last_bid.bid_amount, ac.start_price) AS current_price, " +
            "       GROUP_CONCAT(img.image_url SEPARATOR ', ') AS image_url " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +           // tên auction
            "JOIN user u ON a.seller_id = u.id " +
            "JOIN item i ON a.item_id = i.id " +
            "JOIN entity ei ON i.id = ei.id " +          // tên item — alias riêng
            "LEFT JOIN item_image_url img ON a.item_id = img.item_id " +
            "LEFT JOIN ( " +
            "    SELECT b1.auction_id, b1.bid_amount FROM bid_transaction b1 " +
            "    WHERE b1.bid_time = (SELECT MAX(b2.bid_time) FROM bid_transaction b2 " +
            "                         WHERE b2.auction_id = b1.auction_id) " +
            ") AS last_bid ON last_bid.auction_id = a.id " +
            "WHERE a.status = 'OPEN' " +
            "GROUP BY a.id, e.name, ac.end_time, u.username, ei.name, i.type, ac.start_price, last_bid.bid_amount";

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            List<AuctionDisplayInfoDTO> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapResultSetToDisplayDTO(rs));
            }
            return result;

        } catch (SQLException e) {
            logger.severe("Lỗi findActiveAuctions: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public boolean updateEndTime(String auctionId, LocalDateTime newEndTime) {
        String sql = "UPDATE auction_config SET end_time = ? WHERE id = ?";
        Connection        conn = null;
        PreparedStatement ps   = null;
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setObject(1, newEndTime);
            ps.setString(2, auctionId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) logger.info("updateEndTime auctionId=" + auctionId + " -> " + newEndTime);
            return ok;
        } catch (SQLException e) {
            logger.severe("Lỗi updateEndTime auctionId=" + auctionId + ": " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    public boolean updateStatus(String auctionId, AuctionStatus newStatus) {
        String sql = "UPDATE auction SET status = ? WHERE id = ?";
        Connection        conn = null;
        PreparedStatement ps   = null;
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, newStatus.name());
            ps.setString(2, auctionId);
            int rows = ps.executeUpdate();
            logger.info("updateStatus auctionId=" + auctionId + " -> " + newStatus);
            return rows > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi updateStatus auctionId=" + auctionId + ": " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public boolean deleteById(String auctionId) {
        String sql = "DELETE FROM auction WHERE id = ?";
        Connection        conn = null;
        PreparedStatement ps   = null;
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, auctionId);
            int rows = ps.executeUpdate();
            if (rows == 0) logger.warning("deleteAuction id=" + auctionId + " - không thể xóa");
            return rows > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi deleteAuction id=" + auctionId + ": " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        AuctionConfig config = new AuctionConfig(
            rs.getString("auction_id"),                    // alias rõ ràng từ a.id AS auction_id
            rs.getString("name"),                          // từ entity e
            rs.getLong("start_price"),
            rs.getLong("min_increment"),
            toLocalDateTime(rs.getTimestamp("start_time")),
            toLocalDateTime(rs.getTimestamp("end_time")),
            rs.getInt("extend_second")
        );
        return new Auction(
            config,
            AuctionStatus.valueOf(rs.getString("status")),
            rs.getString("seller_id"),
            rs.getString("item_id"),
            new ArrayList<>()
        );
    }

    // Khác với mapResultSetToBid bên BidTransactionDAO: dùng alias "auction_id" rõ ràng
    private BidTransaction mapResultSetToBid(ResultSet rs) throws SQLException {
        return new BidTransaction(
            rs.getString("auction_id"),                    // alias rõ ràng từ a.id AS auction_id
            rs.getString("bidder_id"),
            rs.getString("bidder_username"),
            rs.getLong("bid_amount"),
            toLocalDateTime(rs.getTimestamp("bid_time")),
            BidType.valueOf(rs.getString("bid_type"))
        );
    }

    private AuctionDisplayInfoDTO mapResultSetToDisplayDTO(ResultSet rs) throws SQLException {
        String imageUrlRaw = rs.getString("image_url");
        List<String> imageUrls = (imageUrlRaw != null)
                ? List.of(imageUrlRaw.split(", "))
                : new ArrayList<>();
        return new AuctionDisplayInfoDTO(
            rs.getString("id"),
            rs.getString("auction_name"),
            rs.getString("item_name"),
            rs.getString("item_type"),
            rs.getLong("current_price"),
            rs.getObject("end_time", LocalDateTime.class),
            rs.getString("seller_username"),
            imageUrls
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}