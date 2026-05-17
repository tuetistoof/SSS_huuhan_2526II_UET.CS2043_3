package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.response.AdminDisplayDTO;
import com.ssscloud.auction.common.dto.response.AdminMetrics;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.AuctionStatus;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;

/**
 * AdminDAO handles data access operations for admin-specific queries.
 * Provides auction list retrieval with optional status filtering and system metrics.
 */
public class AdminDAO extends BaseDAO {

    // Logging Standards: Declared first as a private static final attribute
    private static final Logger logger = Logger.getLogger(AdminDAO.class.getName());

    // --- PUBLIC METHODS ---

    /**
     * Lấy tất cả auction, có thể filter theo status.
     * Nếu filter == null thì lấy toàn bộ.
     */
    public List<AdminDisplayDTO> findAllAuctions(AuctionStatus filter) throws DAOException, Exception {
        // Query base — JOIN để lấy tên auction, seller, và giá hiện tại
        String baseSql =
            "SELECT a.id, " +
            "       e.name                                        AS auction_name, " +
            "       u.username                                    AS seller_username, " +
            "       a.status, " +
            "       ac.end_time, " +
            "       COALESCE(last_bid.bid_amount, ac.start_price) AS current_price " +
            "FROM auction a " +
            "JOIN auction_config ac ON a.id = ac.id " +
            "JOIN entity e          ON a.id = e.id " +
            "JOIN user u            ON a.seller_id = u.id " +
            // Subquery lấy bid mới nhất để tính current_price
            "LEFT JOIN ( " +
            "    SELECT b1.auction_id, b1.bid_amount " +
            "    FROM bid_transaction b1 " +
            "    WHERE b1.bid_time = ( " +
            "        SELECT MAX(b2.bid_time) " +
            "        FROM bid_transaction b2 " +
            "        WHERE b2.auction_id = b1.auction_id " +
            "    ) " +
            ") AS last_bid ON last_bid.auction_id = a.id ";

        // Thêm WHERE nếu có filter, không thì lấy tất cả
        String sql = (filter != null)
            ? baseSql + "WHERE a.status = ? ORDER BY ac.end_time DESC"
            : baseSql + "ORDER BY ac.end_time DESC";

        Connection             connection        = null;
        PreparedStatement      preparedStatement = null;
        ResultSet              resultSet         = null;
        List<AdminDisplayDTO> auctionList       = new ArrayList<>();

        try {
            logger.log(Level.INFO, "Retrieving all auctions for admin, filter: {0}",
                filter != null ? filter.name() : "NONE");

            connection        = getConnection();
            preparedStatement = connection.prepareStatement(sql);

            // Chỉ set tham số khi có filter
            if (filter != null) {
                preparedStatement.setString(1, filter.name());
            }

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                auctionList.add(mapRowToAdminAuctionView(resultSet));
            }

            logger.log(Level.INFO, "Successfully retrieved {0} auction(s) for admin view.",
                auctionList.size());

            return auctionList;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.AUCTION_FETCH_FAILED,
                "Database interaction failure while retrieving auctions for admin.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in AdminDAO.findAllAuctions", exception);
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }

    /**
     * Lấy 3 con số thống kê cho metric cards:
     * số auction đang RUNNING, số auction đã FINISHED, tổng số user.
     */
    public AdminMetrics getMetrics() throws DAOException, Exception {
        // 1 câu query duy nhất — SUM điều kiện để đếm theo status
        String sql =
            "SELECT " +
            "    SUM(status = 'RUNNING')  AS running_count, " +
            "    SUM(status = 'FINISHED') AS ended_count, " +
            "    (SELECT COUNT(*) FROM user) AS total_users " +
            "FROM auction";

        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet         = null;

        try {
            logger.log(Level.INFO, "Retrieving admin dashboard metrics.");

            connection        = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            resultSet         = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return new AdminMetrics(
                    resultSet.getInt("running_count"),
                    resultSet.getInt("ended_count"),
                    resultSet.getInt("total_users")
                );
            }

            // Không có row nào thì trả về toàn 0
            return new AdminMetrics(0, 0, 0);

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.AUCTION_FETCH_FAILED,
                "Database interaction failure while retrieving admin metrics.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in AdminDAO.getMetrics", exception);
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }

    /**
     * Lấy tất cả user (BIDDER + SELLER), có thể filter theo role.
     * filter == null thì lấy tất cả.
     */
    public List<UserDTO> getAllUsers(String roleFilter) throws DAOException, Exception {
        String baseSql =
            "SELECT u.id, u.username, u.email, u.role, " +
            "       COALESCE(b.account_balance, s.account_balance, 0) AS account_balance " +
            "FROM user u " +
            "LEFT JOIN bidder b  ON u.id = b.id " +
            "LEFT JOIN seller s  ON u.id = s.id ";

        String sql = (roleFilter != null && !roleFilter.isBlank())
            ? baseSql + "WHERE u.role = ? ORDER BY u.username ASC"
            : baseSql + "ORDER BY u.username ASC";

        Connection        connection        = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet         = null;
        List<UserDTO>     userList          = new ArrayList<>();

        try {
            logger.log(Level.INFO, "Admin retrieving user list, roleFilter: {0}",
                roleFilter != null ? roleFilter : "ALL");

            connection        = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            if (roleFilter != null && !roleFilter.isBlank()) {
                preparedStatement.setString(1, roleFilter.toUpperCase());
            }

            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                userList.add(mapRowToUserDTO(resultSet));
            }

            logger.log(Level.INFO, "Admin user list returned {0} user(s).", userList.size());
            return userList;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_RETRIEVAL_FAILED,
                "Database interaction failure while retrieving user list for admin.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in AdminDAO.getAllUsers", exception);
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }

    // --- PRIVATE METHODS ---

    /** Map một row ResultSet sang AdminAuctionView */
    private AdminDisplayDTO mapRowToAdminAuctionView(ResultSet rs) throws SQLException {
        // Đọc end_time an toàn — có thể null nếu auction chưa set
        LocalDateTime endTime = null;
        java.sql.Timestamp endTimeTs = rs.getTimestamp("end_time");
        if (endTimeTs != null) {
            endTime = endTimeTs.toLocalDateTime();
        }

        return new AdminDisplayDTO(
            rs.getString("id"),
            rs.getString("auction_name"),
            rs.getString("seller_username"),
            rs.getLong("current_price"),
            AuctionStatus.valueOf(rs.getString("status")),
            endTime
        );
    }

    /** Map một row ResultSet sang UserDTO cho admin user list */
    private UserDTO mapRowToUserDTO(ResultSet rs) throws SQLException {
        String roleStr = rs.getString("role");
        UserRole role = (roleStr != null) ? UserRole.valueOf(roleStr) : null;
        return new UserDTO(
            rs.getString("id"),
            rs.getString("username"),
            rs.getString("email"),
            role,
            rs.getLong("account_balance"),
            0L  // unsettledBalance không cần cho admin view
        );
    }
}