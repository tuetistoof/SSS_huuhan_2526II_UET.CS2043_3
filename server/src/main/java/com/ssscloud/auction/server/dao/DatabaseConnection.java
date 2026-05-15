package com.ssscloud.auction.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * DatabaseConnection manages the lifecycle and configuration of the HikariCP connection pool.
 * It follows the Singleton pattern to ensure a shared resource across the persistence layer.
 */
public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

    private static DatabaseConnection instance;
    private final HikariDataSource dataSource;

    // --- CONSTRUCTOR ---

    private DatabaseConnection() throws DAOException, Exception {
        try {
            InputStream inputStream = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("application.properties");
            
            if (inputStream == null){
                throw new DAOException(ErrorCode.DB_CONFIG_NOT_FOUND, "Infrastructure failure: The application.properties configuration file was not found in the classpath.");
            }
            Properties databaseProperties = new Properties();
            databaseProperties.load(inputStream);
            
            String databaseUrl = databaseProperties.getProperty("spring.datasource.url");
            String databaseUser = databaseProperties.getProperty("spring.datasource.username");
            String databasePassword = databaseProperties.getProperty("spring.datasource.password");

            if (databaseUrl == null || databaseUser == null || databasePassword == null) {
                throw new DAOException(ErrorCode.DB_CONFIG_MISSING, "Configuration failure: Missing required database connection parameters (url/username/password).");
            }

            HikariConfig poolConfig = new HikariConfig();
            poolConfig.setJdbcUrl(databaseUrl);
            poolConfig.setUsername(databaseUser);
            poolConfig.setPassword(databasePassword);
            
            // Pool configuration settings
            poolConfig.setMaximumPoolSize(18);
            poolConfig.setMinimumIdle(3);
            poolConfig.setConnectionTimeout(30000);
            poolConfig.setIdleTimeout(60000);
            poolConfig.setMaxLifetime(180000);

            poolConfig.setConnectionTestQuery("SELECT 1");
            poolConfig.setPoolName("AuctionHikariPool");

            this.dataSource = new HikariDataSource(poolConfig);

            logger.log(Level.INFO, "HikariCP connection pool initialized successfully for URL: " + databaseUrl);
        } 
        catch (IOException ioException) {
            logger.log(Level.SEVERE, "Input/Output failure: Unable to access the application.properties configuration file.", ioException);
            throw new DAOException(ErrorCode.DB_CONFIG_READ_ERROR, "Configuration read failure: " + ioException.getMessage());
        }
        catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error during DatabaseConnection initialization: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    // --- PUBLIC METHODS ---

    public static DatabaseConnection getInstance() throws DAOException, Exception {
        try {
            if (instance == null) { 
                synchronized (DatabaseConnection.class) {
                    if (instance == null) { 
                        instance = new DatabaseConnection();
                    }
                }
            }
            return instance;
        } catch (DAOException daoException) {
            throw daoException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in DatabaseConnection.getInstance: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    public Connection getConnection() throws DAOException, Exception {
        try {
            return dataSource.getConnection();
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Database connectivity failure: Unable to acquire a valid connection from the Hikari pool.", sqlException);
            throw new DAOException(ErrorCode.CONNECTION_FAILURE, "Connection acquisition failure: " + sqlException.getMessage());
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in DatabaseConnection.getConnection: " + exception.getMessage(), exception);
            throw exception;
        }
    }

    public void close() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.log(Level.INFO, "Infrastructure status: HikariCP connection pool has been gracefully terminated.");
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Critical failure during database pool shutdown: " + exception.getMessage(), exception);
        }
    }
}