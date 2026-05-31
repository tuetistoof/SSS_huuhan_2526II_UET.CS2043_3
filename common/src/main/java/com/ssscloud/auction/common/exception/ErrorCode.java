package com.ssscloud.auction.common.exception;


public class ErrorCode {

  // ==================== VALIDATION ERRORS (Controller) ====================

  public static final String INVALID_LOGIN_REQUEST = "INVALID_LOGIN_REQUEST";
  public static final String INVALID_REGISTER_REQUEST = "INVALID_REGISTER_REQUEST";
  public static final String INVALID_USERNAME = "INVALID_USERNAME";
  public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
  public static final String INVALID_EMAIL = "INVALID_EMAIL";
  public static final String INVALID_LENGTH_USERNAME = "INVALID_LENGTH_USERNAME";
  public static final String INVALID_LENGTH_PASSWORD = "INVALID_LENGTH_PASSWORD";
  public static final String UNDEFINED_ROLE = "UNDEFINED_ROLE";
  public static final String INVALID_DEPOSIT = "INVALID_DEPOSIT";
  public static final String INVALID_AUCTION_ID = "INVALID_AUCTION_ID";

  // ==================== AUTHENTICATION & AUTHORIZATION ====================

  public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
  public static final String WRONG_PASSWORD = "WRONG_PASSWORD";
  public static final String USERNAME_EXISTED = "USERNAME_EXISTED";
  public static final String EMAIL_EXISTED = "EMAIL_EXISTED";
  public static final String INVALID_ROLE = "INVALID_ROLE";

  // ==================== AUCTION ERRORS (Service) ====================

  public static final String AUCTION_NOT_FOUND = "AUCTION_NOT_FOUND";
  public static final String AUCTION_CLOSED = "AUCTION_CLOSED";
  public static final String ITEM_NOT_FOUND = "ITEM_NOT_FOUND";
  public static final String INVALID_ITEM_TYPE = "INVALID_ITEM_TYPE";

  // ==================== BID ERRORS (Service) ====================

  public static final String INVALID_BID_REQUEST = "INVALID_BID_REQUEST";
  public static final String MISSING_AUCTION_ID = "MISSING_AUCTION_ID";
  public static final String MISSING_BIDDER_ID = "MISSING_BIDDER_ID";
  public static final String INVALID_BID_AMOUNT = "INVALID_BID_AMOUNT";
  public static final String INVALID_INCREMENT = "INVALID_INCREMENT";
  public static final String SELLER_CANNOT_BID = "SELLER_CANNOT_BID";
  public static final String NOT_BIDDER = "NOT_BIDDER";
  public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";

  // ==================== WATCHLIST ERRORS (Controller) ====================

  public static final String WATCHLIST_ERROR = "WATCHLIST_ERROR";
  public static final String DATA_CONFLICT = "DATA_CONFLICT";

  // ==================== DATABASE ERRORS (DAO) ====================

  public static final String DATABASE_ERROR = "DATABASE_ERROR";

  public static final String USER_PERSISTENCE_FAILED = "USER_PERSISTENCE_FAILED";
  public static final String USER_RETRIEVAL_FAILED = "USER_RETRIEVAL_FAILED";
  public static final String USER_MODIFICATION_FAILED = "USER_MODIFICATION_FAILED";
  public static final String ITEM_FETCH_FAILED = "ITEM_FETCH_FAILED";
  public static final String WATCHLIST_QUERY_FAILED = "WATCHLIST_QUERY_FAILED";
  public static final String WATCHLIST_DETAILS_RETRIEVAL_FAILED =
      "WATCHLIST_DETAILS_RETRIEVAL_FAILED";
  public static final String WATCHLIST_ADD_FAILED = "WATCHLIST_ADD_FAILED";
  public static final String WATCHLIST_REMOVE_FAILED = "WATCHLIST_REMOVE_FAILED";
  public static final String WATCHLIST_STATUS_CHECK_FAILED = "WATCHLIST_STATUS_CHECK_FAILED";
  public static final String WON_ITEMS_DETAILS_RETRIEVAL_FAILED =
      "WON_ITEMS_DETAILS_RETRIEVAL_FAILED";
  public static final String WATCHLIST_WATCHER_FETCH_FAILED = "WATCHLIST_WATCHER_FETCH_FAILED";

  public static final String NOTIFICATION_SAVE_FAILED = "NOTIFICATION_SAVE_FAILED";
  public static final String NOTIFICATION_FETCH_FAILED = "NOTIFICATION_FETCH_FAILED";
  public static final String NOTIFICATION_UPDATE_FAILED = "NOTIFICATION_UPDATE_FAILED";
  public static final String NOTIFICATION_PERSISTENCE_FAILED = "NOTIFICATION_PERSISTENCE_FAILED";

  public static final String BID_HISTORY_FETCH_FAILED = "BID_HISTORY_FETCH_FAILED";
  public static final String BIDDED_AUCTIONS_FETCH_FAILED = "BIDDED_AUCTIONS_FETCH_FAILED";
  public static final String SELLER_AUCTION_FETCH_FAILED = "SELLER_AUCTION_FETCH_FAILED";
  public static final String ACTIVE_AUCTION_FETCH_FAILED = "ACTIVE_AUCTION_FETCH_FAILED";
  public static final String ITEM_DELETE_FAILED = "ITEM_DELETE_FAILED";
  public static final String ITEM_UPDATE_FAILED = "ITEM_UPDATE_FAILED";
  public static final String BID_SAVE_FAILED = "BID_SAVE_FAILED";
  public static final String BID_FETCH_FAILED = "BID_FETCH_FAILED";

  public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";
  public static final String AUCTION_SAVE_FAILED = "AUCTION_SAVE_FAILED";
  public static final String AUCTION_FETCH_FAILED = "AUCTION_FETCH_FAILED";
  public static final String AUCTION_UPDATE_FAILED = "AUCTION_UPDATE_FAILED";
  public static final String AUCTION_DELETE_FAILED = "AUCTION_DELETE_FAILED";
  public static final String DB_CONFIG_NOT_FOUND = "DB_CONFIG_NOT_FOUND";
public static final String DB_CONFIG_MISSING = "DB_CONFIG_MISSING";

  public static final String DB_CONFIG_READ_ERROR = "DB_CONFIG_READ_ERROR";
  public static final String INVALID_USER_ROLE = "INVALID_USER_ROLE";
  public static final String CONNECTION_FAILURE = "CONNECTION_FAILURE";
  public static final String SQL_EXECUTION_FAILURE = "SQL_EXECUTION_FAILURE";
  public static final String DATA_MAPPING_FAILURE = "DATA_MAPPING_FAILURE";
  public static final String TRANSACTION_ROLLBACK_FAILED = "TRANSACTION_ROLLBACK_FAILED";
  public static final String AUTO_COMMIT_RESET_FAILED = "AUTO_COMMIT_RESET_FAILED";
  public static final String RESOURCE_CLEANUP_FAILED = "RESOURCE_CLEANUP_FAILED";
  public static final String CONNECTION_CLOSE_FAILED = "CONNECTION_CLOSE_FAILED";

  public static final String INTERNAL_DB_ERROR = "INTERNAL_DB_ERROR";
  public static final String SYSTEM_FAILURE = "SYSTEM_FAILURE";

  // ==================== AUTO BID ERRORS (AutoBidService) ====================

  public static final String AUTO_BID_REGISTRATION_FAILED = "AUTO_BID_REGISTRATION_FAILED";
  public static final String AUTO_BID_VALIDATION_ERROR = "AUTO_BID_VALIDATION_ERROR";
  public static final String AUTO_SELLER_CANNOT_AUTOBID = "AUTO_SELLER_CANNOT_AUTOBID";
  public static final String AUTO_BID_INVALID_RANGE = "AUTO_BID_INVALID_RANGE";

  // ==================== BID ERRORS - ADVANCED ====================

  public static final String BID_LOWER_THAN_CURRENT = "BID_LOWER_THAN_CURRENT";
  public static final String INCREMENT_TOO_LOW = "INCREMENT_TOO_LOW";
  public static final String INVALID_BID_VALIDATION = "INVALID_BID_VALIDATION";

  // ==================== ITEM ERRORS (ItemService) ====================

  public static final String ITEM_SAVE_FAILED = "ITEM_SAVE_FAILED";
  public static final String ITEM_TYPE_UNSUPPORTED = "ITEM_TYPE_UNSUPPORTED";
  public static final String INVALID_ITEM_DATA = "INVALID_ITEM_DATA";

  // ==================== AUCTION ERRORS - ADVANCED ====================

  public static final String AUCTION_CREATION_FAILED = "AUCTION_CREATION_FAILED";
  public static final String AUCTION_NOT_STARTED = "AUCTION_NOT_STARTED";
  public static final String AUCTION_ITEM_NOT_SAVED = "AUCTION_ITEM_NOT_SAVED";
  public static final String AUCTION_NOT_ACTIVE = "AUCTION_NOT_ACTIVE";

  // ==================== NOTIFICATION ERRORS (NotificationService) ====================
  public static final String NOTIFICATION_SERVICE_NOT_INITIALIZED = "NOTIFICATION_SERVICE_NOT_INITIALIZED";
  public static final String NOTIFICATION_FAILED = "NOTIFICATION_FAILED";
  public static final String USER_OFFLINE = "USER_OFFLINE";
  public static final String USER_SESSION_NOT_FOUND = "USER_SESSION_NOT_FOUND";

  // ==================== ANTI-SNIPING ERRORS (AntiSnipingService) ====================

  public static final String ANTI_SNIPING_ERROR = "ANTI_SNIPING_ERROR";
  public static final String INVALID_ANTI_SNIPING_CONFIG = "INVALID_ANTI_SNIPING_CONFIG";

  // ==================== WATCHLIST ERRORS - ADVANCED ====================

  public static final String AUCTION_ALREADY_IN_WATCHLIST = "AUCTION_ALREADY_IN_WATCHLIST";
  public static final String AUCTION_NOT_IN_WATCHLIST = "AUCTION_NOT_IN_WATCHLIST";

  // ==================== FACTORY ERRORS ====================

  public static final String FACTORY_CREATION_FAILED = "FACTORY_CREATION_FAILED";
  public static final String UNKNOWN_TYPE_MAPPING = "UNKNOWN_TYPE_MAPPING";

  // ==================== TRANSACTION ERRORS (BidTransactionDAO) ====================

  public static final String BID_TRANSACTION_FAILED = "BID_TRANSACTION_FAILED";
  public static final String ACCOUNT_BALANCE_UPDATE_FAILED = "ACCOUNT_BALANCE_UPDATE_FAILED";

  // ==================== CONCURRENCY ERRORS (ConcurrentBidManager) ====================

  public static final String CONCURRENT_BID_PROCESSING_ERROR = "CONCURRENT_BID_PROCESSING_ERROR";
  public static final String BID_WORKER_INTERRUPTED = "BID_WORKER_INTERRUPTED";

  // ==================== GENERAL ERRORS ====================

  public static final String SAVE_ERROR = "SAVE_ERROR";
  public static final String GENERAL_ERROR = "GENERAL_ERROR";
  public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
  public static final String INVALID_DATA = "INVALID_DATA";
  public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
  public static final String IO_ERROR = "IO_ERROR";
  public static final String NULL_VALUE_ENCOUNTERED = "NULL_VALUE_ENCOUNTERED";
  public static final String ILLEGAL_STATE_ERROR = "ILLEGAL_STATE_ERROR";
}
