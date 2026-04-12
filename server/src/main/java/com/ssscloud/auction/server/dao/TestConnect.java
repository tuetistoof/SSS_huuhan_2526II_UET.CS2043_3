package com.ssscloud.auction.server.dao;

import java.sql.Connection;

public class TestConnect {
    public static void main(String[] args) throws Exception {
        System.out.println("Dang ket noi...");
        
        Connection conn = DatabaseConnection.getInstance().getConnection();
        
        if (conn != null) {
            System.out.println("Noi duoc roi siuuuuuuuuu!");
            System.out.println("Database: " + conn.getCatalog());
        } else {
            System.out.println("Sai cmnr!");
        }
    }
}