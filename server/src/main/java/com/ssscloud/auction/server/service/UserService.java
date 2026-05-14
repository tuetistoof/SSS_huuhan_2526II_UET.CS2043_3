package com.ssscloud.auction.server.service;

import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.request.LoginRequest;
import com.ssscloud.auction.common.dto.request.RegisterRequest;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.ServiceExceptions;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.Seller;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.server.dao.UserDAO;

/*
 * UserService is responsible for handling user-related business logic, including:
 * - Login
 * - Registration
 * It interacts with UserDAO to retrieve and store user data in the database.
 */
public class UserService {
    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public UserDTO login(LoginRequest req) {
        User user = userDAO.findByUsername(req.getUsername());

        if (user == null)
            throw new ServiceExceptions("ACCOUNT_NOT_FOUND", "Account does not exist");
        if (!req.getPassword().equals(user.getPassword()))
            throw new ServiceExceptions("WRONG_PASSWORD", "Incorrect password");

        return toDTO(user);
    }

    public UserDTO register(RegisterRequest req) {
        if (userDAO.findByUsername(req.getUsername()) != null)
            throw new ServiceExceptions("USERNAME_EXISTED", "Username already exists: " + req.getUsername());

        if (userDAO.findByEmail(req.getEmail()) != null)
            throw new ServiceExceptions("EMAIL_EXISTED", "Email already exists: " + req.getEmail());

        User user = buildUser(req);
        persistUser(user, req.getRole());
        return toDTO(user);
    }

    public long deposit(long amount, String userId) {
        User user = userDAO.findById(userId);
        logger.info(">>> found user: " + (user == null ? "NULL" : user.getClass().getSimpleName()));
        
        if (user == null) {
            throw new ServiceExceptions("ACCOUNT_NOT_FOUND", "Account does not exist");
        }

        long newBalance = 0;
        if (user instanceof Bidder b) {
            newBalance = b.getAccountBalance() + amount;
            userDAO.updateAccountBalance(userId, newBalance);
        } else if (user instanceof Seller s) {
            newBalance = s.getAccountBalance() + amount;
            userDAO.updateSellerBalance(userId, newBalance);
        }
        return newBalance;
    }

    public UserDTO getByUserId(String id) {
        User user = userDAO.findById(id);
        if (user == null) {
            throw new ServiceExceptions("ACCOUNT_NOT_FOUND", "Account does not exist");
        }
        return toDTO(user);
    }

    private User buildUser(RegisterRequest req) {
        return switch (req.getRole()) {
            case BIDDER -> new Bidder(req.getName(), req.getUsername(), req.getPassword(), req.getEmail(), req.getRole());
            case SELLER -> new Seller(req.getName(), req.getUsername(), req.getPassword(), req.getEmail(), req.getRole());
            default -> throw new ServiceExceptions("INVALID_ROLE", "Invalid role: " + req.getRole());
        };
    }

    private void persistUser(User user, UserRole role) {
        boolean saved = switch (role) {
            case BIDDER -> userDAO.saveBidder((Bidder) user);
            case SELLER -> userDAO.saveSeller((Seller) user);
            default -> false;
        };

        if (!saved)
            throw new ServiceExceptions("SAVE_ERROR", "Error saving user to database");
    }

    private UserDTO toDTO(User user) {
        long balance = 0;
        if (user instanceof Bidder b) balance = b.getAccountBalance();
        else if (user instanceof Seller s) balance = s.getAccountBalance();
        return new UserDTO(user.getId(), user.getUserName(), user.getEmail(), user.getRole(), balance);
    }
}