package com.ssscloud.auction.server.dao;

import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.BidTransaction;

public class BidTransactionDAO extends BaseDAO {
    // Logging Standards: Declared first
    private static final Logger logger = Logger.getLogger(BidTransactionDAO.class.getName());

    // --- PUBLIC METHODS ---

    public boolean saveBidTransaction(BidTransaction bidTransaction) throws DAOException, Exception {
        logger.log(Level.INFO, "Initiating bid transaction persistence for auctionId: {0}", bidTransaction.getAuctionId());
        String sqlBidTransaction = "INSERT INTO bid_transaction (auction_id, bidder_id, bidder_username, bid_amount, bid_time, bid_type) VALUES (?, ?, ?, ?, ?, ?)";
        Connection        connection = null;
        PreparedStatement ps         = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            ps = connection.prepareStatement(sqlBidTransaction);
            ps.setString(1, bidTransaction.getAuctionId());
            ps.setString(2, bidTransaction.getBidderId());
            ps.setString(3, bidTransaction.getBidderUsername());
            ps.setLong(4, bidTransaction.getBidAmount());
            ps.setObject(5, bidTransaction.getBidTime());
            ps.setString(6, bidTransaction.getType().name());
            ps.executeUpdate();

            connection.commit();
            logger.log(Level.INFO, "Bid transaction successfully persisted.");
            return true;
        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.DATA_INTEGRITY_VIOLATION, "Constraint violation: Bid transaction already exists.");
        } catch (SQLException sqlException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.BID_SAVE_FAILED, "Database interaction failure during bid persistence.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in BidTransactionDAO.saveBidTransaction", exception);
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeConnect(connection);
            closeResource(ps);
        }
    }

    public boolean saveBidTransaction(Connection connection, BidTransaction bidTransaction) throws DAOException, Exception {
        String sqlBidTransaction = "INSERT INTO bid_transaction (auction_id, bidder_id, bidder_username, bid_amount, bid_time, bid_type) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps         = null;
        try {
            // Transaction management is handled by the caller (shared connection)
            ps = connection.prepareStatement(sqlBidTransaction);
            ps.setString(1, bidTransaction.getAuctionId());
            ps.setString(2, bidTransaction.getBidderId());
            ps.setString(3, bidTransaction.getBidderUsername());
            ps.setLong(4, bidTransaction.getBidAmount());
            ps.setObject(5, bidTransaction.getBidTime());
            ps.setString(6, bidTransaction.getType().name());
            ps.executeUpdate();

            logger.log(Level.INFO, "Bid transaction successfully persisted using shared connection.");
            return true;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.BID_SAVE_FAILED, "Database interaction failure during shared connection bid persistence.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in BidTransactionDAO.saveBidTransaction (shared)", exception);
            throw exception;
        } finally {
            closeResource(ps);
        }
    }

    public BidTransaction findHighest(String auctionId) throws DAOException, Exception {
        String sql = "SELECT b.auction_id, b.bidder_id, bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
                "FROM bid_transaction b " +
                "WHERE b.auction_id = ? " +
                "ORDER BY b.bid_amount DESC, b.bid_time ASC " +
                "LIMIT 1";
        Connection        connection = null;
        PreparedStatement ps         = null;
        ResultSet         rs         = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, auctionId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapRowToBidTransaction(rs);
            }
            logger.log(Level.INFO, "No bid records found for auctionId: {0}", auctionId);
            return null;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.BID_FETCH_FAILED, "Database failure while retrieving the highest bid.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in BidTransactionDAO.findHighest", exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(rs, ps);
        }
    }

    public List<BidTransaction> findByBidderId(String bidderId) throws DAOException, Exception {
        String sql = "SELECT b.auction_id, b.bidder_id, bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
                "FROM bid_transaction b " +
                "WHERE b.bidder_id = ? " +
                "ORDER BY b.bid_time ASC";
        Connection           connection         = null;
        PreparedStatement    ps                 = null;
        ResultSet            rs                 = null;
        List<BidTransaction> bidTransactionList = new ArrayList<>();

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, bidderId);
            rs = ps.executeQuery();

            while (rs.next()) {
                bidTransactionList.add(mapRowToBidTransaction(rs));
            }
            logger.log(Level.INFO, "Successfully retrieved {0} bid(s) for bidderId: {1}", new Object[]{bidTransactionList.size(), bidderId});
            return bidTransactionList;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.BID_FETCH_FAILED, "Database failure while retrieving bids by bidderId.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in BidTransactionDAO.findByBidderId", exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(rs, ps);
        }
    }

    public List<BidTransaction> findByAuctionId(String auctionId) throws DAOException, Exception {
        String sql = "SELECT b.auction_id, b.bidder_id, bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
                "FROM bid_transaction b " +
                "WHERE b.auction_id = ? " +
                "ORDER BY b.bid_time ASC";
        Connection           connection         = null;
        PreparedStatement    ps                 = null;
        ResultSet            rs                 = null;
        List<BidTransaction> bidTransactionList = new ArrayList<>();

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, auctionId);
            rs = ps.executeQuery();

            while (rs.next()) {
                bidTransactionList.add(mapRowToBidTransaction(rs));
            }
            logger.log(Level.INFO, "Successfully retrieved {0} bid(s) for auctionId: {1}", new Object[]{bidTransactionList.size(), auctionId});
            return bidTransactionList;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.BID_FETCH_FAILED, "Database failure while retrieving bid history by auctionId.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in BidTransactionDAO.findByAuctionId", exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(rs, ps);
        }
    }

    // --- PRIVATE METHODS ---

    private BidTransaction mapRowToBidTransaction(ResultSet rs) throws SQLException {
        String auctionId = rs.getString("auction_id");
        String bidderId = rs.getString("bidder_id");
        String bidderUsername = rs.getString("bidder_username");
        long bidAmount = rs.getLong("bid_amount");
        LocalDateTime bidTime = toLocalDateTime(rs.getTimestamp("bid_time"));
        BidType bidType = BidType.valueOf(rs.getString("bid_type"));
        return new BidTransaction(auctionId, bidderId, bidderUsername, bidAmount, bidTime, bidType);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}