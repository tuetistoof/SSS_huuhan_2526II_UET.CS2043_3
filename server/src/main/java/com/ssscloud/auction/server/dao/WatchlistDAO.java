package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;


public class WatchlistDAO extends BaseDAO {

    // ── Watch ─────────────────────────────────────────────────────────────────

    public boolean add(String auctionId, String userId) {
        // auction_id trước, user_id sau — khớp PRIMARY KEY (auction_id, user_id)
        String sql = "INSERT INTO watchlist (auction_id, user_id) VALUES (?, ?)";
        Connection        conn = null;
        PreparedStatement ps   = null;
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, auctionId);   // auction_id — cột 1 trong PK
            ps.setString(2, userId);      // user_id    — cột 2 trong PK
            ps.executeUpdate();
            logger.info("Watchlist: user " + userId + " watch auction " + auctionId);
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("Watchlist: user " + userId + " đã watch auction " + auctionId);
            return false;
        } catch (SQLException e) {
            logger.severe("Lỗi add watchlist: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.severe("Lỗi add watchlist: " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    // ── Unwatch ───────────────────────────────────────────────────────────────

    public boolean remove(String auctionId, String userId) {
        String sql = "DELETE FROM watchlist WHERE auction_id = ? AND user_id = ?";
        Connection        conn = null;
        PreparedStatement ps   = null;
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, auctionId);   // auction_id trước
            ps.setString(2, userId);
            int rows = ps.executeUpdate();
            logger.info("Watchlist: user " + userId + " unwatch auction " + auctionId);
            return rows > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi remove watchlist: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.severe("Lỗi remove watchlist: " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public List<AuctionDisplayInfoDTO> findWatchlistDetailsByUser(String userId) {
    List<AuctionDisplayInfoDTO> list = new ArrayList<>();
    
    // SQL được xây dựng dựa trên cấu trúc findSellerAuction của bạn
    String sql = 
        "SELECT a.id, " +
        "       e.name AS auction_name, " +          // Tên cuộc đấu giá lấy từ entity
        "       ac.end_time, " +                     // Thời gian kết thúc từ auction_config
        "       u_seller.username AS seller_username, " + // Người bán
        "       ei.name AS item_name, " +            // Tên vật phẩm lấy từ entity (alias ei)
        "       i.type AS item_type, " +             // Loại vật phẩm (electronic/vehicle...)
        "       COALESCE(last_bid.bid_amount, ac.start_price) AS current_price, " + // Giá hiện tại
        "       (SELECT img.image_url FROM item_image_url img " +
        "        WHERE img.item_id = a.item_id LIMIT 1) AS image_url " + // Lấy 1 ảnh đại diện
        "FROM watchlist w " +
        "JOIN auction a ON w.auction_id = a.id " +
        "JOIN auction_config ac ON a.id = ac.id " +
        "JOIN entity e ON a.id = e.id " +            // JOIN entity để lấy tên auction
        "JOIN user u_seller ON a.seller_id = u_seller.id " + 
        "JOIN item i ON a.item_id = i.id " +
        "JOIN entity ei ON i.id = ei.id " +          // JOIN entity lần nữa để lấy tên item
        "LEFT JOIN ( " +
        "    SELECT b1.auction_id, b1.bid_amount FROM bid_transaction b1 " +
        "    WHERE b1.bid_time = (SELECT MAX(b2.bid_time) FROM bid_transaction b2 " +
        "                         WHERE b2.auction_id = b1.auction_id) " +
        ") AS last_bid ON last_bid.auction_id = a.id " +
        "WHERE w.user_id = ?";

    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
        conn = getConnection();
        ps = conn.prepareStatement(sql);
        ps.setString(1, userId);
        rs = ps.executeQuery();

        while (rs.next()) {
            AuctionDisplayInfoDTO dto = new AuctionDisplayInfoDTO();
            dto.setId(rs.getString("id"));
            dto.setAuctionName(rs.getString("auction_name"));
            dto.setItemName(rs.getString("item_name"));
            dto.setItemType(rs.getString("item_type"));
            dto.setCurrentPrice(rs.getLong("current_price"));
            
            Timestamp ts = rs.getTimestamp("end_time");
            if (ts != null) {
                dto.setEndTime(ts.toLocalDateTime());
            }
            
            dto.setSellerUsername(rs.getString("seller_username"));
            dto.setImageUrl(List.of(rs.getString("image_url")));
            

            list.add(dto);
        }
        logger.info("[Watchlist] Loaded " + list.size() + " detailed items for user " + userId);
    } catch (SQLException e) {
        logger.severe("Lỗi SQL Watchlist (Chi tiết): " + e.getMessage());
    } catch (Exception e) {
        logger.severe("Lỗi SQL Watchlist (Chi tiết): " + e.getMessage());
    } finally {
        closeResource(rs, ps);
        closeConnect(conn);
    }
    return list;
}

    /** Lấy list user_id đang watch auction — dùng để push OUTBID_NOTIFICATION. */
    public List<String> findUserIdsByAuction(String auctionId) {
        String sql = "SELECT user_id FROM watchlist WHERE auction_id = ?";
        Connection        conn   = null;
        PreparedStatement ps     = null;
        ResultSet         rs     = null;
        List<String>      result = new ArrayList<>();
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, auctionId);
            rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString("user_id"));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findUserIdsByAuction: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Lỗi findUserIdsByAuction: " + e.getMessage());
        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
        return result;
    }

    /** Kiểm tra user có đang follow auction không — dùng để render nút Follow/Unfollow. */
    public boolean isFollowing(String auctionId, String userId) {
        String sql = "SELECT 1 FROM watchlist WHERE auction_id = ? AND user_id = ?";
        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, auctionId);   // auction_id trước — tận dụng PK index
            ps.setString(2, userId);
            rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.severe("Lỗi isFollowing: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.severe("Lỗi isFollowing: " + e.getMessage());
            return false;
        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }
}