package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.common.model.user.Admin;
import com.ssscloud.auction.common.model.user.Bidder;
import com.ssscloud.auction.common.model.user.Seller;

public class UserDAO extends BaseDAO {
    private static final Logger logger = Logger.getLogger(UserDAO.class.getName()); 


    public boolean saveBidder(Bidder bidder) throws DAOException, Exception {
        logger.log(Level.INFO, "Initiating persistence for Bidder: {0}", bidder.getUserName());
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
            logger.log(Level.INFO, "Bidder account successfully persisted for username: {0}", bidder.getUserName());
            return true;
        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.DATA_INTEGRITY_VIOLATION, "Constraint violation: User already exists.", sqlConstraintException);
        } catch (SQLException sqlException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.USER_PERSISTENCE_FAILED, "Database interaction failure while saving Bidder entity.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeResource(psEntity, psUser, psBidder);
            closeConnect(connection);
        }
    }

    public boolean saveSeller(Seller seller) throws DAOException, Exception {
        logger.log(Level.INFO, "Initiating persistence for Seller: {0}", seller.getUserName());
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlUser   = "INSERT INTO user (id, username, password, email, role) VALUES (?, ?, ?, ?, ?)";
        String sqlSeller = "INSERT INTO seller (id, bank_account, account_balance, pending_balance) VALUES (?, ?, ?, ?)";

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
            psSeller.setLong(4, seller.getPendingBalance());
            psSeller.executeUpdate();

            connection.commit();
            logger.log(Level.INFO, "Seller account successfully persisted for username: {0}", seller.getUserName());
            return true;
        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.DATA_INTEGRITY_VIOLATION, "Constraint violation: Seller already exists.", sqlConstraintException);
        } catch (SQLException sqlException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.USER_PERSISTENCE_FAILED, "Database interaction failure while saving Seller entity.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeResource(psEntity, psUser, psSeller);
            closeConnect(connection);
        }
    }

    public User findByUsername(String username) throws DAOException, Exception {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "u.username, u.password, u.email, u.role, " +
                "b.account_balance AS bidder_balance, b.locked_balance, " +
                "s.bank_account, s.account_balance AS seller_balance, s.pending_balance " +
                "FROM entity e " +
                "JOIN user u ON e.id = u.id " +
                "LEFT JOIN bidder b ON u.id = b.id " +
                "LEFT JOIN seller s ON u.id = s.id " +
                "WHERE u.username = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToUser(resultSet);
            }
            else {
                logger.log(Level.INFO, "No user record found for username: {0}", username);
                return null;
            }
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_RETRIEVAL_FAILED, "Database failure while fetching user by username.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }
    
    public User findByEmail(String userEmail) throws DAOException, Exception {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "u.username, u.password, u.email, u.role, " +
                "b.account_balance AS bidder_balance, b.locked_balance, " +
                "s.bank_account, s.account_balance AS seller_balance, s.pending_balance " +
                "FROM entity e " +
                "JOIN user u ON e.id = u.id " +
                "LEFT JOIN bidder b ON u.id = b.id " +
                "LEFT JOIN seller s ON u.id = s.id " +
                "WHERE u.email = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, userEmail);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToUser(resultSet);
            }
            else {
                logger.log(Level.INFO, "No user record found for email: {0}", userEmail);
                return null;
            }
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_RETRIEVAL_FAILED, "Database failure while fetching user by email.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }

    public User findById(String userId) throws DAOException, Exception {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "u.username, u.password, u.email, u.role, " +
                "b.account_balance AS bidder_balance, b.locked_balance, " +
                "s.bank_account, s.account_balance AS seller_balance, s.pending_balance " +
                "FROM entity e " +
                "JOIN user u ON e.id = u.id " +
                "LEFT JOIN bidder b ON u.id = b.id " +
                "LEFT JOIN seller s ON u.id = s.id " +
                "WHERE e.id = ?";

        Connection        connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToUser(resultSet);
            }

            return null;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_RETRIEVAL_FAILED, "Database failure while fetching user by ID.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }

    public boolean existByUsername(String username) throws DAOException, Exception {
        String sql = "SELECT 1 FROM user WHERE username = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet         resultSet = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            resultSet = preparedStatement.executeQuery();
            return resultSet.next();

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_RETRIEVAL_FAILED, "Database failure while verifying username existence.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(resultSet, preparedStatement);
            closeConnect(connection);
        }
    }

    public boolean updatePassword(String userId, String newPassword) throws DAOException, Exception {
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Credential update: Password successfully changed for userId: {0}", userId);
            }
            return rowsAffected > 0;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_MODIFICATION_FAILED, "Database failure while updating user password.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    public boolean updateEmail(String userId, String userEmail) throws DAOException, Exception {
        String sql = "UPDATE user SET email = ? WHERE id = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, userEmail);
            preparedStatement.setString(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Attribute update: Email successfully changed for userId: {0}", userId);
            }
            return rowsAffected > 0;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_MODIFICATION_FAILED, "Database failure while updating user email.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    public boolean updateAccountBalance (String userId, long newAccountBalance) throws DAOException, Exception {
        String sql = "UPDATE bidder SET account_balance = ? WHERE id = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, newAccountBalance);
            preparedStatement.setString(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Balance update: Bidder account updated for userId: {0}", userId);
            }
            return rowsAffected > 0;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_MODIFICATION_FAILED, "Database failure while updating bidder balance.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    public boolean updateBankAccount (String userId, String newBankAccountNumber) throws DAOException, Exception {
        String sql = "UPDATE seller SET bank_account = ? WHERE id = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, newBankAccountNumber);
            preparedStatement.setString(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Attribute update: Seller bank account changed for userId: {0}", userId);
            }
            return rowsAffected > 0;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_MODIFICATION_FAILED, "Database failure while updating bank account details.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    public boolean updateSellerBalance(String userId, long newSellerBalance) throws DAOException, Exception {
        String sql = "UPDATE seller SET account_balance = ? WHERE id = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, newSellerBalance);
            preparedStatement.setString(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Balance update: Seller account updated for userId: {0}", userId);
            }
            return rowsAffected > 0;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_MODIFICATION_FAILED, "Database failure while updating seller balance.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }


    public boolean lockBidderBalance(String userId, long amount) throws DAOException, Exception {
        String sql = "UPDATE bidder SET locked_balance = locked_balance + ? " +
                    "WHERE id = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, amount);
            preparedStatement.setString(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Balance lock: {0} locked for userId: {1}", new Object[]{amount, userId});
            }
            return rowsAffected > 0;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_MODIFICATION_FAILED, "Database failure while locking bidder balance.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    public boolean unlockBidderBalance(String userId, long amount) throws DAOException, Exception {
        String sql = "UPDATE bidder SET locked_balance = locked_balance - ? " +
                    "WHERE id = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, amount);
            preparedStatement.setString(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Balance unlock: {0} unlocked for userId: {1}", new Object[]{amount, userId});
            }
            return rowsAffected > 0;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_MODIFICATION_FAILED, "Database failure while unlocking bidder balance.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    public boolean updatePendingBalance(String userId, long amount) throws DAOException, Exception {
        String sql = "UPDATE seller SET pending_balance = pending_balance + ? " +
                    "WHERE id = ?";
        Connection        connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, amount);
            preparedStatement.setString(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                logger.log(Level.INFO, "Balance lock: {0} locked for userId: {1}", new Object[]{amount, userId});
            }
            return rowsAffected > 0;

        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_MODIFICATION_FAILED, "Database failure while locking bidder balance.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(preparedStatement);
            closeConnect(connection);
        }
    }

    public long getUnsettledBalance(String userId, UserRole role) throws DAOException, Exception {
        String sql = switch (role) {
            case BIDDER -> "SELECT locked_balance AS unsettled FROM bidder WHERE id = ?";
            case SELLER -> "SELECT pending_balance AS unsettled FROM seller WHERE id = ?";
            default -> null;
        };

        if (sql == null) {
            logger.log(Level.INFO, "getUnsettledBalance: role {0} has no unsettled balance, returning 0 for userId: {1}",
                    new Object[]{role, userId});
            return -1 * 1L;
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                long unsettled = rs.getLong("unsettled");
                logger.log(Level.INFO, "getUnsettledBalance: userId={0}, role={1}, unsettled={2}",
                        new Object[]{userId, role, unsettled});
                return unsettled;
            } else {
                logger.log(Level.WARNING, "getUnsettledBalance: no record found for userId: {0}, role: {1}",
                        new Object[]{userId, role});
                return 0L;
            }

        } catch (SQLException e) {
            throw new DAOException(ErrorCode.USER_RETRIEVAL_FAILED, "Failed to get unsettled balance for userId: " + userId, e);
        } finally {
            closeResource(rs, ps);
            closeConnect(connection);
        }
    }

    public boolean settleWinnerBalance(String winnerId, long finalPrice, long lockAmount) throws DAOException, Exception {
            String sql = "UPDATE bidder SET " +
                        "account_balance = account_balance - ?, " +
                        "locked_balance  = locked_balance  - ? " +
                        "WHERE id = ?";
            Connection        connection = null;
            PreparedStatement ps         = null;
            try {
                connection = getConnection();
                ps = connection.prepareStatement(sql);
                ps.setLong(1, finalPrice);   // account_balance -= finalPrice (tiền thực trả)
                ps.setLong(2, lockAmount);   // locked_balance  -= lockAmount (tiền đã lock thực tế)
                ps.setString(3, winnerId);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    logger.log(Level.INFO, "Settle winner: deducted {0} from account, released {1} from lock for userId: {2}",
                            new Object[]{finalPrice, lockAmount, winnerId});
                } else {
                    logger.log(Level.WARNING, "settleWinnerBalance: no rows affected for winnerId: {0} — insufficient lock?", winnerId);
                }
                return rows > 0;
            } catch (SQLException sqlException) {
                throw new DAOException(ErrorCode.USER_MODIFICATION_FAILED, "Database failure while settling winner balance.", sqlException);
            } catch (Exception exception) {
                throw exception;
            } finally {
                closeResource(ps);
                closeConnect(connection);
            }
        }
 
    /**
     * Settle the seller's account after auction ends.
     * Moves finalPrice from pending_balance into account_balance atomically.
     */
    public boolean settleSellerBalance(String sellerId, long finalPrice) throws DAOException, Exception {
        String sql = "UPDATE seller SET " +
                     "account_balance  = account_balance  + ?, " +
                     "pending_balance  = pending_balance  - ? " +
                     "WHERE id = ? AND pending_balance >= ?";
        Connection        connection = null;
        PreparedStatement ps         = null;
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setLong(1, finalPrice);
            ps.setLong(2, finalPrice);
            ps.setString(3, sellerId);
            ps.setLong(4, finalPrice);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                logger.log(Level.INFO, "Settle seller: moved {0} from pending to account for userId: {1}",
                        new Object[]{finalPrice, sellerId});
            } else {
                logger.log(Level.WARNING, "settleSellerBalance: no rows affected for sellerId: {0} — pending insufficient?", sellerId);
            }
            return rows > 0;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.USER_MODIFICATION_FAILED, "Database failure while settling seller balance.", sqlException);
        } catch (Exception exception) {
            throw exception;
        } finally {
            closeResource(ps);
            closeConnect(connection);
        }
    }
    // --- PRIVATE METHODS ---

    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        String   userId   = resultSet.getString("id");
        String   name     = resultSet.getString("name");
        String   userName = resultSet.getString("username");
        String   password = resultSet.getString("password");
        String   email    = resultSet.getString("email");
        UserRole role     = UserRole.valueOf(resultSet.getString("role"));

        return switch (role) {
            case BIDDER -> {
                long balance = resultSet.getLong("bidder_balance");
                long lockedBalance = resultSet.getLong("locked_balance");
                yield new Bidder(userId, name, userName, password, email, role, balance, lockedBalance);
            }
            case SELLER -> {
                String bankAccount    = resultSet.getString("bank_account");
                long   sellerBalance  = resultSet.getLong("seller_balance");
                long   pendingBalance = resultSet.getLong("pending_balance");
                yield new Seller(userId, name, userName, password, email, role, bankAccount, sellerBalance, pendingBalance);
            }
            case ADMIN -> new Admin(userId, name, userName, password, email, role);
            default -> throw new SQLException("Unrecognized user role: " + role);
        };
    }
}