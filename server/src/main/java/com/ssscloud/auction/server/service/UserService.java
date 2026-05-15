package com.ssscloud.auction.server.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.request.LoginRequest;
import com.ssscloud.auction.common.dto.request.RegisterRequest;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.ServiceExceptions;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.Bidder;
import com.ssscloud.auction.common.model.Seller;
import com.ssscloud.auction.common.model.base.User;
import com.ssscloud.auction.server.dao.UserDAO;

/**
 * UserService manages the core business logic for user entities, 
 * including authentication, registration, and financial balance operations.
 */
public class UserService {
    private static final Logger logger = Logger.getLogger(UserService.class.getName()); // Logging Standards: First attribute

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // --- PUBLIC METHODS ---

    public UserDTO login(LoginRequest loginRequest) throws ServiceExceptions {
        validateLoginRequest(loginRequest);

        User user = userDAO.findByUsername(loginRequest.getUsername());
        if (user == null) {
            throw new ServiceExceptions(ErrorCode.ACCOUNT_NOT_FOUND, "Authentication failed: The specified account does not exist.");
        }
        if (!loginRequest.getPassword().equals(user.getPassword())) {
            throw new ServiceExceptions(ErrorCode.WRONG_PASSWORD, "Authentication failed: The provided password is incorrect.");
        }

        UserDTO userDto = toUserDto(user);
        return userDto;
    }

    public UserDTO register(RegisterRequest registerRequest) throws ServiceExceptions {
        validateRegisterRequest(registerRequest);

        if (userDAO.findByUsername(registerRequest.getUsername()) != null) {
            throw new ServiceExceptions(ErrorCode.USERNAME_EXISTED, "Registration failure: The username is already associated with an account: " + registerRequest.getUsername());
        }
        if (userDAO.findByEmail(registerRequest.getEmail()) != null) {
            throw new ServiceExceptions(ErrorCode.EMAIL_EXISTED, "Registration failure: The email address is already associated with an account: " + registerRequest.getEmail());
        }

        User user = buildUser(registerRequest);
        persistUser(user, registerRequest.getRole());
        
        UserDTO userDto = toUserDto(user);
        return userDto;
    }

    public long deposit(long depositAmount, String userId) throws ServiceExceptions {
        validateDepositRequest(depositAmount, userId);

        User user = userDAO.findById(userId);
        logger.log(Level.INFO, "Executing deposit operation for userType: " + (user == null ? "NULL" : user.getClass().getSimpleName()));
        
        if (user == null) {
            throw new ServiceExceptions(ErrorCode.ACCOUNT_NOT_FOUND, "Deposition failure: The specified account could not be found.");
        }
        
        long updatedBalance;
        if (user instanceof Bidder bidderAccount) {
            updatedBalance = bidderAccount.getAccountBalance() + depositAmount;
            if (!userDAO.updateAccountBalance(userId, updatedBalance)) {
                throw new ServiceExceptions(ErrorCode.ACCOUNT_BALANCE_UPDATE_FAILED, "Persistence failure: Failed to update bidder account balance in the database.");
            }
        } else if (user instanceof Seller sellerAccount) {
            updatedBalance = sellerAccount.getAccountBalance() + depositAmount;
            if (!userDAO.updateSellerBalance(userId, updatedBalance)) {
                throw new ServiceExceptions(ErrorCode.ACCOUNT_BALANCE_UPDATE_FAILED, "Persistence failure: Failed to update seller account balance in the database.");
            }
        } else {
            throw new ServiceExceptions(ErrorCode.INVALID_ROLE, "Authorization failure: The user role is unauthorized for account balance deposition.");
        }
        return updatedBalance;
    }

    public UserDTO getByUserId(String userId) throws ServiceExceptions {
        validateUserIdRequest(userId);
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new ServiceExceptions(ErrorCode.ACCOUNT_NOT_FOUND, "Retrieval failure: The specified account does not exist.");
        }
        UserDTO userDto = toUserDto(user);
        return userDto;
    }

    // --- PRIVATE HELPERS ---

    private User buildUser(RegisterRequest registerRequest) {
        return switch (registerRequest.getRole()) {
            case BIDDER -> new Bidder(registerRequest.getName(), registerRequest.getUsername(), registerRequest.getPassword(), registerRequest.getEmail(), registerRequest.getRole());
            case SELLER -> new Seller(registerRequest.getName(), registerRequest.getUsername(), registerRequest.getPassword(), registerRequest.getEmail(), registerRequest.getRole());
            default -> throw new ServiceExceptions(ErrorCode.INVALID_ROLE, "Logic failure: Encountered an unsupported user role: " + registerRequest.getRole());
        };
    }

    private void persistUser(User user, UserRole role) throws ServiceExceptions {
        boolean isSaved = switch (role) {
            case BIDDER -> userDAO.saveBidder((Bidder) user);
            case SELLER -> userDAO.saveSeller((Seller) user);
            default -> false;
        };

        if (!isSaved) {
            throw new ServiceExceptions(ErrorCode.SAVE_ERROR, "Critical persistence failure: Could not save user entity to the database.");
        }
    }

    private UserDTO toUserDto(User user) {
        long accountBalance = 0;
        if (user instanceof Bidder bidderAccount) {
            accountBalance = bidderAccount.getAccountBalance();
        } else if (user instanceof Seller sellerAccount) {
            accountBalance = sellerAccount.getAccountBalance();
        }
        return new UserDTO(user.getId(), user.getUserName(), user.getEmail(), user.getRole(), accountBalance);
    }

    // --- VALIDATION METHODS ---

    private void validateLoginRequest(LoginRequest loginRequest) throws ServiceExceptions {
        if (loginRequest == null) throw new ServiceExceptions(ErrorCode.INVALID_LOGIN_REQUEST, "The login request payload cannot be null.");
        // Further validation (e.g., username/password format) is handled by Controller
    }

    private void validateRegisterRequest(RegisterRequest registerRequest) throws ServiceExceptions {
        if (registerRequest == null) throw new ServiceExceptions(ErrorCode.INVALID_REGISTER_REQUEST, "The registration request payload cannot be null.");
        // Further validation (e.g., username/password/email format, length) is handled by Controller
    }

    private void validateDepositRequest(long depositAmount, String userId) throws ServiceExceptions {
        if (depositAmount <= 0) {
            throw new ServiceExceptions(ErrorCode.INVALID_DEPOSIT, "Validation failure: The deposit amount must be a positive integer greater than zero.");
        }
        if (userId == null || userId.isBlank()) {
            throw new ServiceExceptions(ErrorCode.INVALID_DATA, "Validation failure: The user identifier is mandatory for the deposition operation.");
        }
    }

    private void validateUserIdRequest(String userId) throws ServiceExceptions {
        if (userId == null || userId.isBlank()) {
            throw new ServiceExceptions(ErrorCode.INVALID_DATA, "Validation failure: The user identifier cannot be null or empty.");
        }
    }
}