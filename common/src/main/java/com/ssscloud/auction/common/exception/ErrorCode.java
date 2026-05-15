package com.ssscloud.auction.common.exception;

/**
 * Quản lý tập trung toàn bộ error code của hệ thống.
 * Giúp tránh lỗi typo khi gõ error code ở khắp nơi trong code.
 * Sử dụng auto-complete IDE để lấy error code an toàn.
 */
public class ErrorCode {

    // ==================== VALIDATION ERRORS (Controller) ====================
    
    /** Lỗi request login không hợp lệ */
    public static final String INVALID_LOGIN_REQUEST = "INVALID_LOGIN_REQUEST";
    
    /** Lỗi request register không hợp lệ */
    public static final String INVALID_REGISTER_REQUEST = "INVALID_REGISTER_REQUEST";
    
    /** Lỗi username không hợp lệ (null hoặc empty) */
    public static final String INVALID_USERNAME = "INVALID_USERNAME";
    
    /** Lỗi password không hợp lệ (null hoặc empty) */
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
    
    /** Lỗi email không hợp lệ (null hoặc empty) */
    public static final String INVALID_EMAIL = "INVALID_EMAIL";
    
    /** Lỗi độ dài username không đúng */
    public static final String INVALID_LENGTH_USERNAME = "INVALID_LENGTH_USERNAME";
    
    /** Lỗi độ dài password không đúng */
    public static final String INVALID_LENGTH_PASSWORD = "INVALID_LENGTH_PASSWORD";
    
    /** Lỗi role không được định nghĩa */
    public static final String UNDEFINED_ROLE = "UNDEFINED_ROLE";
    
    /** Lỗi số tiền deposit không hợp lệ (phải > 0) */
    public static final String INVALID_DEPOSIT = "INVALID_DEPOSIT";
    
    /** Lỗi ID auction không hợp lệ */
    public static final String INVALID_AUCTION_ID = "INVALID_AUCTION_ID";

    // ==================== AUTHENTICATION & AUTHORIZATION ====================
    
    /** Tài khoản không tồn tại */
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    
    /** Mật khẩu không chính xác */
    public static final String WRONG_PASSWORD = "WRONG_PASSWORD";
    
    /** Username đã tồn tại */
    public static final String USERNAME_EXISTED = "USERNAME_EXISTED";
    
    /** Email đã tồn tại */
    public static final String EMAIL_EXISTED = "EMAIL_EXISTED";
    
    /** Role không hợp lệ */
    public static final String INVALID_ROLE = "INVALID_ROLE";

    // ==================== AUCTION ERRORS (Service) ====================
    
    /** Phiên đấu giá không tồn tại */
    public static final String AUCTION_NOT_FOUND = "AUCTION_NOT_FOUND";
    
    /** Phiên đấu giá đã kết thúc */
    public static final String AUCTION_CLOSED = "AUCTION_CLOSED";
    
    /** Sản phẩm không tồn tại */
    public static final String ITEM_NOT_FOUND = "ITEM_NOT_FOUND";
    
    /** Loại sản phẩm không hợp lệ */
    public static final String INVALID_ITEM_TYPE = "INVALID_ITEM_TYPE";

    // ==================== BID ERRORS (Service) ====================
    
    /** Lỗi request bid không hợp lệ */
    public static final String INVALID_BID_REQUEST = "INVALID_BID_REQUEST";
    
    /** Thiếu auctionId trong request */
    public static final String MISSING_AUCTION_ID = "MISSING_AUCTION_ID";
    
    /** Thiếu bidderId trong request */
    public static final String MISSING_BIDDER_ID = "MISSING_BIDDER_ID";
    
    /** Giá bid không hợp lệ (phải dương) */
    public static final String INVALID_BID_AMOUNT = "INVALID_BID_AMOUNT";
    
    /** Mức tăng giá không hợp lệ */
    public static final String INVALID_INCREMENT = "INVALID_INCREMENT";
    
    /** Người bán không thể đấu giá sản phẩm của mình */
    public static final String SELLER_CANNOT_BID = "SELLER_CANNOT_BID";
    
    /** Người dùng không phải bidder */
    public static final String NOT_BIDDER = "NOT_BIDDER";
    
    /** Số dư tài khoản không đủ để đặt giá */
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";

    // ==================== WATCHLIST ERRORS (Controller) ====================
    
    /** Lỗi thêm/xóa khỏi watchlist */
    public static final String WATCHLIST_ERROR = "WATCHLIST_ERROR";

    public static final String DATA_CONFLICT = "DATA_CONFLICT";

    // ==================== DATABASE ERRORS (DAO) ====================
    
    /** File application.properties không tìm thấy */
    public static final String DB_CONFIG_NOT_FOUND = "DB_CONFIG_NOT_FOUND";
    
    /** Cấu hình database bị thiếu (url/username/password) */
    public static final String DB_CONFIG_MISSING = "DB_CONFIG_MISSING";
    
    /** Lỗi đọc cấu hình database */
    public static final String DB_CONFIG_READ_ERROR = "DB_CONFIG_READ_ERROR";
    
    /** Không thể xác định user role */
    public static final String INVALID_USER_ROLE = "INVALID_USER_ROLE";

    /** Lỗi kết nối database (Connection Pool failure) */
    public static final String CONNECTION_FAILURE = "CONNECTION_FAILURE";

    /** Lỗi thực thi truy vấn SQL */
    public static final String SQL_EXECUTION_FAILURE = "SQL_EXECUTION_FAILURE";

    /** Lỗi chuyển đổi dữ liệu từ ResultSet sang Object */
    public static final String DATA_MAPPING_FAILURE = "DATA_MAPPING_FAILURE";

    /** Lỗi rollback transaction */
    public static final String TRANSACTION_ROLLBACK_FAILED = "TRANSACTION_ROLLBACK_FAILED";

    /** Lỗi reset trạng thái auto-commit */
    public static final String AUTO_COMMIT_RESET_FAILED = "AUTO_COMMIT_RESET_FAILED";

    /** Lỗi đóng tài nguyên database (ResultSet/Statement) */
    public static final String RESOURCE_CLEANUP_FAILED = "RESOURCE_CLEANUP_FAILED";

    /** Lỗi đóng kết nối database */
    public static final String CONNECTION_CLOSE_FAILED = "CONNECTION_CLOSE_FAILED";

    public static final String INTERNAL_DB_ERROR = "INTERNAL_DB_ERROR";

    // ==================== AUTO BID ERRORS (AutoBidService) ====================
    
    /** Đăng ký auto bid thất bại */
    public static final String AUTO_BID_REGISTRATION_FAILED = "AUTO_BID_REGISTRATION_FAILED";
    
    /** Auto bid validation error */
    public static final String AUTO_BID_VALIDATION_ERROR = "AUTO_BID_VALIDATION_ERROR";
    
    /** Người bán không thể đăng ký auto bid */
    public static final String AUTO_SELLER_CANNOT_AUTOBID = "AUTO_SELLER_CANNOT_AUTOBID";
    
    /** Auto bid tối đa nhỏ hơn mức tăng giá */
    public static final String AUTO_BID_INVALID_RANGE = "AUTO_BID_INVALID_RANGE";

    // ==================== BID ERRORS - ADVANCED ====================
    
    /** Giá đặt thấp hơn giá hiện tại */
    public static final String BID_LOWER_THAN_CURRENT = "BID_LOWER_THAN_CURRENT";
    
    /** Mức tăng giá quá thấp */
    public static final String INCREMENT_TOO_LOW = "INCREMENT_TOO_LOW";
    
    /** Giá đặt không hợp lệ */
    public static final String INVALID_BID_VALIDATION = "INVALID_BID_VALIDATION";

    // ==================== ITEM ERRORS (ItemService) ====================
    
    /** Lỗi lưu item vào database */
    public static final String ITEM_SAVE_FAILED = "ITEM_SAVE_FAILED";
    
    /** Loại item không được hỗ trợ */
    public static final String ITEM_TYPE_UNSUPPORTED = "ITEM_TYPE_UNSUPPORTED";
    
    /** Dữ liệu item không hợp lệ */
    public static final String INVALID_ITEM_DATA = "INVALID_ITEM_DATA";

    // ==================== AUCTION ERRORS - ADVANCED ====================
    
    /** Lỗi tạo phiên đấu giá */
    public static final String AUCTION_CREATION_FAILED = "AUCTION_CREATION_FAILED";
    
    /** Phiên đấu giá chưa bắt đầu */
    public static final String AUCTION_NOT_STARTED = "AUCTION_NOT_STARTED";
    
    /** Item lưu thất bại, phiên đấu giá không được tạo */
    public static final String AUCTION_ITEM_NOT_SAVED = "AUCTION_ITEM_NOT_SAVED";
    
    /** Phiên đấu giá được hỗ trợ không còn hoạt động */
    public static final String AUCTION_NOT_ACTIVE = "AUCTION_NOT_ACTIVE";

    // ==================== NOTIFICATION ERRORS (NotificationService) ====================
    
    /** Lỗi gửi thông báo */
    public static final String NOTIFICATION_FAILED = "NOTIFICATION_FAILED";
    
    /** Người dùng không online */
    public static final String USER_OFFLINE = "USER_OFFLINE";
    
    /** Session người dùng không tồn tại */
    public static final String USER_SESSION_NOT_FOUND = "USER_SESSION_NOT_FOUND";

    // ==================== ANTI-SNIPING ERRORS (AntiSnipingService) ====================
    
    /** Lỗi xử lý anti-sniping */
    public static final String ANTI_SNIPING_ERROR = "ANTI_SNIPING_ERROR";
    
    /** Cấu hình anti-sniping không hợp lệ */
    public static final String INVALID_ANTI_SNIPING_CONFIG = "INVALID_ANTI_SNIPING_CONFIG";

    // ==================== WATCHLIST ERRORS - ADVANCED ====================
    
    /** Auction đã trong watchlist */
    public static final String AUCTION_ALREADY_IN_WATCHLIST = "AUCTION_ALREADY_IN_WATCHLIST";
    
    /** Auction không trong watchlist */
    public static final String AUCTION_NOT_IN_WATCHLIST = "AUCTION_NOT_IN_WATCHLIST";

    // ==================== FACTORY ERRORS ====================

    /** Lỗi khởi tạo đối tượng trong Factory */
    public static final String FACTORY_CREATION_FAILED = "FACTORY_CREATION_FAILED";

    /** Loại đối tượng không được hỗ trợ trong Factory mapping */
    public static final String UNKNOWN_TYPE_MAPPING = "UNKNOWN_TYPE_MAPPING";


    // ==================== TRANSACTION ERRORS (BidTransactionDAO) ====================
    
    /** Lỗi lưu giao dịch bid */
    public static final String BID_TRANSACTION_FAILED = "BID_TRANSACTION_FAILED";
    
    /** Lỗi cập nhật số dư tài khoản */
    public static final String ACCOUNT_BALANCE_UPDATE_FAILED = "ACCOUNT_BALANCE_UPDATE_FAILED";

    // ==================== CONCURRENCY ERRORS (ConcurrentBidManager) ====================
    
    /** Lỗi xử lý bid đồng thời */
    public static final String CONCURRENT_BID_PROCESSING_ERROR = "CONCURRENT_BID_PROCESSING_ERROR";
    
    /** Worker bid bị gián đoạn */
    public static final String BID_WORKER_INTERRUPTED = "BID_WORKER_INTERRUPTED";

    // ==================== GENERAL ERRORS ====================
    
    /** Lỗi lưu dữ liệu vào database */
    public static final String SAVE_ERROR = "SAVE_ERROR";
    
    /** Lỗi chung không xác định */
    public static final String GENERAL_ERROR = "GENERAL_ERROR";
    
    /** Tài nguyên không tìm thấy */
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    
    /** Dữ liệu không hợp lệ */
    public static final String INVALID_DATA = "INVALID_DATA";
}
