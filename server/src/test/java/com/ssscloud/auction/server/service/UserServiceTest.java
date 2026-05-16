package com.ssscloud.auction.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.ssscloud.auction.common.exception.ServiceException;
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

    // --- Login Tests ---

    @Test
    void testLoginSuccess() throws Exception{
        Bidder bidder = new Bidder("Nguyễn Khánh Phong", "Kphong", "123456@", "Kphong@gmail.com", UserRole.BIDDER);
        when(userDAO.findByUsername("Kphong")).thenReturn(bidder);

        LoginRequest loginRequest = new LoginRequest("Kphong", "123456@");
        UserDTO result = userService.login(loginRequest);

        assertNotNull(result);
        assertEquals("Kphong", result.getUsername());
    }

    @Test
    void testLoginNullRequest() {
        assertThrows(ServiceException.class, () -> userService.login(null));
    }

    @Test
    void testLoginIncorrectPassword() throws Exception{
        Bidder bidder = new Bidder("Nguyễn Khánh Phong", "Kphong", "123456@", "Kphong@gmail.com", UserRole.BIDDER);
        when(userDAO.findByUsername("Kphong")).thenReturn(bidder);

        LoginRequest loginRequest = new LoginRequest("Kphong", "wrongpassword");
        assertThrows(ServiceException.class, () -> userService.login(loginRequest));
    }

    @Test
    void testLoginUserNotFound() throws Exception{
        when(userDAO.findByUsername("NonExistentUser")).thenReturn(null);

        LoginRequest loginRequest = new LoginRequest("NonExistentUser", "anyPassword");
        assertThrows(ServiceException.class, () -> userService.login(loginRequest));
    }

    // --- Register Tests ---

    @Test
    void testRegisterSuccess() throws Exception{
        when(userDAO.findByUsername("newUser")).thenReturn(null);
        when(userDAO.findByEmail("newuser@gmail.com")).thenReturn(null);
        when(userDAO.saveBidder(any(Bidder.class))).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest("New User", "newUser", "password123", "newuser@gmail.com", UserRole.BIDDER);
        UserDTO result = userService.register(registerRequest);

        assertNotNull(result);
        assertEquals("newUser", result.getUsername());
    }

    @Test
    void testRegisterNullRequest() {
        assertThrows(ServiceException.class, () -> userService.register(null));
    }

    @Test
    void testRegisterExistedUsername() throws Exception{
        Bidder existingBidder = new Bidder("Existing User", "existingUser", "password123", "existinguser@gmail.com", UserRole.BIDDER);
        when(userDAO.findByUsername("existingUser")).thenReturn(existingBidder);

        RegisterRequest registerRequest = new RegisterRequest("New User", "existingUser", "password123", "newuser@gmail.com", UserRole.BIDDER);
        assertThrows(ServiceException.class, () -> userService.register(registerRequest));
    }

    @Test
    void testRegisterExistedEmail() throws Exception{
        Seller existingSeller = new Seller("Existing User", "existingUser", "password123", "existinguser@gmail.com", UserRole.SELLER);
        when(userDAO.findByEmail("existinguser@gmail.com")).thenReturn(existingSeller);

        RegisterRequest registerRequest = new RegisterRequest("New User", "newUser", "password123", "existinguser@gmail.com", UserRole.BIDDER);
        assertThrows(ServiceException.class, () -> userService.register(registerRequest));
    }
}