package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.Admin;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.Seller;
import com.ssscloud.auction.common.model.base.User;

public class UserDAO extends BaseDAO {
    public boolean saveBidder(Bidder bidder) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlUser = "INSERT INTO user (id, user_name, password, email, role) VALUES (?, ?, ?, ?, ?)";
        String sqlBidder = "INSERT INTO bidder (id, account_balance) VALUES (?, ?)";

        Connection conn = null;
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
            logger.info("da luu bidder: " + bidder.getUserName());
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("User name da ton tai: " + bidder.getUserName() + " - " + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi kh luu Bidder: " + bidder.getUserName() + " - " + e.getMessage());
            safelyRollback(conn);
            return false;
        } finally {
            resetAutocommit(conn);
            closeResource(psEntity, psUser, psBidder);
        }
    }

    public boolean saveSeller(Seller seller) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlUser = "INSERT INTO user (id, user_name, password, email, role) VALUES (?, ?, ?, ?, ?)";
        String sqlSeller = "INSERT INTO bidder (id, bank_account) VALUES (?, ?)";

        Connection conn = null;
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
            psSeller.executeUpdate();

            conn.commit();
            logger.info("da luu seller: " + seller.getUserName());
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("User name da ton tai: " + seller.getUserName() + " - " + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi kh luu seller: " + seller.getUserName() + " - " + e.getMessage());
            safelyRollback(conn);
            return false;
        } finally {
            resetAutocommit(conn);
            closeResource(psEntity, psUser, psSeller);
        }
    }

    public User findByUsername(String userName) {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "u.user_name, u.password, u.email, u.role, " +
                "b.account_balance, s.bank_account " +
                "FROM entity e " +
                "JOIN user u ON e.id = u.id " +
                "LEFT JOIN bidder b ON u.id = b.id " +
                "LEFT JOIN seller s ON u.id = s.id " +
                "WHERE u.userName = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userName);
            rs = ps.executeQuery();

            if (rs.next())
                return mapResultSetToUser(rs);
            else {
                logger.info("Khong tim thay user: " + userName);
                return null;
            }
        } catch (SQLException e) {
            logger.info("Loi khi tim theo userName: " + userName + " - " + e.getMessage());
            return null;
        } finally {
            closeResource(rs, ps);
        }
    }
    
    public User findByEmail(String email) {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "u.user_name, u.password, u.email, u.role, " +
                "b.account_balance, s.bank_account " +
                "FROM entity e " +
                "JOIN user u ON e.id = u.id " +
                "LEFT JOIN bidder b ON u.id = b.id " +
                "LEFT JOIN seller s ON u.id = s.id " +
                "WHERE u.email = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            rs = ps.executeQuery();

            if (rs.next())
                return mapResultSetToUser(rs);
            else {
                logger.info("Khong tim thay user: " + email);
                return null;
            }
        } catch (SQLException e) {
            logger.info("Loi khi tim theo userName: " + email + " - " + e.getMessage());
            return null;
        } finally {
            closeResource(rs, ps);
        }
    }
    // dung cho login register và mot so tac vu
    public User findById(String id) {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "u.user_name, u.password, u.email, u.role, " +
                "b.account_balance, s.bank_account " +
                "FROM entity e " +
                "JOIN user u ON e.id = u.id " +
                "LEFT JOIN bidder b ON u.id = b.id " +
                "LEFT JOIN seller s ON u.id = s.id " +
                "WHERE e.id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;

        } catch (SQLException e) {
            logger.severe("Loi khi tim theo id " + id + " - " + e.getMessage());
            return null;
        } finally {
            closeResource(rs, ps);
        }
    }
    // kiem tra ten dang nhap da ton tai chua
    public boolean existByUsername(String userName) {
        String sql = "SELECT 1 FROM user WHERE userName = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userName);
            rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            logger.severe("Lỗi existsByUsername: " + userName + " - " + e.getMessage());
            return false;
        } finally {
            closeResource(rs, ps);
        }
    }

    // cac ham update sua thong tin user
    public boolean updatePassword(String id, String newPassword) {
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newPassword);
            ps.setString(2, id);
            int row = ps.executeUpdate();
            logger.info("updatePassword userId = " + id + " - row =" + row);
            return row > 0;

        } catch (SQLException e) {
            logger.severe("Lỗi update password id: " + id + " - " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
        }
    }
    public boolean updateEmail(String id, String email) {
        String sql = "UPDATE user SET email = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, id);
            int row = ps.executeUpdate();
            logger.info("update email userId = " + id + " - row =" + row);
            return row > 0;

        } catch (SQLException e) {
            logger.severe("Lỗi update email id: " + id + " - " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
        }
    }
    public boolean updateAccountBalance (String id, String newAccountBlance) {
        String sql = "UPDATE bidder SET account_balance = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newAccountBlance);
            ps.setString(2, id);
            int row = ps.executeUpdate();
            logger.info("update account balance userId = " + id + " - row =" + row);
            return row > 0;

        } catch (SQLException e) {
            logger.severe("Lỗi update account balance id: " + id + " - " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
        }
    }
    public boolean updateBankAccount (String id, String newBankAccount) {
        String sql = "UPDATE seller SET bank_account = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newBankAccount);
            ps.setString(2, id);
            int row = ps.executeUpdate();
            logger.info("update bank account userId = " + id + " - row =" + row);
            return row > 0;

        } catch (SQLException e) {
            logger.severe("Lỗi bank account blance id: " + id + " - " + e.getMessage());
            return false;
        } finally {
            closeResource(ps);
        }
    }
    // ham ho tro
    public User mapResultSetToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String userName = rs.getString("userName");
        String password = rs.getString("password");
        String email = rs.getString("email");
        UserRole role = UserRole.valueOf(rs.getString("role"));
        switch (role) {
            case BIDDER: {
                long balance = rs.getLong("account_balance");
                Bidder b = new Bidder(id, name, userName, password, email, role, balance);
                return b;
            }
            case SELLER: {
                String bankAccount = rs.getString("bank_account");
                Seller s = new Seller(id, name, userName, password, email, role, bankAccount);
                return s;
            }
            case ADMIN: {
                Admin a = new Admin(id, name, userName, password, email, role);
                return a;
            }
            default:
                throw new SQLException("UserRole khong xac dinh duoc role " + role);
        }
    }

}
