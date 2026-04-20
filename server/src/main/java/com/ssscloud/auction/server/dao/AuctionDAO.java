package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.model.base.AuctionConfig;

public class AuctionDAO extends BaseDAO {
    public boolean saveAuction(Auction auction) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlAuctionConfig = "INSERT INTO auction_config (id, name, start_price, min_increment, start_time, end_time, extend_second, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlAuction = "INSERT INTO auction (id, status, seller_id, item_id) VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement psEntity = null, psAuctionConfig = null, psAuction = null, psBidTransaction = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString(1, auction.getAuctionConfig().getId());
            psEntity.setString(2, auction.getAuctionConfig().getName());
            psEntity.executeUpdate();

            psAuctionConfig = conn.prepareStatement(sqlAuctionConfig);
            psAuctionConfig.setString(1, auction.getAuctionConfig().getId());
            psAuctionConfig.setString(1, auction.getAuctionConfig().getName());
            psAuctionConfig.setLong(3, auction.getAuctionConfig().getStartPrice());
            psAuctionConfig.setLong(4, auction.getAuctionConfig().getMinIncrement());
            psAuctionConfig.setObject(5, auction.getAuctionConfig().getStartTime());
            psAuctionConfig.setObject(6, auction.getAuctionConfig().getEndTime());
            psAuctionConfig.setInt(7, auction.getAuctionConfig().getExtendSecond());
            psAuctionConfig.setString(8, auction.getAuctionConfig().getDescription());
            psAuctionConfig.executeQuery();

            psAuction = conn.prepareStatement(sqlAuction);
            psAuction.setString(1, auction.getAuctionConfig().getId());
            psAuction.setString(2, auction.getStatus().name());
            psAuction.setString(3, auction.getSellerId());
            psAuction.setString(4, auction.getItemId());

            BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
            for (BidTransaction bidTransaction : auction.getbidTransaction()) {
                bidTransactionDAO.saveBidTransaction(bidTransaction);
            }
            conn.commit();
            logger.info("da luu auction: " + auction.getAuctionConfig().getName());
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("aution name da ton tai: " + auction.getAuctionConfig().getName() + " - " + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi kh luu aution: " + auction.getAuctionConfig().getName() + " - " + e.getMessage());
            safelyRollback(conn);
            return false;
        } finally {
            resetAutocommit(conn);
            closeResource(psEntity, psAuction, psAuctionConfig, psBidTransaction);
        }
    }

    public List<Auction> findBySellerId(String sellerId) {
        String sql = "SELECT a.id, a.status, a.seller_id, a.item_id, " +
                "ac.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, ac.description, " +
                "b.bidder_id, b.bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
                "FROM auction a " +
                "JOIN auction_config ac ON a.id = ac.id " +
                "LEFT JOIN bid_transaction b ON a.id = b.auction_id " +
                "WHERE a.seller_id = ? " +
                "ORDER BY b.bid_time DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Auction> list = new ArrayList<>();

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, sellerId);
            rs = ps.executeQuery();
            while (rs.next())
                list.add(mapResultSetToAuction(rs));
            return list;

        } catch (SQLException e) {
            logger.severe("Lỗi findBySellerId [" + sellerId + "]: " + e.getMessage());
            return list;
        } finally {
            closeResource(rs, ps);
        }
    }

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        String id = rs.getString ("id");
        String name = rs.getString ("name");
        long startPrice = rs.getLong("start_price");
        long minIncrement = rs.getLong("min_increment");
        LocalDateTime startTime = toLocalDateTime(rs.getTimestamp("start_time"));
        LocalDateTime endTime = toLocalDateTime(rs.getTimestamp("end_time"));
        int extendSecond = rs.getInt("extend_second");
        String description = rs.getString("description");

        AuctionConfig config = new AuctionConfig(id, name, startPrice, minIncrement, startTime, endTime, extendSecond, description);

        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
        String sellerId = rs.getString("seller_id");
        String itemId = rs.getString("item_id");

        List<BidTransaction> bidTransaction = new ArrayList<>();
        do {
            if (rs.getString("bidder_id") != null) {
                String auctionId = rs.getString("auction_id");
                String bidderId = rs.getString("bidder_id");
                String bidderUsername = rs.getString("bidder_username");
                long bidAmount = rs.getLong("bid_amount");
                LocalDateTime bidTime = toLocalDateTime(rs.getTimestamp("bid_time"));
                BidType bidType = BidType.valueOf(rs.getString("bid_type"));
                bidTransaction.add(new BidTransaction(auctionId, bidderId, bidderUsername, bidAmount, bidTime, bidType));
            }
        } while (rs.next());

        return new Auction(config, status, sellerId, itemId, bidTransaction);

    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
