/**
 * BaseDAO provides shared utility methods for all Data Access Objects within the persistence layer.
 * It handles database connection acquisition, transaction rollbacks, and resource management.
 */
package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;

public abstract class BaseDAO {
    protected static final Logger logger = Logger.getLogger(BaseDAO.class.getName());

    // --- PROTECTED METHODS ---

    /**
     * Retrieves a managed database connection from the centralized infrastructure pool.
     * @return A valid Connection object.
     * @throws DAOException, Exception if a connection cannot be established or an unexpected error occurs.
     * Lấy một kết nối cơ sở dữ liệu được quản lý từ pool hạ tầng tập trung.
     * @return Đối tượng Connection hợp lệ.
     * @throws DAOException nếu không thể thiết lập kết nối.
     * @throws Exception cho các lỗi hệ thống chưa xác định.
     */
    protected Connection getConnection() throws DAOException, Exception {
        try {
            return DatabaseConnection.getInstance().getConnection();
        } catch (DAOException daoException) {
            logger.log(Level.SEVERE, "[" + ErrorCode.CONNECTION_FAILURE + "] Lỗi tại " + getClass().getSimpleName() + ": Không thể lấy kết nối từ DatabaseConnection pool.", daoException);
            throw daoException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Lỗi hệ thống không xác định tại " + getClass().getSimpleName() + ": " + exception.getMessage(), exception);
            throw exception;
        }
    }

    protected void safelyRollback(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException sqlException) {
                logger.log(Level.SEVERE, "[" + ErrorCode.TRANSACTION_ROLLBACK_FAILED + "] Persistence failure: Unable to perform database transaction rollback. Details: " + sqlException.getMessage(), sqlException);
            }
        }
    }

    protected void resetAutocommit(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException sqlException) {
                logger.log(Level.WARNING, "[" + ErrorCode.AUTO_COMMIT_RESET_FAILED + "] Infrastructure warning: Failed to reset database auto-commit state. Message: " + sqlException.getMessage());
            }
        }
    }

    protected void closeResource(AutoCloseable... databaseResourcesList) { // Naming: List suffix for varargs collection
        for (AutoCloseable databaseResource : databaseResourcesList) {
            if (databaseResource != null) {
                try {
                    databaseResource.close();
                } catch (Exception exception) {
                    logger.log(Level.WARNING, "[" + ErrorCode.RESOURCE_CLEANUP_FAILED + "] Resource cleanup failure: Failed to close database resource. Reason: " + exception.getMessage());
                }
            }
        }
    }

    protected void closeConnect(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.log(Level.INFO, "Infrastructure status: Centralized database connection has been returned to the pool.");
            }
        } catch (SQLException sqlException) {
             logger.log(Level.SEVERE, "[" + ErrorCode.CONNECTION_CLOSE_FAILED + "] Connectivity failure: Failed to close the database connection. Message: " + sqlException.getMessage());
        }
    }
}
