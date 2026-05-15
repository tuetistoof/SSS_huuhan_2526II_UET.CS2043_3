package com.ssscloud.auction.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.exception.DAOExceptions;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * DatabaseConnection manages the lifecycle and configuration of the HikariCP connection pool.
 * It follows the Singleton pattern to ensure a shared resource across the persistence layer.
 */
public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName()); // Logging Standard: First Attribute

    private static DatabaseConnection instance;

    private final HikariDataSource dataSource;

    // --- CONSTRUCTOR ---

    private DatabaseConnection() throws DAOExceptions { // Technical English and Explicit Throws
        try {
            InputStream inputStream = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("application.properties");
            
            if (inputStream == null){
                throw new DAOExceptions(ErrorCode.DB_CONFIG_NOT_FOUND, "Configuration failure: The application.properties file was not found in the classpath.");
            }
            Properties databaseProperties = new Properties();
            databaseProperties.load(inputStream);
            
            String databaseUrl = databaseProperties.getProperty("spring.datasource.url");
            String databaseUser = databaseProperties.getProperty("spring.datasource.username");
            String databasePassword = databaseProperties.getProperty("spring.datasource.password");

            if (databaseUrl == null || databaseUser == null || databasePassword == null) {
                throw new DAOExceptions(ErrorCode.DB_CONFIG_MISSING, "Configuration failure: Missing required database connection parameters (url/username/password).");
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
        catch (IOException ioException){
            logger.log(Level.SEVERE, "IO failure: Unable to read the application.properties configuration file.", ioException);
            throw new DAOExceptions(ErrorCode.DB_CONFIG_READ_ERROR, "Configuration read failure: " + ioException.getMessage());
        }
        catch (Exception genericException) {
            logger.log(Level.SEVERE, "Connectivity failure: Failed to connect to the database server during initialization.", genericException);
            throw new DAOExceptions(ErrorCode.CONNECTION_FAILURE, "Critical database initialization failure: " + genericException.getMessage());
        }
    }

    // --- PUBLIC METHODS ---

    public static DatabaseConnection getInstance() throws DAOExceptions {
        if (instance == null) { 
            synchronized (DatabaseConnection.class) {
                if (instance == null) { 
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws DAOExceptions {
        try {
            return dataSource.getConnection();
        } catch (SQLException sqlException) {
            logger.log(Level.SEVERE, "Persistence failure: Unable to acquire a database connection from the pool.", sqlException);
            throw new DAOExceptions(ErrorCode.CONNECTION_FAILURE, "Database connection acquisition failure.");
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.log(Level.INFO, "HikariCP connection pool has been closed successfully.");
        }
    }
}