// cac ham dung chung cho ca DAO
package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseDAO {
    protected final Logger logger = Logger.getLogger(getClass().getName());

    protected Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    protected void safelyRollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                logger.severe("Khong the rollback transaction: " + e.getMessage());
            }
        }
    }
    protected void resetAutocommit (Connection conn){
        if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.warning("Lỗi khi reset autoCommit: " + e.getMessage());
                }
            }
    }

    protected void closeResource(AutoCloseable... resource) {
        for (AutoCloseable r : resource) {
            if (r != null) {
                try {
                    r.close();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "loi ong resource: " + e.getMessage(), e);
                }
            }
        }
    }

}
