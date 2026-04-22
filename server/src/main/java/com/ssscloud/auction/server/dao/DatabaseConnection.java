package com.ssscloud.auction.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    //singleton
    private static DatabaseConnection instance;

    private Connection connection;

    private final HikariDataSource dataSource;
    private DatabaseConnection() throws SQLException {
        try {
            InputStream input = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("application.properties");
            
            if (input == null){
                throw new SQLException("Khong thay file application.properties trong class path");
            }
            Properties props = new Properties();
            props.load(input);
            
            String url = props.getProperty("spring.datasource.url");
            String user = props.getProperty("spring.datasource.username");
            String pass = props.getProperty("spring.datasource.password");

            if (url == null || user == null || pass == null) {
                throw new SQLException("thieu cau hinh DB trong application.properties (url/username/password)");
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
            
            config.setMaximumPoolSize(18);
            config.setMinimumIdle(3);
            
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(60000);
            config.setMaxLifetime(180000);

            config.setConnectionTestQuery("SELECT 1");

            config.setPoolName("AuctionHikariPool");

            this.dataSource = new HikariDataSource(config);

            LOGGER.info("Khởi tạo HikariCP pool thành công: " + url);
            System.out.println("Kết nối database (HikariCP) thành công!");
        } 
        catch (IOException e){
            LOGGER.log(Level.SEVERE, "Không đọc được application.properties", e);
            throw new SQLException("Lỗi đọc cấu hình: " + e.getMessage(), e);
        }
        catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi kết nối MySQL: " + e.getMessage(), e);
            throw e;
        }
    }

    public static DatabaseConnection getInstance() throws SQLException {
        if (instance == null) { 
            synchronized (DatabaseConnection.class) {
                if (instance == null) { 
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("HikariCP pool đã đóng.");
        }
    }
}