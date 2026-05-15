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

import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.BidType;
import com.ssscloud.auction.common.exception.DAOExceptions;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceExceptions;
import com.ssscloud.auction.common.model.Auction;
import com.ssscloud.auction.common.model.BidTransaction;
import com.ssscloud.auction.common.model.base.AuctionConfig;

public class AuctionDAO extends BaseDAO {

    private static final Logger logger = Logger.getLogger(AuctionDAO.class.getName());

    // --- PUBLIC METHODS ---

    public boolean saveAuction(Auction auction) throws SQLException, Exception {
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

            // 4. Bid transactions - Use shared connection
            BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
            for (BidTransaction bidTransaction : auction.getBidTransaction()) {
                bidTransactionDAO.saveBidTransaction(connection, bidTransaction);
            }

            connection.commit();
            logger.log(Level.INFO, "Auction successfully persisted: " + auction.getAuctionConfig().getName());
            return true;

        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            logger.log(Level.WARNING, "Auction already exists: " + auction.getAuctionConfig().getName() + " - " + sqlConstraintException.getMessage());
            safelyRollback(connection);
            return false;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Persistence error in saveAuction for: " + auction.getAuctionConfig().getName(), sqlException);
            safelyRollback(connection);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionDAO.saveAuction: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeResource(psEntity, psAuctionConfig, psAuction);
            closeConnect(connection);
        }
    }

    public List<Auction> findBySellerId(String sellerId) throws SQLException, Exception {
        String sql =
            "SELECT a.id AS auction_id, a.status, a.seller_id, a.item_id, " +
            "       e.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
            "       b.auction_id AS b_auction_id, b.bidder_id, b.bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
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
                    auction.placeBid(mapRowToBidTransaction(rs));
                }
            }
            return new ArrayList<>(auctionMap.values());
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error retrieving auctions for sellerId: " + sellerId, sqlException);
            return new ArrayList<>();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionDAO.findBySellerId: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public Auction findByAuctionId(String auctionId) throws SQLException, Exception {
        String sql =
            "SELECT a.id AS auction_id, a.status, a.seller_id, a.item_id, " +
            "       e.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
            "       b.auction_id AS b_auction_id, b.bidder_id, b.bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
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
                    auction.placeBid(mapRowToBidTransaction(rs));
                }
            }
            return auction;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error retrieving auction for auctionId: " + auctionId, sqlException);
            return null;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionDAO.findByAuctionId: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public List<Auction> findByStatus(AuctionStatus status) throws SQLException, Exception {
        String sql =
            "SELECT a.id AS auction_id, a.status, a.seller_id, a.item_id, " +
            "       e.name, ac.start_price, ac.min_increment, ac.start_time, ac.end_time, ac.extend_second, " +
            "       b.auction_id AS b_auction_id, b.bidder_id, b.bidder_username, b.bid_amount, b.bid_time, b.bid_type " +
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
                    auction.placeBid(mapRowToBidTransaction(rs));
                }
            }
            return new ArrayList<>(auctionMap.values());
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error retrieving auctions for status: " + status.name(), sqlException);
            return new ArrayList<>();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionDAO.findByStatus: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public List<AuctionDisplayInfoDTO> findSellerAuction(String sellerId) throws SQLException, Exception {
        String sql =
            "SELECT a.id, " +
            "       e.name AS auction_name, ac.end_time, " +
            "       u.username AS seller_username, " +
            "       ei.name AS item_name, i.type AS item_type, " +
            "       COALESCE(last_bid.bid_amount, ac.start_price) AS current_price, " +
            "       GROUP_CONCAT(img.image_url SEPARATOR ', ') AS image_url " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +
            "JOIN user u ON a.seller_id = u.id " +
            "JOIN item i ON a.item_id = i.id " +
            "JOIN entity ei ON i.id = ei.id " +
            "LEFT JOIN item_image_url img ON a.item_id = img.item_id " +
            "LEFT JOIN ( " +
            "    SELECT b1.auction_id, b1.bid_amount FROM bid_transaction b1 " +
            "    WHERE b1.bid_time = (SELECT MAX(b2.bid_time) FROM bid_transaction b2 " +
            "                         WHERE b2.auction_id = b1.auction_id) " +
            ") AS last_bid ON last_bid.auction_id = a.id " +
            "WHERE a.seller_id = ? " +
            "GROUP BY a.id, e.name, ac.end_time, u.username, ei.name, i.type, ac.start_price, last_bid.bid_amount";

        Connection        connection = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            ps.setString(1, sellerId);
            rs = ps.executeQuery();

            List<AuctionDisplayInfoDTO> auctionDisplayInfoList = new ArrayList<>();
            while (rs.next()) {
                auctionDisplayInfoList.add(mapRowToDisplayDTO(rs));
            }
            return auctionDisplayInfoList;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error in findSellerAuction for sellerId: " + sellerId, sqlException);
            return new ArrayList<>();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionDAO.findSellerAuction: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public List<AuctionDisplayInfoDTO> findActiveAuctions() throws SQLException, Exception {
        String sql =
            "SELECT a.id, " +
            "       e.name AS auction_name, ac.end_time, " +
            "       u.username AS seller_username, " +
            "       ei.name AS item_name, i.type AS item_type, " +
            "       COALESCE(last_bid.bid_amount, ac.start_price) AS current_price, " +
            "       GROUP_CONCAT(img.image_url SEPARATOR ', ') AS image_url " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +
            "JOIN user u ON a.seller_id = u.id " +
            "JOIN item i ON a.item_id = i.id " +
            "JOIN entity ei ON i.id = ei.id " +
            "LEFT JOIN item_image_url img ON a.item_id = img.item_id " +
            "LEFT JOIN ( " +
            "    SELECT b1.auction_id, b1.bid_amount FROM bid_transaction b1 " +
            "    WHERE b1.bid_time = (SELECT MAX(b2.bid_time) FROM bid_transaction b2 " +
            "                         WHERE b2.auction_id = b1.auction_id) " +
            ") AS last_bid ON last_bid.auction_id = a.id " +
            "WHERE a.status IN ('OPEN', 'RUNNING') " +
            "GROUP BY a.id, e.name, ac.end_time, u.username, ei.name, i.type, ac.start_price, last_bid.bid_amount";

        Connection        connection = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            rs = ps.executeQuery();

            List<AuctionDisplayInfoDTO> auctionDisplayInfoList = new ArrayList<>();
            while (rs.next()) {
                auctionDisplayInfoList.add(mapRowToDisplayDTO(rs));
            }
            return auctionDisplayInfoList;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error retrieving active auctions", sqlException);
            return new ArrayList<>();
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionDAO.findActiveAuctions: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public boolean updateEndTime(String auctionId, LocalDateTime newEndTime) throws SQLException, Exception {
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
                logger.log(Level.INFO, "Updated end time for auctionId=" + auctionId + " to " + newEndTime);
            }
            return isUpdated;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error updating end time for auctionId: " + auctionId, sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionDAO.updateEndTime: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    public boolean updateStatus(String auctionId, AuctionStatus newStatus) throws SQLException, Exception {
        String sql = "UPDATE auction SET status = ? WHERE id = ?";
        Connection        connection = null;
        PreparedStatement ps   = null;
        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            ps.setString(1, newStatus.name());
            ps.setString(2, auctionId);
            int rows = ps.executeUpdate();
            logger.log(Level.INFO, "Updated status for auctionId=" + auctionId + " to " + newStatus);
            return rows > 0;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error updating status for auctionId: " + auctionId, sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionDAO.updateStatus: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    public boolean deleteById(String auctionId) throws SQLException, Exception {
        String sql = "DELETE FROM auction WHERE id = ?";
        Connection        connection = null;
        PreparedStatement ps   = null;
        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            ps.setString(1, auctionId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                logger.log(Level.WARNING, "Failed to delete auction with id=" + auctionId + " - Resource not found");
            }
            return rows > 0;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error deleting auction for auctionId: " + auctionId, sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in AuctionDAO.deleteById: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    // --- PRIVATE METHODS ---

    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        AuctionConfig auctionConfig = new AuctionConfig(
            rs.getString("auction_id"),
            rs.getString("name"),
            rs.getLong("start_price"),
            rs.getLong("min_increment"),
            toLocalDateTime(rs.getTimestamp("start_time")),
            toLocalDateTime(rs.getTimestamp("end_time")),
            rs.getInt("extend_second")
        );

        return new Auction(
            auctionConfig,
            AuctionStatus.valueOf(rs.getString("status")),
            rs.getString("seller_id"),
            rs.getString("item_id"),
            new ArrayList<>()
        );
    }

    /**
     * Maps a ResultSet row to a BidTransaction.
     * Uses alias "b_auction_id" to avoid column name ambiguity with "a.id AS auction_id"
     * from the parent JOIN query — reading "auction_id" directly would return the wrong column.
     */
    private BidTransaction mapRowToBidTransaction(ResultSet rs) throws SQLException {
        return new BidTransaction(
            rs.getString("b_auction_id"),
            rs.getString("bidder_id"),
            rs.getString("bidder_username"),
            rs.getLong("bid_amount"),
            toLocalDateTime(rs.getTimestamp("bid_time")),
            BidType.valueOf(rs.getString("bid_type"))
        );
    }

    private AuctionDisplayInfoDTO mapRowToDisplayDto(ResultSet rs) throws SQLException {
        String imageUrlRaw = rs.getString("image_url");
        List<String> imageUrls = (imageUrlRaw != null)
                ? List.of(imageUrlRaw.split(", "))
                : new ArrayList<>();

        return new AuctionDisplayInfoDTO(
            rs.getString("id"),
            rs.getString("auction_name"),
            rs.getString("item_name"),
            rs.getString("item_type"),
            rs.getLong("current_price"),
            rs.getObject("end_time", LocalDateTime.class),
            rs.getString("seller_username"),
            imageUrls
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}