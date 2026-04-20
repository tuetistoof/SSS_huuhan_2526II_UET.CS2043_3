package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import com.ssscloud.auction.common.model.Bidder;

public class AuctionDAO extends BaseDAO{
    public boolean saveBidTransaction(BidTransactionDAO bidTransaction) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlAuctionConfig = "INSERT INTO auction_config (id, start_price, min_increment, start_time, end_time, extend_second, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlAuction = "INSERT INTO auction (id, status, seller_id, item_id, current_price, highest_bidder_id, highest_bidder_name, bid_count, bid_time, bid_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlBidTransaction = "INSERT INTO bid_transaction (auction_id, bid_amount, bidder_id, bidder_username, bid_time, bid_type) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement psEntity = null, psUser = null, psBidTransaction = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString(1, BidTransactionDAO.getId());
            psEntity.setString(2, bidder.getName());
            psEntity.executeUpdate();

            psUser = conn.prepareStatement(sqlUser);
            psUser.setString(1, bidder.getId());
            psUser.setString(2, bidder.getUserName());
            psUser.setString(3, bidder.getPassword());
            psUser.setString(4, bidder.getEmail());
            psUser.setString(5, bidder.getRole().name());
            psUser.executeUpdate();

            psBidder = conn.prepareStatement(sqlBidder);
            psBidder.setString(1, bidder.getId());
            psBidder.setLong(2, bidder.getAccountBalance());
            psBidder.executeUpdate();

            conn.commit();
            logger.info("da luu bidder: " + bidder.getUserName());
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("User name da ton tai: " + bidder.getUserName() + " - " + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi kh luu Bidder: " + bidder.getUserName() + " - " + e.getMessage());
            safelyRollback(conn);
            return false;
        } finally {
            resetAutocommit(conn);
            closeResource(psEntity, psUser, psBidder);
        }
    }
}
