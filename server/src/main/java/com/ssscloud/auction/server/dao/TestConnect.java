package com.ssscloud.auction.server.dao;

import java.sql.Connection;

import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.Seller;
import com.ssscloud.auction.common.model.base.User;

public class TestConnect {
   static UserDAO userDAO = new UserDAO();
    // ==================== REGISTER ====================

    static void testRegisterBidder() {
        System.out.println("\n=== TEST: Register Bidder ===");
        Bidder bidder = new Bidder(
            "Nguyen Van A",
            "nguyenvana",        // username
            "123456",            // password
            "a@gmail.com",       // email
            UserRole.BIDDER,
            1000000L             // account balance
        );
        boolean result = userDAO.saveBidder(bidder);
        System.out.println(result ? "PASS: Tao bidder thanh cong" : "FAIL: Tao bidder that bai");
    }

    static void testRegisterSeller() {
        System.out.println("\n=== TEST: Register Seller ===");
        Seller seller = new Seller(
            "Tran Thi B",
            "tranthib",          // username
            "abcdef",            // password
            "b@gmail.com",       // email
            UserRole.SELLER,
            "MB-9999888"         // bank account
        );
        boolean result = userDAO.saveSeller(seller);
        System.out.println(result ? "PASS: Tao seller thanh cong" : "FAIL: Tao seller that bai");
    }

    static void testRegisterDuplicate() {
        System.out.println("\n=== TEST: Register trung username (phai FAIL) ===");
        Bidder bidder = new Bidder(
            "Nguyen Van A Clone",
            "nguyenvana",        // username trung voi testRegisterBidder
            "000000",
            "clone@gmail.com",
            UserRole.BIDDER,
            0L
        );
        boolean result = userDAO.saveBidder(bidder);
        System.out.println(!result ? "PASS: Chặn đúng duplicate username" : "FAIL: Đã cho tạo trùng username!");
    }

    // ==================== LOGIN ====================

    static User login(String username, String password) {
        // Bước 1: tìm user theo username
        User user = userDAO.findByUsername(username);
        if (user == null) return null;

        // Bước 2: so sánh password plain text
        if (!user.getPassword().equals(password)) return null;

        return user;
    }

    static void testLoginSuccess() {
        System.out.println("\n=== TEST: Login dung username & password ===");
        User user = login("nguyenvana", "123456");
        if (user != null)
            System.out.println("PASS: Login thanh cong - role: " + user.getRole() + " - name: " + user.getName());
        else
            System.out.println("FAIL: Login that bai");
    }

    static void testLoginWrongPassword() {
        System.out.println("\n=== TEST: Login sai password (phai FAIL) ===");
        User user = login("nguyenvana", "saimatkhau");
        System.out.println(user == null ? "PASS: Chặn đúng sai password" : "FAIL: Đã cho login với sai password!");
    }

    static void testLoginNotFound() {
        System.out.println("\n=== TEST: Login username khong ton tai (phai FAIL) ===");
        User user = login("khongtontai", "123456");
        System.out.println(user == null ? "PASS: Chặn đúng user không tồn tại" : "FAIL: Đã cho login user ma!");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Dang ket noi...");

        Connection conn = DatabaseConnection.getInstance().getConnection();

        if (conn != null) {
            System.out.println("Noi duoc roi siuuuuuuuuu!");
            System.out.println("Database: " + conn.getCatalog());
        } else {
            System.out.println("Sai cmnr!");
        }
        testRegisterBidder();
        testRegisterSeller();
        testRegisterDuplicate();
        testLoginSuccess();
        testLoginWrongPassword();
        testLoginNotFound();
    }
}