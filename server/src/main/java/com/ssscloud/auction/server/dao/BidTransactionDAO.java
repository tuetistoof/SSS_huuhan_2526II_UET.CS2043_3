package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import com.ssscloud.auction.common.model.BidTransaction;

public class BidTransactionDAO extends BaseDAO{
    public boolean saveBidTransaction(BidTransaction bidTransaction) {
        String sqlBidTransaction = "INSERT INTO bid_transaction (auction_id, bidder_id, bidder_username, bid_amount, bid_time, bid_type) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement psBidTransaction = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            
            psBidTransaction = conn.prepareStatement(sqlBidTransaction);
            psBidTransaction.setString(1, bidTransaction.getAuctionId());
            psBidTransaction.setString(2,bidTransaction.getBidderId());
            psBidTransaction.setString(3, bidTransaction.getBidderUsername());
            psBidTransaction.setLong(4, bidTransaction.getBidAmount());
            psBidTransaction.setObject(5, bidTransaction.getBidTime());
            psBidTransaction.setString(6,bidTransaction.getType().name());
            psBidTransaction.executeQuery();

            conn.commit();
            logger.info("da luu bidTransaction" );
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("User name da ton tai: "  + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi kh luu bidTransaction: " + e.getMessage());
            safelyRollback(conn);
            return false;
        } finally {
            resetAutocommit(conn);
            closeResource(psBidTransaction);
        }
    }
}
