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

import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.BidTransaction;

public class BidTransactionDAO extends BaseDAO {
    public boolean saveBidTransaction(BidTransaction bidTransaction) {
        String sqlBidTransaction = "INSERT INTO bid_transaction (auction_id, bidder_id, bidder_username, bid_amount, bid_time, bid_type) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement psBidTransaction = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psBidTransaction = conn.prepareStatement(sqlBidTransaction);
            psBidTransaction.setString(1, bidTransaction.getAuctionId());
            psBidTransaction.setString(2, bidTransaction.getBidderId());
            psBidTransaction.setString(3, bidTransaction.getBidderUsername());
            psBidTransaction.setLong(4, bidTransaction.getBidAmount());
            psBidTransaction.setObject(5, bidTransaction.getBidTime());
            psBidTransaction.setString(6, bidTransaction.getType().name());
            psBidTransaction.executeQuery();

            conn.commit();
            logger.info("da luu bidTransaction");
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("User name da ton tai: " + e.getMessage());
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

    public boolean saveBidTransaction(Connection conn, BidTransaction bidTransaction) {
        String sqlBidTransaction = "INSERT INTO bid_transaction (auction_id, bidder_id, bidder_username, bid_amount, bid_time, bid_type) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement psBidTransaction = null;
        try {
            conn.setAutoCommit(false);

            psBidTransaction = conn.prepareStatement(sqlBidTransaction);
            psBidTransaction.setString(1, bidTransaction.getAuctionId());
            psBidTransaction.setString(2, bidTransaction.getBidderId());
            psBidTransaction.setString(3, bidTransaction.getBidderUsername());
            psBidTransaction.setLong(4, bidTransaction.getBidAmount());
            psBidTransaction.setObject(5, bidTransaction.getBidTime());
            psBidTransaction.setString(6, bidTransaction.getType().name());
            psBidTransaction.executeQuery();

            conn.commit();
            logger.info("da luu bidTransaction");
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("User name da ton tai: " + e.getMessage());
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

    public BidTransaction findHighest (String auctionId) {
        String sql = "SELECT b.auction_id, b.bidder_id, bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
                "FROM bid_transaction b " +
                "WHERE b.auction_id = ? " +
                "ORDER BY b.bid_amount DESC, b.bid_time ASC " +
                "LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, auctionId);
            rs = ps.executeQuery();

            while (rs.next()) {
                return mapResultSetToBid(rs);
            }
            logger.info("findByAuctionId auctionId=" + auctionId + " - " +  " bids");
            return null;

        } catch (SQLException e) {
            logger.severe("Lỗi findByAuctionId auctionId=" + auctionId + ": " + e.getMessage());
            return null;
        } finally {
            closeResource(rs, ps);
        }
    }

    public List<BidTransaction> findByBidderId(String bidderId) {
        String sql = "SELECT b.auction_id, b.bidder_id, bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
                "FROM bid_transaction b " +
                "WHERE b.bidder_id = ? " +
                "ORDER BY b.bid_time DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<BidTransaction> list = new ArrayList<>();

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, bidderId);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSetToBid(rs));
            }
            logger.info("findByBidderId auctionId=" + bidderId + " - " + list.size() + " bids");
            return list;

        } catch (SQLException e) {
            logger.severe("Lỗi findByBidderId auctionId=" + bidderId + ": " + e.getMessage());
            return list;
        } finally {
            closeResource(rs, ps);
        }
    }

    public List<BidTransaction> findByAuctionId(String auctionId) {
        String sql = "SELECT b.auction_id, b.bidder_id, bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
                "FROM bid_transaction b " +
                "WHERE b.auction_id = ? " +
                "ORDER BY b.bid_time DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<BidTransaction> list = new ArrayList<>();

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, auctionId);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapResultSetToBid(rs));
            }
            logger.info("findByAuctionId auctionId=" + auctionId + " - " + list.size() + " bids");
            return list;

        } catch (SQLException e) {
            logger.severe("Lỗi findByAuctionId auctionId=" + auctionId + ": " + e.getMessage());
            return list;
        } finally {
            closeResource(rs, ps);
        }
    }

    private BidTransaction mapResultSetToBid(ResultSet rs) throws SQLException {
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
