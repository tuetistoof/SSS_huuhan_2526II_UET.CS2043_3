package com.ssscloud.auction.common.exception;

/**
 * Centralized management of all system error codes.
 * Helps avoid typos when using error codes throughout the codebase.
 * Use IDE auto-complete to select error codes safely.
 */
public class ErrorCode {

    // ==================== VALIDATION ERRORS (Controller) ====================
    
    /** Invalid login request data */
    public static final String INVALID_LOGIN_REQUEST = "INVALID_LOGIN_REQUEST";
    
    /** Invalid registration request data */
    public static final String INVALID_REGISTER_REQUEST = "INVALID_REGISTER_REQUEST";
    
    /** Username is null or empty */
    public static final String INVALID_USERNAME = "INVALID_USERNAME";
    
    /** Password is null or empty */
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
    
    /** Email is null or empty */
    public static final String INVALID_EMAIL = "INVALID_EMAIL";
    
    /** Username length is invalid */
    public static final String INVALID_LENGTH_USERNAME = "INVALID_LENGTH_USERNAME";
    
    /** Password length is invalid */
    public static final String INVALID_LENGTH_PASSWORD = "INVALID_LENGTH_PASSWORD";
    
    /** Role is not defined */
    public static final String UNDEFINED_ROLE = "UNDEFINED_ROLE";
    
    /** Deposit amount is invalid (must be greater than 0) */
    public static final String INVALID_DEPOSIT = "INVALID_DEPOSIT";
    
    /** Auction ID is invalid */
    public static final String INVALID_AUCTION_ID = "INVALID_AUCTION_ID";

    // ==================== AUTHENTICATION & AUTHORIZATION ====================
    
    /** Account does not exist */
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    
    /** Password is incorrect */
    public static final String WRONG_PASSWORD = "WRONG_PASSWORD";
    
    /** Username already exists */
    public static final String USERNAME_EXISTED = "USERNAME_EXISTED";
    
    /** Email already exists */
    public static final String EMAIL_EXISTED = "EMAIL_EXISTED";
    
    /** Role is invalid */
    public static final String INVALID_ROLE = "INVALID_ROLE";

    // ==================== AUCTION ERRORS (Service) ====================
    
    /** Auction does not exist */
    public static final String AUCTION_NOT_FOUND = "AUCTION_NOT_FOUND";
    
    /** Auction has already closed */
    public static final String AUCTION_CLOSED = "AUCTION_CLOSED";
    
    /** Item does not exist */
    public static final String ITEM_NOT_FOUND = "ITEM_NOT_FOUND";
    
    /** Item type is invalid */
    public static final String INVALID_ITEM_TYPE = "INVALID_ITEM_TYPE";

    // ==================== BID ERRORS (Service) ====================
    
    /** Invalid bid request data */
    public static final String INVALID_BID_REQUEST = "INVALID_BID_REQUEST";
    
    /** AuctionId is missing from request */
    public static final String MISSING_AUCTION_ID = "MISSING_AUCTION_ID";
    
    /** BidderId is missing from request */
    public static final String MISSING_BIDDER_ID = "MISSING_BIDDER_ID";
    
    /** Bid amount is invalid (must be positive) */
    public static final String INVALID_BID_AMOUNT = "INVALID_BID_AMOUNT";
    
    /** Bid increment is invalid */
    public static final String INVALID_INCREMENT = "INVALID_INCREMENT";
    
    /** Seller cannot place bid on their own item */
    public static final String SELLER_CANNOT_BID = "SELLER_CANNOT_BID";
    
    /** User is not a bidder */
    public static final String NOT_BIDDER = "NOT_BIDDER";
    
    /** Insufficient account balance to place bid */
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";

    // ==================== WATCHLIST ERRORS (Controller) ====================
    
    /** Error occurring during watchlist addition or removal */
    public static final String WATCHLIST_ERROR = "WATCHLIST_ERROR";

    // ==================== DATABASE ERRORS (DAO) ====================
    
    /** application.properties configuration file not found */
    public static final String DB_CONFIG_NOT_FOUND = "DB_CONFIG_NOT_FOUND";
    
    /** Database configuration is missing (url/username/password) */
    public static final String DB_CONFIG_MISSING = "DB_CONFIG_MISSING";
    
    /** Failure while reading database configuration */
    public static final String DB_CONFIG_READ_ERROR = "DB_CONFIG_READ_ERROR";
    
    /** Failed to identify or map user role */
    public static final String INVALID_USER_ROLE = "INVALID_USER_ROLE";

    /** Database connection failure (Connection Pool exhaustion or timeout) */
    public static final String CONNECTION_FAILURE = "CONNECTION_FAILURE";

    /** Failure during SQL query execution */
    public static final String SQL_EXECUTION_FAILURE = "SQL_EXECUTION_FAILURE";

    /** Error mapping data from ResultSet to Domain Object */
    public static final String DATA_MAPPING_FAILURE = "DATA_MAPPING_FAILURE";

    /** Database transaction rollback failure */
    public static final String TRANSACTION_ROLLBACK_FAILED = "TRANSACTION_ROLLBACK_FAILED";

    /** Failed to reset database auto-commit state */
    public static final String AUTO_COMMIT_RESET_FAILED = "AUTO_COMMIT_RESET_FAILED";

    /** Failure while closing database resources (ResultSet/Statement) */
    public static final String RESOURCE_CLEANUP_FAILED = "RESOURCE_CLEANUP_FAILED";

    /** Failure while closing the database connection */
    public static final String CONNECTION_CLOSE_FAILED = "CONNECTION_CLOSE_FAILED";

    // ==================== AUTO BID ERRORS (AutoBidService) ====================
    
    /** Auto-bid registration process failed */
    public static final String AUTO_BID_REGISTRATION_FAILED = "AUTO_BID_REGISTRATION_FAILED";
    
    /** Auto bid validation error */
    public static final String AUTO_BID_VALIDATION_ERROR = "AUTO_BID_VALIDATION_ERROR";
    
    /** Sellers are prohibited from registering auto-bids on their own auctions */
    public static final String AUTO_SELLER_CANNOT_AUTOBID = "AUTO_SELLER_CANNOT_AUTOBID";
    
    /** Maximum auto-bid amount is less than the required increment */
    public static final String AUTO_BID_INVALID_RANGE = "AUTO_BID_INVALID_RANGE";

    // ==================== BID ERRORS - ADVANCED ====================
    
    /** Bid amount is lower than the current auction price */
    public static final String BID_LOWER_THAN_CURRENT = "BID_LOWER_THAN_CURRENT";
    
    /** Bid increment does not meet the minimum requirement */
    public static final String INCREMENT_TOO_LOW = "INCREMENT_TOO_LOW";
    
    /** General bid validation failure */
    public static final String INVALID_BID_VALIDATION = "INVALID_BID_VALIDATION";

    // ==================== ITEM ERRORS (ItemService) ====================
    
    /** Persistence failure when saving item entity */
    public static final String ITEM_SAVE_FAILED = "ITEM_SAVE_FAILED";
    
    /** The specified item type is not supported by the system */
    public static final String ITEM_TYPE_UNSUPPORTED = "ITEM_TYPE_UNSUPPORTED";
    
    /** Provided item data is invalid or incomplete */
    public static final String INVALID_ITEM_DATA = "INVALID_ITEM_DATA";

    // ==================== AUCTION ERRORS - ADVANCED ====================
    
    /** Failure during auction creation process */
    public static final String AUCTION_CREATION_FAILED = "AUCTION_CREATION_FAILED";
    
    /** The target auction has not started yet */
    public static final String AUCTION_NOT_STARTED = "AUCTION_NOT_STARTED";
    
    /** Auction creation aborted due to item persistence failure */
    public static final String AUCTION_ITEM_NOT_SAVED = "AUCTION_ITEM_NOT_SAVED";
    
    /** The requested auction is no longer active */
    public static final String AUCTION_NOT_ACTIVE = "AUCTION_NOT_ACTIVE";

    // ==================== NOTIFICATION ERRORS (NotificationService) ====================
    
    /** NotificationService is not properly initialized with its dependencies */
    public static final String NOTIFICATION_SERVICE_NOT_INITIALIZED = "NOTIFICATION_SERVICE_NOT_INITIALIZED";

    /** Failure during notification delivery */
    public static final String NOTIFICATION_FAILED = "NOTIFICATION_FAILED";
    
    /** Target user is currently offline */
    public static final String USER_OFFLINE = "USER_OFFLINE";
    
    /** Active user session not found */
    public static final String USER_SESSION_NOT_FOUND = "USER_SESSION_NOT_FOUND";

    // ==================== ANTI-SNIPING ERRORS (AntiSnipingService) ====================
    
    /** Error occurring during anti-sniping processing */
    public static final String ANTI_SNIPING_ERROR = "ANTI_SNIPING_ERROR";
    
    /** Provided anti-sniping configuration is invalid */
    public static final String INVALID_ANTI_SNIPING_CONFIG = "INVALID_ANTI_SNIPING_CONFIG";

    // ==================== WATCHLIST ERRORS - ADVANCED ====================
    
    /** Auction entity is already present in the user watchlist */
    public static final String AUCTION_ALREADY_IN_WATCHLIST = "AUCTION_ALREADY_IN_WATCHLIST";
    
    /** Auction entity not found in the user watchlist */
    public static final String AUCTION_NOT_IN_WATCHLIST = "AUCTION_NOT_IN_WATCHLIST";

    // ==================== FACTORY ERRORS ====================

    /** Failure during object instantiation within the Factory layer */
    public static final String FACTORY_CREATION_FAILED = "FACTORY_CREATION_FAILED";

    /** Unsupported object type encountered during Factory mapping */
    public static final String UNKNOWN_TYPE_MAPPING = "UNKNOWN_TYPE_MAPPING";


    // ==================== TRANSACTION ERRORS (BidTransactionDAO) ====================
    
    /** Failure while recording bid transaction */
    public static final String BID_TRANSACTION_FAILED = "BID_TRANSACTION_FAILED";
    
    /** Failure while updating account balance */
    public static final String ACCOUNT_BALANCE_UPDATE_FAILED = "ACCOUNT_BALANCE_UPDATE_FAILED";

    // ==================== CONCURRENCY ERRORS (ConcurrentBidManager) ====================
    
    /** Error during concurrent bid task execution */
    public static final String CONCURRENT_BID_PROCESSING_ERROR = "CONCURRENT_BID_PROCESSING_ERROR";
    
    /** Sequential bid worker thread was interrupted */
    public static final String BID_WORKER_INTERRUPTED = "BID_WORKER_INTERRUPTED";

    // ==================== GENERAL ERRORS ====================
    
    /** General persistence failure */
    public static final String SAVE_ERROR = "SAVE_ERROR";
    
    /** Unidentified general system error */
    public static final String GENERAL_ERROR = "GENERAL_ERROR";
    
    /** Requested resource not found */
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    
    /** Provided data is invalid or malformed */
    public static final String INVALID_DATA = "INVALID_DATA";

    /** Unexpected internal server error */
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    /** System-level input/output operation failure */
    public static final String IO_ERROR = "IO_ERROR";

    /** Null value encountered where an object reference was required */
    public static final String NULL_VALUE_ENCOUNTERED = "NULL_VALUE_ENCOUNTERED";

    /** Operation aborted due to invalid system state */
    public static final String ILLEGAL_STATE_ERROR = "ILLEGAL_STATE_ERROR";
}
