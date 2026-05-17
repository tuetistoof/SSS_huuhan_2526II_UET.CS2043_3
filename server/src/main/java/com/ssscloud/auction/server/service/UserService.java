package com.ssscloud.auction.server.service;


import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.dto.request.LoginRequest;
import com.ssscloud.auction.common.dto.request.RegisterRequest;
import com.ssscloud.auction.common.dto.response.UserDTO;
import com.ssscloud.auction.common.enums.UserRole;
import com.ssscloud.auction.common.exception.ServiceException;
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

    public UserDTO login(LoginRequest loginRequest) throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Initiating user authentication for username: " + (loginRequest != null ? loginRequest.getUsername() : "null"));
            validateLoginRequest(loginRequest);

            User user = userDAO.findByUsername(loginRequest.getUsername());
            if (user == null) {
                throw new ServiceException(ErrorCode.ACCOUNT_NOT_FOUND, "Authentication failed: The specified account does not exist.");
            }
            if (!loginRequest.getPassword().equals(user.getPassword())) {
                throw new ServiceException(ErrorCode.WRONG_PASSWORD, "Authentication failed: The provided password is incorrect.");
            }

            UserDTO userDto = toUserDto(user);
            logger.log(Level.INFO, "User authentication successful for userId: " + user.getId());
            return userDto;
        } catch (ServiceException serviceException) {
            logger.log(Level.WARNING, "Service exception during user login for username: " + (loginRequest != null ? loginRequest.getUsername() : "null"), serviceException);
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during user authentication", exception);
            throw exception;
        }
    }

    public UserDTO register(RegisterRequest registerRequest) throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Initiating user registration for username: " + (registerRequest != null ? registerRequest.getUsername() : "null"));
            validateRegisterRequest(registerRequest);

            if (userDAO.findByUsername(registerRequest.getUsername()) != null) {
                throw new ServiceException(ErrorCode.USERNAME_EXISTED, "Registration failure: The username is already associated with an account: " + registerRequest.getUsername());
            }
            if (userDAO.findByEmail(registerRequest.getEmail()) != null) {
                throw new ServiceException(ErrorCode.EMAIL_EXISTED, "Registration failure: The email address is already associated with an account: " + registerRequest.getEmail());
            }

            User user = buildUser(registerRequest);
            persistUser(user, registerRequest.getRole());
            
            UserDTO userDto = toUserDto(user);
            logger.log(Level.INFO, "User registration successful for userId: " + user.getId() + " with role: " + registerRequest.getRole());
            return userDto;
        } catch (ServiceException serviceException) {
            logger.log(Level.WARNING, "Service exception during user registration for username: " + (registerRequest != null ? registerRequest.getUsername() : "null"), serviceException);
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during user registration", exception);
            throw exception;
        }
    }

    public long deposit(long depositAmount, String userId) throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Initiating deposit operation for userId: " + userId + " with amount: " + depositAmount);
            validateDepositRequest(depositAmount, userId);

            User user = userDAO.findById(userId);
            if (user == null) {
                throw new ServiceException(ErrorCode.ACCOUNT_NOT_FOUND, "Deposit failure: The specified account could not be found.");
            }
            
            long updatedBalance;
            if (user instanceof Bidder bidderAccount) {
                updatedBalance = bidderAccount.getAccountBalance() + depositAmount;
                if (!userDAO.updateAccountBalance(userId, updatedBalance)) {
                    throw new ServiceException(ErrorCode.ACCOUNT_BALANCE_UPDATE_FAILED, "Persistence failure: Failed to update bidder account balance in the database.");
                }
            } else if (user instanceof Seller sellerAccount) {
                updatedBalance = sellerAccount.getAccountBalance() + depositAmount;
                if (!userDAO.updateSellerBalance(userId, updatedBalance)) {
                    throw new ServiceException(ErrorCode.ACCOUNT_BALANCE_UPDATE_FAILED, "Persistence failure: Failed to update seller account balance in the database.");
                }
            } else {
                throw new ServiceException(ErrorCode.INVALID_ROLE, "Authorization failure: The user role is unauthorized for account balance deposit.");
            }
            logger.log(Level.INFO, "Deposit operation completed successfully for userId: " + userId + " new balance: " + updatedBalance);
            return updatedBalance;
        } catch (ServiceException serviceException) {
            logger.log(Level.WARNING, "Service exception during deposit operation for userId: " + userId, serviceException);
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during deposit operation for userId: " + userId, exception);
            throw exception;
        }
    }

    public UserDTO getByUserId(String userId) throws ServiceException, Exception {
        try {
            logger.log(Level.INFO, "Retrieving user information for userId: " + userId);
            validateUserIdRequest(userId);
            
            User user = userDAO.findById(userId);
            if (user == null) {
                throw new ServiceException(ErrorCode.ACCOUNT_NOT_FOUND, "Retrieval failure: The specified account does not exist.");
            }
            
            UserDTO userDto = toUserDto(user);
            logger.log(Level.INFO, "User retrieval successful for userId: " + userId);
            return userDto;
        } catch (ServiceException serviceException) {
            logger.log(Level.WARNING, "Service exception during user retrieval for userId: " + userId, serviceException);
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during user retrieval for userId: " + userId, exception);
            throw exception;
        }
    }

    // --- PRIVATE HELPERS ---

    private User buildUser(RegisterRequest registerRequest) throws ServiceException, Exception {
        try {
            return switch (registerRequest.getRole()) {
                case BIDDER -> new Bidder(registerRequest.getName(), registerRequest.getUsername(), registerRequest.getPassword(), registerRequest.getEmail(), registerRequest.getRole());
                case SELLER -> new Seller(registerRequest.getName(), registerRequest.getUsername(), registerRequest.getPassword(), registerRequest.getEmail(), registerRequest.getRole(), registerRequest.getBankAccount());
                default -> throw new ServiceException(ErrorCode.INVALID_ROLE, "Logic failure: Encountered an unsupported user role: " + registerRequest.getRole());
            };
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during user object construction for role: " + registerRequest.getRole(), exception);
            throw exception;
        }
    }

    private void persistUser(User user, UserRole role) throws ServiceException, Exception {
        try {
            boolean isSaved = switch (role) {
                case BIDDER -> {
                    try { 
                        yield userDAO.saveBidder((Bidder) user);
                    } catch (Exception e) { 
                        throw e; 
                    }
                }
                case SELLER -> userDAO.saveSeller((Seller) user);
                default -> false;
            };
    
            if (!isSaved) {
                throw new ServiceException(ErrorCode.SAVE_ERROR, "Critical persistence failure: Could not save user entity to the database.");
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during user persistence for role: " + role, exception);
            throw exception;
        }
    }

    private UserDTO toUserDto(User user) {
        long accountBalance = 0;
        if (user instanceof Bidder bidderAccount) {
            accountBalance = bidderAccount.getAccountBalance();
        } else if (user instanceof Seller sellerAccount) {
            accountBalance = sellerAccount.getAccountBalance();
        }
        long unsettledBalance = 0;
        if (user instanceof Seller sellerAccount) {
            unsettledBalance = sellerAccount.getPendingBalance();
        }
        if (user instanceof Bidder bidderAccount){
            unsettledBalance = bidderAccount.getLockedBalance();
        }
        return new UserDTO(user.getId(), user.getUserName(), user.getEmail(), user.getRole(), accountBalance, unsettledBalance);
    }

    // --- VALIDATION METHODS ---

    private void validateLoginRequest(LoginRequest loginRequest) throws ServiceException, Exception {
        try {
            if (loginRequest == null) {
                throw new ServiceException(ErrorCode.INVALID_LOGIN_REQUEST, "The login request payload cannot be null.");
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during login request validation", exception);
            throw exception;
        }
    }

    private void validateRegisterRequest(RegisterRequest registerRequest) throws ServiceException, Exception {
        try {
            if (registerRequest == null) {
                throw new ServiceException(ErrorCode.INVALID_REGISTER_REQUEST, "The registration request payload cannot be null.");
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during register request validation", exception);
            throw exception;
        }
    }

    private void validateDepositRequest(long depositAmount, String userId) throws ServiceException, Exception {
        try {
            if (depositAmount <= 0) {
                throw new ServiceException(ErrorCode.INVALID_DEPOSIT, "Validation failure: The deposit amount must be a positive integer greater than zero.");
            }
            if (userId == null || userId.isBlank()) {
                throw new ServiceException(ErrorCode.INVALID_DATA, "Validation failure: The user identifier is mandatory for the deposit operation.");
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during deposit request validation", exception);
            throw exception;
        }
    }

    private void validateUserIdRequest(String userId) throws ServiceException, Exception {
        try {
            if (userId == null || userId.isBlank()) {
                throw new ServiceException(ErrorCode.INVALID_DATA, "Validation failure: The user identifier cannot be null or empty.");
            }
        } catch (ServiceException serviceException) {
            throw serviceException;
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unexpected error during userId request validation", exception);
            throw exception;
        }
    }
}