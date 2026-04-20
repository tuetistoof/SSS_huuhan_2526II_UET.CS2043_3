package com.ssscloud.auction.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    //singleton
    private static DatabaseConnection instance;

    private Connection connection;

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

            
            // Tạo connection 1 lần duy nhất
            this.connection = DriverManager.getConnection(url, user, pass);
            System.out.println("Kết nối database thành công!");
            LOGGER.info("Kết nối database thành công: " + url);

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
        try {
            if (connection == null || connection.isClosed()) {
                LOGGER.warning("Connection bi dong hoặc null đang reconnect...");
                synchronized (DatabaseConnection.class) {
                    if (connection == null || connection.isClosed()) {
                        instance = new DatabaseConnection();
                        this.connection = instance.connection;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Loi khi kiem tra trang thai connection", e);
            throw e;
        }
        return connection;
    }
}