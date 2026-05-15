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

    // 1. Log
    private static final Logger logger = Logger.getLogger(AuctionDAO.class.getName());

    // 2. Constructor
    public AuctionDAO() {
        super();
    }

    // 3. Public Methods (Write / Persist)

    public void saveAuction(Auction auction) throws DAOExceptions, ServiceExceptions {
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

            // Persist Entity
            psEntity = connection.prepareStatement(sqlEntity);
            psEntity.setString(1, auction.getAuctionConfig().getId());
            psEntity.setString(2, auction.getAuctionConfig().getName());
            psEntity.executeUpdate();

            // Persist AuctionConfig
            psAuctionConfig = connection.prepareStatement(sqlAuctionConfig);
            psAuctionConfig.setString(1, auction.getAuctionConfig().getId());
            psAuctionConfig.setLong(2,   auction.getAuctionConfig().getStartPrice());
            psAuctionConfig.setLong(3,   auction.getAuctionConfig().getMinIncrement());
            psAuctionConfig.setObject(4, auction.getAuctionConfig().getStartTime());
            psAuctionConfig.setObject(5, auction.getAuctionConfig().getEndTime());
            psAuctionConfig.setInt(6,    auction.getAuctionConfig().getExtendSecond());
            psAuctionConfig.executeUpdate();

            // Persist Auction
            psAuction = connection.prepareStatement(sqlAuction);
            psAuction.setString(1, auction.getAuctionConfig().getId());
            psAuction.setString(2, auction.getStatus().name());
            psAuction.setString(3, auction.getSellerId());
            psAuction.setString(4, auction.getItemId());
            psAuction.executeUpdate();

            // Persist BidTransactions (if any)
            BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
            for (BidTransaction bidTransaction : auction.getBidTransaction()) {
                bidTransactionDAO.saveBidTransaction(connection, bidTransaction);
            }

            connection.commit();
            logger.log(Level.INFO, "Successfully persisted auction: {0}", auction.getAuctionConfig().getName());

        } catch (SQLIntegrityConstraintViolationException e) {
            safelyRollback(connection);
            logger.log(Level.WARNING, "Constraint violation during auction persistence: {0}", e.getMessage());
            throw new ServiceExceptions(ErrorCode.DATA_CONFLICT, "Data conflict: Auction ID already exists or foreign key violated.", e);

        } catch (SQLException e) {
            safelyRollback(connection);
            logger.log(Level.SEVERE, "Database infrastructure failure during auction persistence.", e);
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database infrastructure error during auction persistence.", e);

        } finally {
            resetAutocommit(connection);
            closeResource(psEntity, psAuctionConfig, psAuction);
            closeConnect(connection);
        }
    }

    public void updateEndTime(String auctionId, LocalDateTime newEndTime) throws DAOExceptions {
        String sql = "UPDATE auction_config SET end_time = ? WHERE id = ?";
        Connection        connection = null;
        PreparedStatement ps         = null;
        
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setObject(1, newEndTime);
            ps.setString(2, auctionId);
            ps.executeUpdate();
            
            logger.log(Level.INFO, "Successfully updated end time for auction ID: {0}", auctionId);
        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database infrastructure error updating end time for auction ID: " + auctionId, e);
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    public void updateStatus(String auctionId, AuctionStatus newStatus) throws DAOExceptions {
        String sql = "UPDATE auction SET status = ? WHERE id = ?";
        Connection        connection = null;
        PreparedStatement ps         = null;
        
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, newStatus.name());
            ps.setString(2, auctionId);
            ps.executeUpdate();
            
            logger.log(Level.INFO, "Successfully updated status for auction ID: {0}", auctionId);
        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database infrastructure error updating status for auction ID: " + auctionId, e);
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    public void deleteById(String auctionId) throws DAOExceptions {
        String sql = "DELETE FROM auction WHERE id = ?";
        Connection        connection = null;
        PreparedStatement ps         = null;
        
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, auctionId);
            ps.executeUpdate();
            
            logger.log(Level.INFO, "Successfully deleted auction ID: {0}", auctionId);
        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database infrastructure error deleting auction ID: " + auctionId, e);
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }

    // 4. Public Methods (Read / Fetch)

    public List<Auction> findBySellerId(String sellerId) throws DAOExceptions {
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
            ps = connection.prepareStatement(sql);
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

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database infrastructure error retrieving auctions for seller ID: " + sellerId, e);
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public Auction findByAuctionId(String auctionId) throws DAOExceptions {
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
        PreparedStatement ps         = null;
        ResultSet         rs         = null;
        Auction           auction    = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
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

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database infrastructure error retrieving auction by ID: " + auctionId, e);
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public List<Auction> findByStatus(AuctionStatus status) throws DAOExceptions {
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
            ps = connection.prepareStatement(sql);
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

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database infrastructure error retrieving auctions by status: " + status.name(), e);
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public List<AuctionDisplayInfoDTO> findSellerAuctions(String sellerId) throws DAOExceptions {
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
        PreparedStatement ps         = null;
        ResultSet         rs         = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, sellerId);
            rs = ps.executeQuery();

            List<AuctionDisplayInfoDTO> resultList = new ArrayList<>();
            while (rs.next()) {
                resultList.add(mapRowToDisplayDto(rs));
            }
            return resultList;

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database infrastructure error retrieving display DTOs for seller ID: " + sellerId, e);
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public List<AuctionDisplayInfoDTO> findActiveAuctions() throws DAOExceptions {
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
        PreparedStatement ps         = null;
        ResultSet         rs         = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();

            List<AuctionDisplayInfoDTO> resultList = new ArrayList<>();
            while (rs.next()) {
                resultList.add(mapRowToDisplayDto(rs));
            }
            return resultList;

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database infrastructure error retrieving active auctions.", e);
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    // 5. Private Methods (Helper)

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
     * Maps a row from a JOIN query to a BidTransaction object.
     * * Reason for using alias "b_auction_id" instead of "auction_id":
     * - The JOIN query already has 'a.id AS auction_id' (used for auction mapping).
     * - If we read rs.getString("auction_id"), JDBC returns the first matching column, 
     * which would be 'a.id' instead of 'b.auction_id', causing a silent bug.
     * - Setting a distinct alias 'b.auction_id AS b_auction_id' completely eliminates this ambiguity.
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