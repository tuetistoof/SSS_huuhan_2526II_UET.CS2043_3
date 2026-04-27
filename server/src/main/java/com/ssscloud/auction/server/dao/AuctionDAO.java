package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.model.base.AuctionConfig;

public class AuctionDAO extends BaseDAO {
    public boolean saveAuction(Auction auction) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlAuctionConfig = "INSERT INTO auction_config (id, start_price, min_increment, start_time, end_time, extend_second) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlAuction = "INSERT INTO auction (id, status, seller_id, item_id) VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement psEntity = null, psAuctionConfig = null, psAuction = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString(1, auction.getAuctionConfig().getId());
            psEntity.setString(2, auction.getAuctionConfig().getName());
            psEntity.executeUpdate();

            psAuctionConfig = conn.prepareStatement(sqlAuctionConfig);
            psAuctionConfig.setString(1, auction.getAuctionConfig().getId());
            psAuctionConfig.setLong (2, auction.getAuctionConfig().getStartPrice());
            psAuctionConfig.setLong(3, auction.getAuctionConfig().getMinIncrement());
            psAuctionConfig.setObject(4, auction.getAuctionConfig().getStartTime());
            psAuctionConfig.setObject(5, auction.getAuctionConfig().getEndTime());
            psAuctionConfig.setInt(6, auction.getAuctionConfig().getExtendSecond());
            psAuctionConfig.executeUpdate();

            psAuction = conn.prepareStatement(sqlAuction);
            psAuction.setString(1, auction.getAuctionConfig().getId());
            psAuction.setString(2, auction.getStatus().name());
            psAuction.setString(3, auction.getSellerId());
            psAuction.setString(4, auction.getItemId());
            psAuction.executeUpdate();

            BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
            for (BidTransaction bidTransaction : auction.getBidTransaction()) {
                bidTransactionDAO.saveBidTransaction(conn, bidTransaction);
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
            closeResource(psEntity, psAuction, psAuctionConfig);
        }
    }

    public List<Auction> findBySellerId(String sellerId) {
        String sql = "SELECT a.id, a.status, a.seller_id, a.item_id, " +
                "ac.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
                "b.bidder_id, b.bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
                "FROM auction a " +
                "JOIN auction_config ac ON a.id = ac.id " +
                "LEFT JOIN bid_transaction b ON a.id = b.auction_id " +
                "WHERE a.seller_id = ? " +
                "ORDER BY b.bid_time DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        Map<String, Auction> auctionMap = new LinkedHashMap<>();
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, sellerId);
            rs = ps.executeQuery();

            while (rs.next()) {
                String auctionId = rs.getString("id");

                Auction auction = auctionMap.get(auctionId);
                if (auction == null) {
                    auction = mapResultSetToAuction(rs);
                    auctionMap.put(auctionId, auction);
                }
                if (rs.getString("bidder_id") != null) {
                    auction.getBidTransaction().add(mapResultSetToBid(rs));
                }
            }

            return new ArrayList<>(auctionMap.values());

        } catch (

        SQLException e) {
            logger.severe("Lỗi findBySellerId [" + sellerId + "]: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            closeResource(rs, ps);
        }
    }

    public Auction findByAuctionId(String id) {
        String sql = "SELECT a.id, a.status, a.seller_id, a.item_id, " +
                "ac.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
                "b.bidder_id, b.bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
                "FROM auction a " +
                "JOIN auction_config ac ON a.id = ac.id " +
                "LEFT JOIN bid_transaction b ON a.id = b.auction_id " +
                "WHERE a.id = ? " +
                "ORDER BY b.bid_time DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Auction auction = null; 

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();

            while (rs.next()) {
                if (auction == null) {
                    auction = mapResultSetToAuction(rs);
                }
                if (rs.getString("bidder_id") != null) {
                    auction.getBidTransaction().add(mapResultSetToBid(rs));
                }
            }

            return auction; // null nếu không tìm thấy

        } catch (SQLException e) {
            logger.severe("Lỗi findByAuctionId [" + id + "]: " + e.getMessage());
            return null;
        } finally {
            closeResource(rs, ps);
        }
    }

    public List<Auction> findByStatus(AuctionStatus status) {
        String sql = "SELECT a.id, a.status, a.seller_id, a.item_id, " +
                "ac.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
                "b.bidder_id, b.bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
                "FROM auction a " +
                "JOIN auction_config ac ON a.id = ac.id " +
                "LEFT JOIN bid_transaction b ON a.id = b.auction_id " +
                "WHERE a.status = ? " +
                "ORDER BY b.bid_time DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        Map<String, Auction> auctionMap = new LinkedHashMap<>();
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status.name());
            rs = ps.executeQuery();

            while (rs.next()) {
                String auctionId = rs.getString("id");

                Auction auction = auctionMap.get(auctionId);
                if (auction == null) {
                    auction = mapResultSetToAuction(rs);
                    auctionMap.put(auctionId, auction);
                }
                if (rs.getString("bidder_id") != null) {
                    auction.getBidTransaction().add(mapResultSetToBid(rs));
                }
            }

            return new ArrayList<>(auctionMap.values());

        } catch (

        SQLException e) {
            logger.severe("Lỗi findByStatus [" + status.name() + "]: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            closeResource(rs, ps);
        }
    }

    public boolean updateStatus(String auctionId, AuctionStatus newStatus) {
        String sql = "UPDATE auction SET status = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus.name());
            ps.setString(2, auctionId);
            int rows = ps.executeUpdate();
            logger.info("updateStatus auctionId=" + auctionId + " -> " + newStatus);
            return rows > 0;

        } catch (SQLException e) {
            logger.severe("Lỗi updateStatus auctionId=" + auctionId + ": " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
        }
    }

    public boolean deleteById(long auctionId) {
        String sql = "DELETE FROM auction WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, auctionId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                logger.warning("deleteAuction id=" + auctionId + " - không thể xóa");
            }
            return rows > 0;

        } catch (SQLException e) {
            logger.severe("Lỗi deleteAuction id=" + auctionId + ": " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
        }
    }

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        long startPrice = rs.getLong("start_price");
        long minIncrement = rs.getLong("min_increment");
        LocalDateTime startTime = toLocalDateTime(rs.getTimestamp("start_time"));
        LocalDateTime endTime = toLocalDateTime(rs.getTimestamp("end_time"));
        int extendSecond = rs.getInt("extend_second");
        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
        String sellerId = rs.getString("seller_id");
        String itemId = rs.getString("item_id");

        AuctionConfig config = new AuctionConfig(id, name, startPrice, minIncrement, startTime, endTime, extendSecond);

        return new Auction(config, status, sellerId, itemId, new ArrayList<>());
    }

    // cai nay khac voi cai ben bidtransaction nhe
    private BidTransaction mapResultSetToBid(ResultSet rs) throws SQLException {
        String auctionId = rs.getString("id");
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
