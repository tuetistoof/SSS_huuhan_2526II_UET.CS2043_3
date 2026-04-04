package com.ssscloud.auction.server.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    //singleton
    private static DatabaseConnection instance;

    private Connection connection;

    private DatabaseConnection() throws SQLException {
        try {
            Properties props = new Properties();
            InputStream input = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("application.properties");

            props.load(input);

            String url = props.getProperty("spring.datasource.url");
            String user = props.getProperty("spring.datasource.username");
            String pass = props.getProperty("spring.datasource.password");

            // Tạo connection 1 lần duy nhất
            connection = DriverManager.getConnection(url, user, pass);
            System.out.println("Kết nối database thành công!");

        } catch (Exception e) {
            System.out.println("Lỗi kết nối: " + e.getMessage());
            throw new SQLException(e);
        }
    }

    public static DatabaseConnection getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            instance = new DatabaseConnection();
            return instance.connection;
        }
        return connection;
    }
}