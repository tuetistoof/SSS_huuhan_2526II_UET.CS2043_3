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

/**
 * WatchlistDAO handles persistence operations for user watchlists.
 */
public class WatchlistDAO extends BaseDAO {
    private static final Logger logger = Logger.getLogger(WatchlistDAO.class.getName());

    // ── Watch ─────────────────────────────────────────────────────────────────

    public boolean add(String auctionId, String userId) throws SQLException, Exception {
        String sql = "INSERT INTO watchlist (auction_id, user_id) VALUES (?, ?)";
        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, auctionId);
            preparedStatement.setString(2, userId);
            preparedStatement.executeUpdate();
            logger.log(Level.INFO, "Watchlist entry created: userId " + userId + " watching auctionId " + auctionId);
            return true;
        } catch (SQLIntegrityConstraintViolationException constraintException) {
            logger.log(Level.WARNING, "Watchlist entry already exists: userId " + userId + " is already watching auctionId " + auctionId);
            return false;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error adding watchlist entry for userId: " + userId, sqlException);
            throw sqlException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in WatchlistDAO.add: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    // ── Unwatch ───────────────────────────────────────────────────────────────

    public boolean remove(String auctionId, String userId) throws SQLException, Exception {
        String sql = "DELETE FROM watchlist WHERE auction_id = ? AND user_id = ?";
        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, auctionId);
            preparedStatement.setString(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            logger.log(Level.INFO, "Watchlist entry removed: userId " + userId + " unwatching auctionId " + auctionId);
            return rowsAffected > 0;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error removing watchlist entry for userId: " + userId, sqlException);
            throw sqlException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in WatchlistDAO.remove: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public List<AuctionDisplayInfoDTO> findWatchlistDetailsByUser(String userId) throws SQLException, Exception {
    List<AuctionDisplayInfoDTO> auctionDetailsList = new ArrayList<>();
    
    String sql = 
        "SELECT a.id, " +
        "       e.name AS auction_name, " +          
        "       ac.end_time, " +                     
        "       u_seller.username AS seller_username, " + 
        "       ei.name AS item_name, " +            
        "       i.type AS item_type, " +             
        "       COALESCE(last_bid.bid_amount, ac.start_price) AS current_price, " + 
        "       (SELECT img.image_url FROM item_image_url img " +
        "        WHERE img.item_id = a.item_id LIMIT 1) AS image_url " + 
        "FROM watchlist w " +
        "JOIN auction a ON w.auction_id = a.id " +
        "JOIN auction_config ac ON a.id = ac.id " +
        "JOIN entity e ON a.id = e.id " +            
        "JOIN user u_seller ON a.seller_id = u_seller.id " + 
        "JOIN item i ON a.item_id = i.id " +
        "JOIN entity ei ON i.id = ei.id " +          
        "LEFT JOIN ( " +
        "    SELECT b1.auction_id, b1.bid_amount FROM bid_transaction b1 " +
        "    WHERE b1.bid_time = (SELECT MAX(b2.bid_time) FROM bid_transaction b2 " +
        "                         WHERE b2.auction_id = b1.auction_id) " +
        ") AS last_bid ON last_bid.auction_id = a.id " +
        "WHERE w.user_id = ?";

    Connection connection = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;

    try {
        connection = getConnection();
        preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, userId);
        resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            AuctionDisplayInfoDTO dto = new AuctionDisplayInfoDTO();
            dto.setId(resultSet.getString("id"));
            dto.setAuctionName(resultSet.getString("auction_name"));
            dto.setItemName(resultSet.getString("item_name"));
            dto.setItemType(resultSet.getString("item_type"));
            dto.setCurrentPrice(resultSet.getLong("current_price"));
            
            Timestamp endTimeStamp = resultSet.getTimestamp("end_time");
            if (endTimeStamp != null) {
                dto.setEndTime(endTimeStamp.toLocalDateTime());
            }
            
            dto.setSellerUsername(resultSet.getString("seller_username"));
            dto.setImageUrl(List.of(resultSet.getString("image_url")));
            
            auctionDetailsList.add(dto);
        }
        logger.log(Level.INFO, "Retrieved " + auctionDetailsList.size() + " detailed watchlist items for userId: " + userId);
    } catch (SQLException sqlException) {
        logger.log(Level.SEVERE, "Database error retrieving detailed watchlist for userId: " + userId, sqlException);
        throw sqlException;
    } catch (Exception exception) {
        logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in WatchlistDAO.findWatchlistDetailsByUser: " + exception.getMessage(), exception);
        throw exception;
    } finally {
        closeResource(resultSet, preparedStatement);
        closeConnect(connection);
    }
    return auctionDetailsList;
}

    public List<String> findUserIdsByAuction(String auctionId) throws SQLException, Exception {
        String sql = "SELECT user_id FROM watchlist WHERE auction_id = ?";
        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet         = null;
        List<String>      userIdList        = new ArrayList<>();
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, auctionId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                userIdList.add(resultSet.getString("user_id"));
            }
            return userIdList;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error retrieving userIds for auctionId: " + auctionId, sqlException);
            throw sqlException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in WatchlistDAO.findUserIdsByAuction: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }

    public boolean isFollowing(String auctionId, String userId) throws SQLException, Exception {
        String sql = "SELECT 1 FROM watchlist WHERE auction_id = ? AND user_id = ?";
        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet         = null;
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, auctionId);
            preparedStatement.setString(2, userId);
            resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error checking follow status for userId: " + userId, sqlException);
            throw sqlException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in WatchlistDAO.isFollowing: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }
}