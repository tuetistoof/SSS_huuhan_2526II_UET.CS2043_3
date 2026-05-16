package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.exception.ServiceException;
import com.ssscloud.auction.common.model.Admin;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.Seller;
import com.ssscloud.auction.common.model.base.User;

public class UserDAO extends BaseDAO {
    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());

    // --- PUBLIC METHODS ---

    public boolean saveBidder(Bidder bidder) throws SQLException, Exception {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlUser   = "INSERT INTO user (id, username, password, email, role) VALUES (?, ?, ?, ?, ?)";
        String sqlBidder = "INSERT INTO bidder (id, account_balance) VALUES (?, ?)";

        Connection connection = null;
        PreparedStatement psEntity = null;
        PreparedStatement psUser = null;
        PreparedStatement psBidder = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            psEntity = connection.prepareStatement(sqlEntity);
            psEntity.setString(1, bidder.getId());
            psEntity.setString(2, bidder.getName());
            psEntity.executeUpdate();

            psUser = connection.prepareStatement(sqlUser);
            psUser.setString(1, bidder.getId());
            psUser.setString(2, bidder.getUserName());
            psUser.setString(3, bidder.getPassword());
            psUser.setString(4, bidder.getEmail());
            psUser.setString(5, bidder.getRole().name());
            psUser.executeUpdate();

            psBidder = connection.prepareStatement(sqlBidder);
            psBidder.setString(1, bidder.getId());
            psBidder.setLong(2, bidder.getAccountBalance());
            psBidder.executeUpdate();

            connection.commit();
            logger.log(Level.INFO, "Bidder successfully persisted: " + bidder.getUserName());
            return true;
        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            logger.log(Level.WARNING, "Constraint violation in saveBidder (username already exists): " + bidder.getUserName() + " - " + sqlConstraintException.getMessage());
            safelyRollback(connection);
            return false;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database failure in saveBidder for username: " + bidder.getUserName(), sqlException);
            safelyRollback(connection);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.saveBidder: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeConnect(connection);
            closeResource(psEntity, psUser, psBidder);
            closeConnect(connection);
        }
    }

    public boolean saveSeller(Seller seller) throws SQLException, Exception {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlUser   = "INSERT INTO user (id, username, password, email, role) VALUES (?, ?, ?, ?, ?)";
        String sqlSeller = "INSERT INTO seller (id, bank_account, account_balance) VALUES (?, ?, ?)";

        Connection connection = null;
        PreparedStatement psEntity = null;
        PreparedStatement psUser = null;
        PreparedStatement psSeller = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            psEntity = connection.prepareStatement(sqlEntity);
            psEntity.setString(1, seller.getId());
            psEntity.setString(2, seller.getName());
            psEntity.executeUpdate();

            psUser = connection.prepareStatement(sqlUser);
            psUser.setString(1, seller.getId());
            psUser.setString(2, seller.getUserName());
            psUser.setString(3, seller.getPassword());
            psUser.setString(4, seller.getEmail());
            psUser.setString(5, seller.getRole().name());
            psUser.executeUpdate();

            psSeller = connection.prepareStatement(sqlSeller);
            psSeller.setString(1, seller.getId());
            psSeller.setString(2, seller.getBankAccount());
            psSeller.setLong(3, seller.getAccountBalance());
            psSeller.executeUpdate();

            connection.commit();
            logger.log(Level.INFO, "Seller successfully persisted: " + seller.getUserName());
            return true;
        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            logger.log(Level.WARNING, "Constraint violation in saveSeller (username already exists): " + seller.getUserName() + " - " + sqlConstraintException.getMessage());
            safelyRollback(connection);
            return false;
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database failure in saveSeller for username: " + seller.getUserName(), sqlException);
            safelyRollback(connection);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.saveSeller: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeConnect(connection);
            closeResource(psEntity, psUser, psSeller);
            closeConnect(connection);
        }
    }

    public User findByUsername(String username) throws SQLException, Exception {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "u.username, u.password, u.email, u.role, " +
                "b.account_balance, s.bank_account, s.account_balance AS seller_balance " +
                "FROM entity e " +
                "JOIN user u ON e.id = u.id " +
                "LEFT JOIN bidder b ON u.id = b.id " +
                "LEFT JOIN seller s ON u.id = s.id " +
                "WHERE u.username = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();

            if (rs.next())
                return mapResultSetToUser(rs);
            else {
                logger.log(Level.INFO, "User not found for username: " + username);
                return null;
            }
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error in findByUsername for username: " + username, sqlException);
            return null;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.findByUsername: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }
    
    public User findByEmail(String userEmail) throws SQLException, Exception {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "u.username, u.password, u.email, u.role, " +
                "b.account_balance, s.bank_account, s.account_balance AS seller_balance " +
                "FROM entity e " +
                "JOIN user u ON e.id = u.id " +
                "LEFT JOIN bidder b ON u.id = b.id " +
                "LEFT JOIN seller s ON u.id = s.id " +
                "WHERE u.email = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, userEmail);
            rs = ps.executeQuery();

            if (rs.next())
                return mapResultSetToUser(rs);
            else {
                logger.log(Level.INFO, "User not found for email: " + userEmail);
                return null;
            }
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error in findByEmail for email: " + userEmail, sqlException);
            return null;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.findByEmail: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }
    // dung cho login register và mot so tac vu
    public User findById(String userId) throws SQLException, Exception {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "u.username, u.password, u.email, u.role, " +
                "b.account_balance, s.bank_account, s.account_balance AS seller_balance " +
                "FROM entity e " +
                "JOIN user u ON e.id = u.id " +
                "LEFT JOIN bidder b ON u.id = b.id " +
                "LEFT JOIN seller s ON u.id = s.id " +
                "WHERE e.id = ?";

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, userId);
            rs = ps.executeQuery();

            if (rs.next())
                return mapResultSetToUser(rs);

            return null;

        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error in findById for userId: " + userId, sqlException);
            return null;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.findById: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }
    // kiem tra ten dang nhap da ton tai chua
    public boolean existByUsername(String username) throws SQLException, Exception {
        String sql = "SELECT 1 FROM user WHERE username = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error in existByUsername for username: " + username, sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.existByUsername: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    // cac ham update sua thong tin user
    public boolean updatePassword(String userId, String newPassword) throws SQLException, Exception {
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, newPassword);
            ps.setString(2, userId);
            int row = ps.executeUpdate();
            logger.log(Level.INFO, "Password updated for userId: " + userId + ". Rows affected: " + row);
            return row > 0;

        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error updating password for userId: " + userId, sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.updatePassword: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(ps);
            closeConnect(connection);
        }
    }
    public boolean updateEmail(String userId, String userEmail) throws SQLException, Exception {
        String sql = "UPDATE user SET email = ? WHERE id = ?";
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, userEmail);
            ps.setString(2, userId);
            int row = ps.executeUpdate();
            logger.log(Level.INFO, "Email updated for userId: " + userId + ". Rows affected: " + row);
            return row > 0;

        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error updating email for userId: " + userId, sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.updateEmail: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(ps);
            closeConnect(connection);
        }
    }
    public boolean updateAccountBalance (String userId, Long newAccountBalance) throws SQLException, Exception {
        String sql = "UPDATE bidder SET account_balance = ? WHERE id = ?";
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setLong(1, newAccountBalance);
            ps.setString(2, userId);
            int row = ps.executeUpdate();
            logger.log(Level.INFO, "Bidder account balance updated for userId: " + userId + ". Rows affected: " + row);
            return row > 0;

        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error updating bidder account balance for userId: " + userId, sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.updateAccountBalance: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(ps);
            closeConnect(connection);
        }
    }
    public boolean updateBankAccount (String userId, String newBankAccountNumber) throws SQLException, Exception {
        String sql = "UPDATE seller SET bank_account = ? WHERE id = ?";
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, newBankAccountNumber);
            ps.setString(2, userId);
            int row = ps.executeUpdate();
            logger.log(Level.INFO, "Seller bank account updated for userId: " + userId + ". Rows affected: " + row);
            return row > 0;

        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error updating seller bank account for userId: " + userId, sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.updateBankAccount: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(ps);
            closeConnect(connection);
        }
    }
    public boolean updateSellerBalance(String userId, long newSellerBalance) throws SQLException, Exception {
        String sql = "UPDATE seller SET account_balance = ? WHERE id = ?";
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setLong(1, newSellerBalance);
            ps.setString(2, userId);
            int row = ps.executeUpdate();
            logger.log(Level.INFO, "Seller account balance updated for userId: " + userId + ". Rows affected: " + row);
            return row > 0;

        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database error updating seller account balance for userId: " + userId, sqlException);
            return false;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected error in UserDAO.updateSellerBalance: " + exception.getMessage(), exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(ps);
        }
    }
    // ham ho tro

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