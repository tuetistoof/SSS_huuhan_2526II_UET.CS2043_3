package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ssscloud.auction.common.dto.request.LoginRequest;
import com.ssscloud.auction.common.dto.request.RegisterRequest;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.Seller;
import com.ssscloud.auction.server.dao.UserDAO;

public class UserServiceTest {

    private UserDAO userDAO;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userDAO = Mockito.mock(UserDAO.class);
        userService = new UserService(userDAO);
    }


    //Login Tests
    @Test
    void testLoginSuccess(){
        Bidder bidder = new Bidder("Nguyễn Khánh Phong", "Kphong", "123456@", "Kphong@gmail.com", UserRole.BIDDER);
        when(userDAO.findByUsername("Kphong")).thenReturn(bidder);

        LoginRequest req = new LoginRequest("Kphong", "123456@");
        UserDTO result = userService.login(req);

        assertNotNull(result);
        assertEquals("Kphong", result.getUsername());   
    }

    @Test
    void testEmptyUsername(){
        LoginRequest req = new LoginRequest("", "anyPassword");
        assertThrows(IllegalArgumentException.class,  () -> userService.login(req));
    }
    
    @Test
    void testIncorrectPassword(){
        Bidder bidder = new Bidder("Nguyễn Khánh Phong", "Kphong", "123456@", "Kphong@gmail.com", UserRole.BIDDER);
        when(userDAO.findByUsername("Kphong")).thenReturn(bidder);
        
        LoginRequest req = new LoginRequest("Kphong", "wrongpassword");
        assertThrows(IllegalArgumentException.class,  () -> userService.login(req));
    }

    @Test
    void testUserNotFound(){
        when(userDAO.findByUsername("NonExistentUser")).thenReturn(null);
        
        LoginRequest req = new LoginRequest("NonExistentUser", "anyPassword");
        assertThrows(IllegalArgumentException.class,  () -> userService.login(req));
    }

    // Register tests
    @Test
    void testRegisterSuccess(){
        when(userDAO.findByUsername("newUser")).thenReturn(null);
        when(userDAO.findByEmail("newuser@gmail.com")).thenReturn(null);
        when(userDAO.saveBidder(any(Bidder.class))).thenReturn(true);

        RegisterRequest req = new RegisterRequest("New User", "newUser", "password123", "newuser@gmail.com", UserRole.BIDDER);
        UserDTO result = userService.register(req);

        assertNotNull(result);
        assertEquals("newUser", result.getUsername());
    }

    @Test
    void testExistedUsername(){
        Bidder existingBidder = new Bidder("Existing User", "existingUser", "password123", "existinguser@gmail.com", UserRole.BIDDER);
        when(userDAO.findByUsername("existingUser")).thenReturn(existingBidder);

        RegisterRequest req = new RegisterRequest("New User", "existingUser", "password123", "newuser@gmail.com", UserRole.BIDDER);
        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
    }

    @Test
    void testExistedEmail(){
        Seller existingSeller = new Seller("Existing User", "existingUser", "password123", "existinguser@gmail.com", UserRole.SELLER);
        when(userDAO.findByEmail("existinguser@gmail.com")).thenReturn(existingSeller);

        RegisterRequest req = new RegisterRequest("New User", "newUser", "password123", "existinguser@gmail.com", UserRole.BIDDER);
        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
    }
    
    @Test
    void testInvalidEmail(){
        RegisterRequest req = new RegisterRequest("New User", "newUser", "password123", "invalidemail", UserRole.BIDDER);
        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
    }

    @Test
    void testShortUsername(){
        RegisterRequest req = new RegisterRequest("New User", "ab", "password123", "newuser@gmail.com", UserRole.BIDDER);
        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
    }

    @Test
    void testLongUsername(){
        String longUsername = "a".repeat(21);
        RegisterRequest req = new RegisterRequest("New User", longUsername, "password123", "newuser@gmail.com", UserRole.BIDDER);
        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
    }

    @Test
    void testShortPassword(){
        RegisterRequest req = new RegisterRequest("New User", "newUser", "12345", "newuser@gmail.com", UserRole.BIDDER);
        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
    }

    @Test
    void testLongPassword(){
        String longPassword = "a".repeat(101);
        RegisterRequest req = new RegisterRequest("New User", "newUser", longPassword, "newuser@gmail.com", UserRole.BIDDER);
        assertThrows(IllegalArgumentException.class, () -> userService.register(req));
    }
}