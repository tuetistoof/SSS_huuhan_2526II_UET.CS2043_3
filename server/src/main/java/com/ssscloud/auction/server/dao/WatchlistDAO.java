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
        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /** Lấy list auction_id mà user đang watch — dùng cho màn hình Watch List. */
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
            closeResource(rs, ps);
            closeConnect(conn);
        }
        return result;
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
        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
        return result;
    }

    /** Kiểm tra user có đang watch auction không — dùng để render nút Watch/Unwatch. */
    public boolean isWatching(String auctionId, String userId) {
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
        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }
}