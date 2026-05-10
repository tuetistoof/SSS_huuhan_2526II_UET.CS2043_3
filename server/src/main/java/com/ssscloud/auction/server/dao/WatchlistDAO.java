package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * WatchlistDAO — thao tác DB cho bảng watchlist.
 *
 * SQL tạo bảng:
 *   CREATE TABLE watchlist (
 *       user_id    VARCHAR(36) NOT NULL,
 *       auction_id VARCHAR(36) NOT NULL,
 *       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
 *       PRIMARY KEY (user_id, auction_id),
 *       FOREIGN KEY (user_id)    REFERENCES user(id),
 *       FOREIGN KEY (auction_id) REFERENCES auction(id)
 *   );
 */
public class WatchlistDAO extends BaseDAO {

    // ── Watch ─────────────────────────────────────────────────────────────────

    public boolean add(String userId, String auctionId) {
        String sql = "INSERT INTO watchlist (user_id, auction_id) VALUES (?, ?)";
        Connection        conn = null;
        PreparedStatement ps   = null;
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setString(2, auctionId);
            ps.executeUpdate();
            logger.info("Watchlist: user " + userId + " watch auction " + auctionId);
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("Watchlist: user " + userId + " đã watch auction " + auctionId);
            return false;
        } catch (SQLException e) {
            logger.severe("Lỗi add watchlist: " + e.getMessage());
            return false;
        } finally {
            closeConnect(conn);
            closeResource(ps);
        }
    }

    // ── Unwatch ───────────────────────────────────────────────────────────────

    public boolean remove(String userId, String auctionId) {
        String sql = "DELETE FROM watchlist WHERE user_id = ? AND auction_id = ?";
        Connection        conn = null;
        PreparedStatement ps   = null;
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setString(2, auctionId);
            int rows = ps.executeUpdate();
            logger.info("Watchlist: user " + userId + " unwatch auction " + auctionId);
            return rows > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi remove watchlist: " + e.getMessage());
            return false;
        } finally {
            closeConnect(conn);
            closeResource(ps);
        }
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /** Lấy list auctionId mà user đang watch — dùng cho màn hình Watch List. */
    public List<String> findAuctionIdsByUser(String userId) {
        String sql = "SELECT auction_id FROM watchlist WHERE user_id = ?";
        Connection        conn   = null;
        PreparedStatement ps     = null;
        ResultSet         rs     = null;
        List<String>      result = new ArrayList<>();
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString("auction_id"));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findAuctionIdsByUser: " + e.getMessage());
        } finally {
            closeConnect(conn);
            closeResource(rs, ps);
        }
        return result;
    }

    /** Lấy list userId đang watch auction — dùng để push OUTBID_NOTIFICATION. */
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
        } finally {
            closeConnect(conn);
            closeResource(rs, ps);
        }
        return result;
    }

    /** Kiểm tra user có đang watch auction không — dùng để render nút Watch/Unwatch. */
    public boolean isWatching(String userId, String auctionId) {
        String sql = "SELECT 1 FROM watchlist WHERE user_id = ? AND auction_id = ?";
        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;
        try {
            conn = getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setString(2, auctionId);
            rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.severe("Lỗi isWatching: " + e.getMessage());
            return false;
        } finally {
            closeConnect(conn);
            closeResource(rs, ps);
        }
    }
}