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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.model.base.AuctionConfig;

public class AuctionDAO extends BaseDAO {
    // Logging Standards: Declared first
    private static final Logger logger = Logger.getLogger(AuctionDAO.class.getName()); 

    // --- PUBLIC METHODS ---

    public boolean saveAuction(Auction auction) throws DAOException, Exception {
        logger.log(Level.INFO, "Initiating auction persistence for: {0}", auction.getAuctionConfig().getName());

        String sqlEntity        = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlAuctionConfig = "INSERT INTO auction_config (id, start_price, min_increment, start_time, end_time, extend_second) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlAuction       = "INSERT INTO auction (id, status, seller_id, item_id) VALUES (?, ?, ?, ?)";

        Connection        connection      = null;
        PreparedStatement psEntity        = null;
        PreparedStatement psAuctionConfig = null;
        PreparedStatement psAuction       = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            // 1. Entity - Persist auction name
            psEntity = connection.prepareStatement(sqlEntity);
            psEntity.setString(1, auction.getAuctionConfig().getId());
            psEntity.setString(2, auction.getAuctionConfig().getName());
            psEntity.executeUpdate();

            // 2. Auction Config
            psAuctionConfig = connection.prepareStatement(sqlAuctionConfig);
            psAuctionConfig.setString(1, auction.getAuctionConfig().getId());
            psAuctionConfig.setLong(2,   auction.getAuctionConfig().getStartPrice());
            psAuctionConfig.setLong(3,   auction.getAuctionConfig().getMinIncrement());
            psAuctionConfig.setObject(4, auction.getAuctionConfig().getStartTime());
            psAuctionConfig.setObject(5, auction.getAuctionConfig().getEndTime());
            psAuctionConfig.setInt(6,    auction.getAuctionConfig().getExtendSecond());
            psAuctionConfig.executeUpdate();

            // 3. Auction
            psAuction = connection.prepareStatement(sqlAuction);
            psAuction.setString(1, auction.getAuctionConfig().getId());
            psAuction.setString(2, auction.getStatus().name());
            psAuction.setString(3, auction.getSellerId());
            psAuction.setString(4, auction.getItemId());
            psAuction.executeUpdate();

            // 4. Bid transactions - Dependency Injection: Short name
            BidTransactionDAO bidDAO = new BidTransactionDAO();
            for (BidTransaction bidTransaction : auction.getBidTransaction()) {
                bidDAO.saveBidTransaction(connection, bidTransaction);
            }

            connection.commit();
            logger.log(Level.INFO, "Auction successfully persisted: {0}", auction.getAuctionConfig().getName());
            return true;

        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.DATA_INTEGRITY_VIOLATION, "Constraint violation: Auction or entity already exists.", sqlConstraintException);
        } catch (SQLException sqlException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.AUCTION_SAVE_FAILED, "Database interaction failure during auction persistence.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeResource(psEntity, psAuctionConfig, psAuction);
            closeConnect(connection);
        }
    }

    public List<Auction> findBySellerId(String sellerId) throws DAOException, Exception {
        String sql =
            "SELECT a.id AS auction_id, a.status, a.seller_id, a.item_id, " +
            "       e.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
            "       b.auction_id AS b_auction_id, b.bidder_id, b.bidder_username, b.bid_amount, b.locked_balance, b.bid_time, b.bid_type " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +
            "LEFT JOIN bid_transaction b ON a.id = b.auction_id " +
            "WHERE a.seller_id = ? " +
            "ORDER BY b.bid_time ASC";

        Connection           connection = null;
        PreparedStatement    ps         = null;
        ResultSet            rs         = null;
        Map<String, Auction> auctionMap = new LinkedHashMap<>();

        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            ps.setString(1, sellerId);
            rs = ps.executeQuery();

            while (rs.next()) {
                String auctionId = rs.getString("auction_id");
                Auction auction = auctionMap.get(auctionId);
                if (auction == null) {
                    auction = mapRowToAuction(rs);
                    auctionMap.put(auctionId, auction);
                }
                if (rs.getString("bidder_id") != null) {
                    auction.placeBid(mapRowToBid(rs));
                }
            }
            return new ArrayList<>(auctionMap.values());
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.AUCTION_FETCH_FAILED, "Database interaction failure while retrieving auctions by sellerId.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public Auction findByAuctionId(String auctionId) throws DAOException, Exception {
        String sql =
            "SELECT a.id AS auction_id, a.status, a.seller_id, a.item_id, " +
            "       e.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
            "       b.auction_id AS b_auction_id, b.bidder_id, b.bidder_username, b.bid_amount, b.locked_balance, b.bid_time, b.bid_type " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +
            "LEFT JOIN bid_transaction b ON a.id = b.auction_id " +
            "WHERE a.id = ? " +
            "ORDER BY b.bid_time ASC";

        Connection        connection = null;
        PreparedStatement ps      = null;
        ResultSet         rs      = null;
        Auction           auction = null;

        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            ps.setString(1, auctionId);
            rs = ps.executeQuery();

            while (rs.next()) {
                if (auction == null) {
                    auction = mapRowToAuction(rs);
                }
                if (rs.getString("bidder_id") != null) {
                    auction.placeBid(mapRowToBid(rs));
                }
            }
            return auction;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.AUCTION_FETCH_FAILED, "Database interaction failure while retrieving auction by ID.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public List<Auction> findByStatus(AuctionStatus status) throws DAOException, Exception {
        String sql =
            "SELECT a.id AS auction_id, a.status, a.seller_id, a.item_id, " +
            "       e.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
            "       b.auction_id AS b_auction_id, b.bidder_id, b.bidder_username, b.bid_amount, b.locked_balance, b.bid_time, b.bid_type " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +
            "LEFT JOIN bid_transaction b ON a.id = b.auction_id " +
            "WHERE a.status = ? " +
            "ORDER BY b.bid_time ASC";

        Connection           connection = null;
        PreparedStatement    ps         = null;
        ResultSet            rs         = null;
        Map<String, Auction> auctionMap = new LinkedHashMap<>();

        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            ps.setString(1, status.name());
            rs = ps.executeQuery();

            while (rs.next()) {
                String auctionId = rs.getString("auction_id");
                Auction auction = auctionMap.get(auctionId);
                if (auction == null) {
                    auction = mapRowToAuction(rs);
                    auctionMap.put(auctionId, auction);
                }
                if (rs.getString("bidder_id") != null) {
                    auction.placeBid(mapRowToBid(rs));
                }
            }
            return new ArrayList<>(auctionMap.values());
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.AUCTION_FETCH_FAILED, "Database interaction failure while retrieving auctions by status.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }


    public boolean updateEndTime(String auctionId, LocalDateTime newEndTime) throws DAOException, Exception {
        String sql = "UPDATE auction_config SET end_time = ? WHERE id = ?";
        Connection        connection = null;
        PreparedStatement ps   = null;
        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            ps.setObject(1, newEndTime);
            ps.setString(2, auctionId);
            boolean isUpdated = ps.executeUpdate() > 0;
            if (isUpdated) {
                logger.log(Level.INFO, "Successfully updated end time for auctionId: {0}", auctionId);
            }
            return isUpdated;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.AUCTION_UPDATE_FAILED, "Database interaction failure while updating auction end time.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    public boolean updateStatus(String auctionId, AuctionStatus newStatus) throws DAOException, Exception {
        String sql = "UPDATE auction SET status = ? WHERE id = ?";
        Connection        connection = null;
        PreparedStatement ps   = null;
        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            ps.setString(1, newStatus.name());
            ps.setString(2, auctionId);
            int rows = ps.executeUpdate();
            logger.log(Level.INFO, "Successfully updated status for auctionId: {0}", auctionId);
            return rows > 0;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.AUCTION_UPDATE_FAILED, "Database interaction failure while updating auction status.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    public boolean deleteById(String auctionId) throws DAOException, Exception {
        String sql = "DELETE FROM auction WHERE id = ?";
        Connection        connection = null;
        PreparedStatement ps   = null;
        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            ps.setString(1, auctionId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                logger.log(Level.WARNING, "Resource not found for deletion: auctionId {0}", auctionId);
            }
            return rows > 0;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.AUCTION_DELETE_FAILED, "Database interaction failure while deleting auction.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    // --- PRIVATE METHODS ---

    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        AuctionConfig config = new AuctionConfig(
            rs.getString("auction_id"),                         // a.id AS auction_id
            rs.getString("name"),                               // e.name
            rs.getLong("start_price"),
            rs.getLong("min_increment"),
            toLocalDateTime(rs.getTimestamp("start_time")),
            toLocalDateTime(rs.getTimestamp("end_time")),
            rs.getInt("extend_second")
        );
        return new Auction(
            config,
            AuctionStatus.valueOf(rs.getString("status")),
            rs.getString("seller_id"),
            rs.getString("item_id"),
            new ArrayList<>()
        );
    }

    /**
     * Map một row từ JOIN query sang BidTransaction.
     *
     * Lý do dùng alias "b_auction_id" thay vì "auction_id":
     *   - JOIN query đã có  a.id AS auction_id  (dùng cho auction)
     *   - Nếu đọc rs.getString("auction_id") thì JDBC trả về cột đầu tiên
     *     khớp tên, tức là a.id — không phải b.auction_id — gây bug silent.
     *   - Đặt alias riêng b.auction_id AS b_auction_id loại bỏ hoàn toàn
     *     sự nhập nhằng này.
     */
    private BidTransaction mapRowToBid(ResultSet rs) throws SQLException {
        return new BidTransaction(
            rs.getString("b_auction_id"),                       // b.auction_id AS b_auction_id
            rs.getString("bidder_id"),
            rs.getString("bidder_username"),
            rs.getLong("bid_amount"),
            rs.getLong("locked_balance"),
            toLocalDateTime(rs.getTimestamp("bid_time")),
            BidType.valueOf(rs.getString("bid_type"))
        );
    }


    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}