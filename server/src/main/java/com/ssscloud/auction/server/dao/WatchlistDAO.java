package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;

public class WatchlistDAO extends BaseDAO {

    private static final Logger logger = Logger.getLogger(WatchlistDAO.class.getName());

    // --- PUBLIC METHODS ---

    /**
     * Adds an auction to a user's watchlist.
     * Primary key order: auction_id followed by user_id.
     */
    public boolean add(String auctionId, String userId) throws SQLException, Exception {
        String sql = "INSERT INTO watchlist (auction_id, user_id) VALUES (?, ?)";
        Connection connection = null;
        PreparedStatement ps = null;
        
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, auctionId);
            ps.setString(2, userId);
            ps.executeUpdate();
            logger.log(Level.INFO, "Watchlist: User " + userId + " started watching auction " + auctionId);
            return true;
        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            logger.log(Level.WARNING, "Watchlist: User " + userId + " is already watching auction " + auctionId);
            return false;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error adding to watchlist: " + sqlException.getMessage(), sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in WatchlistDAO.add: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    /**
     * Removes an auction from a user's watchlist.
     */
    public boolean remove(String auctionId, String userId) throws SQLException, Exception {
        String sql = "DELETE FROM watchlist WHERE auction_id = ? AND user_id = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, auctionId);
            ps.setString(2, userId);
            int rows = ps.executeUpdate();
            logger.log(Level.INFO, "Watchlist: User " + userId + " unwatched auction " + auctionId);
            return rows > 0;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error removing from watchlist: " + sqlException.getMessage(), sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in WatchlistDAO.remove: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    /**
     * Retrieves the list of auction identifiers a specific user is following.
     **/
    public List<String> findAuctionIdsByUser(String userId) throws SQLException, Exception {
        String sql = "SELECT auction_id FROM watchlist WHERE user_id = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<String> auctionIdList = new ArrayList<>();
        
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                auctionIdList.add(rs.getString("auction_id"));
            }
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error in findAuctionIdsByUser for userId: " + userId, sqlException);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in WatchlistDAO.findAuctionIdsByUser: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
        return auctionIdList;
    }

    /**
     * Retrieves the list of user identifiers watching a specific auction.
     * Commonly used for outbid notifications.
     */
    public List<String> findUserIdsByAuction(String auctionId) throws SQLException, Exception {
        String sql = "SELECT user_id FROM watchlist WHERE auction_id = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<String> userIdList = new ArrayList<>();
        
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, auctionId);
            rs = ps.executeQuery();
            while (rs.next()) {
                userIdList.add(rs.getString("user_id"));
            }
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error in findUserIdsByAuction for auctionId: " + auctionId, sqlException);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in WatchlistDAO.findUserIdsByAuction: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
        return userIdList;
    }

    /**
     * Checks if a user is currently following a specific auction.
     */
    public boolean isFollowing(String auctionId, String userId) throws SQLException, Exception {
        String sql = "SELECT 1 FROM watchlist WHERE auction_id = ? AND user_id = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, auctionId);
            ps.setString(2, userId);
            rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error in isFollowing check for auctionId: " + auctionId, sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in WatchlistDAO.isFollowing: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }
}