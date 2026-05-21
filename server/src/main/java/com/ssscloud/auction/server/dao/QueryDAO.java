package com.ssscloud.auction.server.dao;

import java.security.Timestamp;
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

import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.payload.response.DTO.BidderDisplayDTO;
import com.ssscloud.auction.common.payload.response.DTO.SellerDisplayDTO;

public class QueryDAO extends BaseDAO{
    private static final Logger logger = Logger.getLogger(QueryDAO.class.getName());

    // --- PUBLIC METHODS ---
    // Thêm inner record (hoặc tạo class riêng, nhưng để gọn thì dùng record)

    public static class AuctionScheduleInfo {
        private final String auctionId;
        private final LocalDateTime endTime;

        public AuctionScheduleInfo(String auctionId, LocalDateTime endTime) {
            this.auctionId = auctionId;
            this.endTime   = endTime;
        }

        public String getAuctionId()      { return auctionId; }
        public LocalDateTime getEndTime() { return endTime;   }
    }

    // --- SCHEDULE RECOVERY ---

    /**
     * Chỉ lấy auctionId + endTime cho auction OPEN/RUNNING.
     * Nhẹ hơn findByStatus() — không load full Auction object lên RAM.
     */
    public List<AuctionScheduleInfo> findActiveScheduleInfos() throws DAOException, Exception {
        String sql =
            "SELECT a.id, ac.end_time " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "WHERE a.status IN ('OPEN', 'RUNNING')";

        Connection        connection = null;
        PreparedStatement ps         = null;
        ResultSet         rs         = null;
        List<AuctionScheduleInfo> result = new ArrayList<>();

        try {
            connection = getConnection();
            ps         = connection.prepareStatement(sql);
            rs         = ps.executeQuery();
            while (rs.next()) {
                result.add(new AuctionScheduleInfo(
                    rs.getString("id"),
                    rs.getObject("end_time", LocalDateTime.class)
                ));
            }
            logger.log(Level.INFO, "findActiveScheduleInfos: loaded {0} active auction(s).", result.size());
            return result;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.AUCTION_FETCH_FAILED,
                "Database failure while retrieving active schedule infos.", sqlException);
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
}
    public List<SellerDisplayDTO> findSellerAuction(String sellerId) throws DAOException, Exception {
        String sql =
            "SELECT a.id, " +
            "       e.name AS auction_name, ac.end_time, a.status, " +
            "       ei.name AS item_name, " +
            "       ac.start_price, " +
            "       COALESCE(last_bid.bid_amount, ac.start_price) AS current_price, " +
            "       COALESCE(bid_count.count, 0) AS bid_count " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e ON a.id = e.id " +
            "JOIN item i ON a.item_id = i.id " +
            "JOIN entity ei ON i.id = ei.id " +
            "LEFT JOIN ( " +
            "    SELECT b1.auction_id, b1.bid_amount FROM bid_transaction b1 " +
            "    WHERE b1.bid_time = (SELECT MAX(b2.bid_time) FROM bid_transaction b2 " +
            "                         WHERE b2.auction_id = b1.auction_id) " +
            ") AS last_bid ON last_bid.auction_id = a.id " +
            "LEFT JOIN ( " +
            "    SELECT auction_id, COUNT(*) AS count FROM bid_transaction GROUP BY auction_id " +
            ") AS bid_count ON bid_count.auction_id = a.id " +
            "WHERE a.seller_id = ? " +
            "GROUP BY a.id, e.name, ac.end_time, a.status, ei.name, ac.start_price, last_bid.bid_amount, bid_count.count";

        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet         = null;
        List<SellerDisplayDTO> sellerAuctionsList = new ArrayList<>();

        try {
            logger.log(Level.INFO, "Initiating query to retrieve seller auctions for sellerId: {0}", sellerId);
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, sellerId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                sellerAuctionsList.add(mapRowToSellerDisplayDto(resultSet));
            }
            logger.log(Level.INFO, "Successfully retrieved {0} auction(s) for sellerId: {1}", new Object[]{sellerAuctionsList.size(), sellerId});
            return sellerAuctionsList;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.SELLER_AUCTION_FETCH_FAILED, "Database interaction failure while retrieving seller-specific auctions.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }


    // ── Query ─────────────────────────────────────────────────────────────────
    public List<BidderDisplayDTO> findBiddedAuctionsDetailsByUser(String userId) throws DAOException, Exception {
        logger.log(Level.INFO, "Retrieving bidded auction details for userId: {0}", userId);
        List<BidderDisplayDTO> biddedAuctionsList = new ArrayList<>();

        String sql =
                    "SELECT a.id, " +
            "       e.name                                              AS auction_name, " +
            "       ac.end_time, " +
            "       u.username                                          AS seller_username, " +
            "       ei.name                                             AS item_name, " +
            "       i.type                                              AS item_type, " +
            "       COALESCE(last_bid.bid_amount, ac.start_price)       AS current_price, " +
            "       GROUP_CONCAT(DISTINCT img.image_url SEPARATOR ', ') AS image_url, " +
            "       my_bid.bid_amount                                   AS my_last_bid, " +
            "       (my_bid.bid_amount = COALESCE(last_bid.bid_amount, ac.start_price)) AS is_leading " +
            "FROM auction a " +
            "JOIN bid_transaction bt_user ON bt_user.auction_id = a.id AND bt_user.bidder_id = ? " +
            "JOIN auction_config ac       ON a.id = ac.id " +
            "JOIN entity e                ON a.id = e.id " +
            "JOIN user u                  ON a.seller_id = u.id " +
            "JOIN item i                  ON a.item_id = i.id " +
            "JOIN entity ei               ON i.id = ei.id " +
            "LEFT JOIN item_image_url img ON a.item_id = img.item_id " +
            
            // Lấy bid cao nhất hiện tại
            "LEFT JOIN ( " +
            "    SELECT auction_id, MAX(bid_amount) AS bid_amount " +
            "    FROM bid_transaction " +
            "    WHERE (auction_id, bid_time) IN ( " +
            "        SELECT auction_id, MAX(bid_time) " +
            "        FROM bid_transaction " +
            "        GROUP BY auction_id " +
            "    ) " +
            "    GROUP BY auction_id " +
            ") AS last_bid ON last_bid.auction_id = a.id " +
            
            // Lấy bid mới nhất của user đang xem
            "LEFT JOIN ( " +
            "    SELECT auction_id, MAX(bid_amount) AS bid_amount " +
            "    FROM bid_transaction " +
            "    WHERE bidder_id = ? " +
            "      AND (auction_id, bid_time) IN ( " +
            "          SELECT auction_id, MAX(bid_time) " +
            "          FROM bid_transaction " +
            "          WHERE bidder_id = ? " +
            "          GROUP BY auction_id " +
            "      ) " +
            "    GROUP BY auction_id " +
            ") AS my_bid ON my_bid.auction_id = a.id " +
            
            "WHERE a.status IN ('OPEN', 'RUNNING') " +
            
            // Fix Bug 1: thêm các cột cần thiết vào GROUP BY
            "GROUP BY a.id, e.name, ac.end_time, u.username, ei.name, i.type, " +
            "         ac.start_price, last_bid.bid_amount, my_bid.bid_amount";

        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet         = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, userId);  // bt_user.bidder_id
            preparedStatement.setString(2, userId);  // b3.bidder_id
            preparedStatement.setString(3, userId);  // b4.bidder_id
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                BidderDisplayDTO dto = new BidderDisplayDTO();
                dto.setId(resultSet.getString("id"));
                dto.setAuctionName(resultSet.getString("auction_name"));
                dto.setItemName(resultSet.getString("item_name"));
                dto.setItemType(resultSet.getString("item_type"));
                dto.setCurrentPrice(resultSet.getLong("current_price"));
                dto.setSellerUsername(resultSet.getString("seller_username"));
                dto.setMyLastBid(resultSet.getLong("my_last_bid"));
                dto.setLeading(resultSet.getBoolean("is_leading"));

                dto.setEndTime(resultSet.getObject("end_time", LocalDateTime.class));

                String imageUrlsRaw = resultSet.getString("image_url");
                if (imageUrlsRaw != null && !imageUrlsRaw.isEmpty()) {
                    dto.setImageUrl(List.of(imageUrlsRaw.split(", ")));
                }

                biddedAuctionsList.add(dto);
            }
            logger.log(Level.INFO, "Successfully loaded {0} bidded auction items for userId: {1}", new Object[]{biddedAuctionsList.size(), userId});
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.BIDDED_AUCTIONS_FETCH_FAILED, "Database failure while fetching user bidded auction history.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
        return biddedAuctionsList;
    }

    public List<BidderDisplayDTO> findActiveAuctions() throws DAOException, Exception {
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

        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet         = null;
        List<BidderDisplayDTO> activeAuctionsList = new ArrayList<>();

        try {
            logger.log(Level.INFO, "Retrieving all active auctions for bidder display.");
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                activeAuctionsList.add(mapRowToBidderDisplayDto(resultSet));
            }
            logger.log(Level.INFO, "Successfully loaded {0} active auctions for public display.", activeAuctionsList.size());
            return activeAuctionsList;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.ACTIVE_AUCTION_FETCH_FAILED, "Database failure while retrieving all active auctions.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }

    // ── Watch ─────────────────────────────────────────────────────────────────

    public boolean add(String auctionId, String userId) throws DAOException, Exception {
        String sql = "INSERT INTO watchlist (auction_id, user_id) VALUES (?, ?)";
        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, auctionId);
            preparedStatement.setString(2, userId);
            preparedStatement.executeUpdate();
            logger.log(Level.INFO, "Watchlist entry successfully created for userId: {0}, auctionId: {1}", new Object[]{userId, auctionId});
            return true;
        } catch (SQLIntegrityConstraintViolationException constraintException) {
            throw new DAOException(ErrorCode.DATA_INTEGRITY_VIOLATION, "Constraint violation: User is already following this auction.", constraintException);
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.WATCHLIST_ADD_FAILED, "Database interaction failure while adding auction to watchlist.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    // ── Unwatch ───────────────────────────────────────────────────────────────

    public boolean remove(String auctionId, String userId) throws DAOException, Exception {
        String sql = "DELETE FROM watchlist WHERE auction_id = ? AND user_id = ?";
        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, auctionId);
            preparedStatement.setString(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            logger.log(Level.INFO, "Watchlist entry successfully removed for userId: {0}, auctionId: {1}", new Object[]{userId, auctionId});
            return rowsAffected > 0;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.WATCHLIST_REMOVE_FAILED, "Database interaction failure while removing auction from watchlist.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public List<BidderDisplayDTO> findWatchlistDetailsByUser(String userId) throws DAOException, Exception {
        List<BidderDisplayDTO> auctionDetailsList = new ArrayList<>();
        
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
            "WHERE w.user_id = ?" +
            "  AND a.status = 'RUNNING'";

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, userId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                BidderDisplayDTO dto = new BidderDisplayDTO();
                dto.setId(resultSet.getString("id"));
                dto.setAuctionName(resultSet.getString("auction_name"));
                dto.setItemName(resultSet.getString("item_name"));
                dto.setItemType(resultSet.getString("item_type"));
                dto.setCurrentPrice(resultSet.getLong("current_price"));
                
                dto.setEndTime(resultSet.getObject("end_time", LocalDateTime.class));
                
                dto.setSellerUsername(resultSet.getString("seller_username"));
                
                String imageUrlRaw = resultSet.getString("image_url");
                List<String> imageUrlList = (imageUrlRaw != null) ? List.of(imageUrlRaw) : new ArrayList<>();
                dto.setImageUrl(imageUrlList);
                
                auctionDetailsList.add(dto);
            }
            logger.log(Level.INFO, "Successfully retrieved {0} detailed watchlist items for userId: {1}", new Object[]{auctionDetailsList.size(), userId});
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.WATCHLIST_DETAILS_RETRIEVAL_FAILED, "Database failure while retrieving user watchlist details.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
        return auctionDetailsList;
    }

    public List<String> findUserIdsByAuction(String auctionId) throws DAOException, Exception {
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
            throw new DAOException(ErrorCode.WATCHLIST_WATCHER_FETCH_FAILED, "Database failure while retrieving watcher IDs for auction.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }

    public boolean isFollowing(String auctionId, String userId) throws DAOException, Exception {
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
            throw new DAOException(ErrorCode.WATCHLIST_STATUS_CHECK_FAILED, "Database failure while checking watchlist following status.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }

    // ── Query ───────────────────────────────────────────────────────

    public List<BidderDisplayDTO> findWonItemsByUser(String userId) throws DAOException, Exception {
        List<BidderDisplayDTO> auctionDetailsList = new ArrayList<>();
        String sql =
            "SELECT a.id, " +
             "       e.name AS auction_name, " +          
             "       ac.end_time, " +                      
             "       u_seller.username AS seller_username, " + 
             "       ei.name AS item_name, " +             
             "       i.type AS item_type, " +              
             "       last_bid.bid_amount AS current_price, " + 
             "       (SELECT img.image_url FROM item_image_url img " +
             "        WHERE img.item_id = a.item_id LIMIT 1) AS image_url " + 
             "FROM auction a " +
             "JOIN auction_config ac ON a.id = ac.id " +
             "JOIN entity e ON a.id = e.id " +            
             "JOIN user u_seller ON a.seller_id = u_seller.id " + 
             "JOIN item i ON a.item_id = i.id " +
             "JOIN entity ei ON i.id = ei.id " +          
             "JOIN ( " +
             "    SELECT b1.auction_id, b1.bidder_id, b1.bid_amount FROM bid_transaction b1 " +
             "    WHERE b1.bid_amount = (SELECT MAX(b2.bid_amount) FROM bid_transaction b2 " +
             "                           WHERE b2.auction_id = b1.auction_id) " +
             ") AS last_bid ON last_bid.auction_id = a.id " +
             "WHERE a.status = 'FINISHED' " +
             "  AND last_bid.bidder_id = ? " +
             "ORDER BY ac.end_time DESC";

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, userId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                BidderDisplayDTO dto = new BidderDisplayDTO();
                
                dto.setId(resultSet.getString("id"));
                dto.setAuctionName(resultSet.getString("auction_name"));
                dto.setItemName(resultSet.getString("item_name"));
                dto.setItemType(resultSet.getString("item_type"));
                dto.setCurrentPrice(resultSet.getLong("current_price"));

                dto.setEndTime(resultSet.getObject("end_time", LocalDateTime.class));
                
                dto.setSellerUsername(resultSet.getString("seller_username"));
                
                String imageUrlRaw = resultSet.getString("image_url");
                List<String> imageUrlList = (imageUrlRaw != null) ? List.of(imageUrlRaw) : new ArrayList<>();
                dto.setImageUrl(imageUrlList);

                auctionDetailsList.add(dto);
            }
            logger.log(Level.INFO, "Successfully retrieved {0} detailed won items for userId: {1}", new Object[]{auctionDetailsList.size(), userId});
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.WON_ITEMS_DETAILS_RETRIEVAL_FAILED, "Database failure while retrieving user won items details.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
        return auctionDetailsList;
    }

    // --- PRIVATE METHODS ---

    private BidderDisplayDTO mapRowToBidderDisplayDto(ResultSet resultSet) throws SQLException {
        String imageUrlRaw = resultSet.getString("image_url");
        List<String> imageUrlList = (imageUrlRaw != null)
                ? List.of(imageUrlRaw.split(", "))
                : new ArrayList<>();

        return new BidderDisplayDTO(
            resultSet.getString("id"),
            resultSet.getString("auction_name"),
            resultSet.getString("item_name"),
            resultSet.getString("item_type"),
            resultSet.getLong("current_price"),
            resultSet.getObject("end_time", LocalDateTime.class),
            resultSet.getString("seller_username"),
            imageUrlList
        );
    }

    private SellerDisplayDTO mapRowToSellerDisplayDto(ResultSet resultSet) throws SQLException {
        String statusString = resultSet.getString("status");
        AuctionStatus auctionStatus = AuctionStatus.valueOf(statusString);
        
        return new SellerDisplayDTO(
            resultSet.getString("id"),
            resultSet.getString("auction_name"),
            resultSet.getString("item_name"),
            resultSet.getLong("start_price"),
            resultSet.getLong("current_price"),
            resultSet.getInt("bid_count"),
            resultSet.getObject("end_time", LocalDateTime.class),
            auctionStatus
        );
    }
}
