package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.DAOExceptions;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceExceptions;
import com.ssscloud.auction.common.model.Admin;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.Seller;
import com.ssscloud.auction.common.model.base.User;

public class UserDAO extends BaseDAO {

    // 1. Log
    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());

    // 2. Public Methods (Write / Persist)

    public boolean saveBidder(Bidder bidder) throws ServiceExceptions, DAOExceptions{
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlUser   = "INSERT INTO user (id, username, password, email, role) VALUES (?, ?, ?, ?, ?)";
        String sqlBidder = "INSERT INTO bidder (id, account_balance) VALUES (?, ?)";

        Connection        conn     = null;
        PreparedStatement psEntity = null, psUser = null, psBidder = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString(1, bidder.getId());
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
            return true;

        } catch (SQLIntegrityConstraintViolationException e) {
            safelyRollback(conn);
            throw new ServiceExceptions(ErrorCode.USERNAME_EXISTED, "Bidder already exists: " + bidder.getUserName(), e);

        } catch (SQLException e) {
            safelyRollback(conn);
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while saving bidder: " + bidder.getUserName(), e);

        } finally {
            resetAutocommit(conn);
            closeResource(psEntity, psUser, psBidder);
            closeConnect(conn);
        }
    }

    public boolean saveSeller(Seller seller)throws ServiceExceptions, DAOExceptions {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlUser   = "INSERT INTO user (id, username, password, email, role) VALUES (?, ?, ?, ?, ?)";
        String sqlSeller = "INSERT INTO seller (id, bank_account, account_balance) VALUES (?, ?, ?)";

        Connection        conn     = null;
        PreparedStatement psEntity = null, psUser = null, psSeller = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString(1, seller.getId());
            psEntity.setString(2, seller.getName());
            psEntity.executeUpdate();

            psUser = conn.prepareStatement(sqlUser);
            psUser.setString(1, seller.getId());
            psUser.setString(2, seller.getUserName());
            psUser.setString(3, seller.getPassword());
            psUser.setString(4, seller.getEmail());
            psUser.setString(5, seller.getRole().name());
            psUser.executeUpdate();

            psSeller = conn.prepareStatement(sqlSeller);
            psSeller.setString(1, seller.getId());
            psSeller.setString(2, seller.getBankAccount());
            psSeller.setLong(3, seller.getAccountBalance());
            psSeller.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLIntegrityConstraintViolationException e) {
            safelyRollback(conn);
            throw new ServiceExceptions(ErrorCode.USERNAME_EXISTED, "Seller already exists: " + seller.getUserName(), e);

        } catch (SQLException e) {
            safelyRollback(conn);
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while saving seller: " + seller.getUserName(), e);

        } finally {
            resetAutocommit(conn);
            closeResource(psEntity, psUser, psSeller);
            closeConnect(conn);
        }
    }

    // 3. Public Methods (Read / Fetch)

    public User findByUsername(String userName) throws DAOExceptions{
        String sql =
            "SELECT e.id, e.name, u.username, u.password, u.email, u.role, " +
            "       b.account_balance, s.bank_account, s.account_balance AS seller_balance " +
            "FROM entity e " +
            "JOIN user u ON e.id = u.id " +
            "LEFT JOIN bidder b ON u.id = b.id " +
            "LEFT JOIN seller s ON u.id = s.id " +
            "WHERE u.username = ?";

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userName);
            rs = ps.executeQuery();

            if (rs.next())
                return mapResultSetToUser(rs);

            logger.log(Level.INFO, "User not found for username: {0}", userName);
            return null;

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while fetching user by username: " + userName, e);

        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }

    public User findByEmail(String email) throws DAOExceptions{
        String sql =
            "SELECT e.id, e.name, u.username, u.password, u.email, u.role, " +
            "       b.account_balance, s.bank_account, s.account_balance AS seller_balance " +
            "FROM entity e " +
            "JOIN user u ON e.id = u.id " +
            "LEFT JOIN bidder b ON u.id = b.id " +
            "LEFT JOIN seller s ON u.id = s.id " +
            "WHERE u.email = ?";

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            rs = ps.executeQuery();

            if (rs.next())
                return mapResultSetToUser(rs);

            return null;

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while fetching user by email: " + email, e);

        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }

    public User findById(String userId) throws DAOExceptions{
        String sql =
            "SELECT e.id, e.name, u.username, u.password, u.email, u.role, " +
            "       b.account_balance, s.bank_account, s.account_balance AS seller_balance " +
            "FROM entity e " +
            "JOIN user u ON e.id = u.id " +
            "LEFT JOIN bidder b ON u.id = b.id " +
            "LEFT JOIN seller s ON u.id = s.id " +
            "WHERE e.id = ?";

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            rs = ps.executeQuery();

            if (rs.next())
                return mapResultSetToUser(rs);

            return null;

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while fetching user by userId: " + userId, e);

        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }

    public boolean existByUsername(String userName) throws DAOExceptions{
        String sql = "SELECT 1 FROM user WHERE username = ?";

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userName);
            rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while checking existence of username: " + userName, e);

        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
    }

    // 4. Public Methods (Update)

    public boolean updatePassword(String userId, String newPassword) throws DAOExceptions{
        String sql = "UPDATE user SET password = ? WHERE id = ?";

        Connection        conn = null;
        PreparedStatement ps   = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newPassword);
            ps.setString(2, userId);
            int rowsAffected = ps.executeUpdate();
            logger.log(Level.INFO, "Password updated for userId: {0}", userId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while updating password for userId: " + userId, e);

        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    public boolean updateEmail(String userId, String newEmail) throws DAOExceptions{
        String sql = "UPDATE user SET email = ? WHERE id = ?";

        Connection        conn = null;
        PreparedStatement ps   = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newEmail);
            ps.setString(2, userId);
            int rowsAffected = ps.executeUpdate();
            logger.log(Level.INFO, "Email updated for userId: {0}", userId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while updating email for userId: " + userId, e);

        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    public boolean updateAccountBalance(String userId, Long newAccountBalance) throws DAOExceptions {
        String sql = "UPDATE bidder SET account_balance = ? WHERE id = ?";

        Connection        conn = null;
        PreparedStatement ps   = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, newAccountBalance);
            ps.setString(2, userId);
            int rowsAffected = ps.executeUpdate();
            logger.log(Level.INFO, "Account balance updated for userId: {0}", userId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while updating account balance for userId: " + userId, e);

        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    public boolean updateBankAccount(String userId, String newBankAccount) throws DAOExceptions {
        String sql = "UPDATE seller SET bank_account = ? WHERE id = ?";

        Connection        conn = null;
        PreparedStatement ps   = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newBankAccount);
            ps.setString(2, userId);
            int rowsAffected = ps.executeUpdate();
            logger.log(Level.INFO, "Bank account updated for userId: {0}", userId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while updating bank account for userId: " + userId, e);

        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    public boolean updateSellerBalance(String userId, long newBalance) throws DAOExceptions {
        String sql = "UPDATE seller SET account_balance = ? WHERE id = ?";

        Connection        conn = null;
        PreparedStatement ps   = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setLong(1, newBalance);
            ps.setString(2, userId);
            int rowsAffected = ps.executeUpdate();
            logger.log(Level.INFO, "Account balance updated for sellerId: {0}", userId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            throw new DAOExceptions(ErrorCode.INTERNAL_DB_ERROR, "Database error while updating balance for sellerId: " + userId, e);

        } finally {
            closeResource(ps);
            closeConnect(conn);
        }
    }

    // 5. Private Methods (Helper)

    public User mapResultSetToUser(ResultSet rs) throws SQLException {
        String   userId   = rs.getString("id");
        String   name     = rs.getString("name");
        String   userName = rs.getString("username");
        String   password = rs.getString("password");
        String   email    = rs.getString("email");
        UserRole role     = UserRole.valueOf(rs.getString("role"));

        return switch (role) {
            case BIDDER -> {
                long balance = rs.getLong("account_balance");
                yield new Bidder(userId, name, userName, password, email, role, balance);
            }
            case SELLER -> {
                String bankAccount    = rs.getString("bank_account");
                long   sellerBalance  = rs.getLong("seller_balance");
                yield new Seller(userId, name, userName, password, email, role, bankAccount, sellerBalance);
            }
            case ADMIN -> new Admin(userId, name, userName, password, email, role);
            default -> throw new SQLException("Unrecognized user role: " + role);
        };
    }
}