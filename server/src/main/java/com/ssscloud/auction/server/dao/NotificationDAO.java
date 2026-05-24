package com.ssscloud.auction.server.dao;

import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.payload.response.DTO.NotificationDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NotificationDAO — CRUD bảng notification.
 * Lưu khi user offline, load khi user login.
 */
public class NotificationDAO extends BaseDAO {
    // Logging Standards: Declared first
    private static final Logger logger = Logger.getLogger(NotificationDAO.class.getName());

    /** Lưu 1 notification vào DB. */
    public boolean save(NotificationDTO notificationDto) throws DAOException, Exception {
        String sql = "INSERT INTO notification (id, user_id, type, auction_id, auction_name, price, winner, is_read, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, FALSE, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String id = UUID.randomUUID().toString();
            ps.setString(1, id);
            ps.setString(2, notificationDto.getUserId());
            ps.setString(3, notificationDto.getType());
            ps.setString(4, notificationDto.getAuctionId());
            ps.setString(5, notificationDto.getAuctionName());
            ps.setLong  (6, notificationDto.getPrice());
            ps.setString(7, notificationDto.getWinner());
            ps.setObject(8, notificationDto.getCreatedAt() != null ? notificationDto.getCreatedAt() : LocalDateTime.now());

            return ps.executeUpdate() > 0;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.NOTIFICATION_SAVE_FAILED, "Database interaction failure while saving notification.", sqlException);
        } catch (Exception exception) {
            throw exception;
        }
    }

    /** Lấy tất cả notification chưa đọc của user — gọi ngay sau login. */
    public List<NotificationDTO> findUnreadByUserId(String userId) throws DAOException, Exception {
        String sql = "SELECT id, type, auction_id, auction_name, price, winner, is_read, created_at "
                   + "FROM notification WHERE user_id = ? AND is_read = FALSE "
                   + "ORDER BY created_at DESC";
        List<NotificationDTO> unreadNotificationsList = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                NotificationDTO notificationDto = new NotificationDTO();
                notificationDto.setId         (rs.getString("id"));
                notificationDto.setType       (rs.getString("type"));
                notificationDto.setAuctionId  (rs.getString("auction_id"));
                notificationDto.setAuctionName(rs.getString("auction_name"));
                notificationDto.setPrice      (rs.getLong  ("price"));
                notificationDto.setWinner     (rs.getString("winner"));
                notificationDto.setRead       (rs.getBoolean("is_read"));
                notificationDto.setCreatedAt  (rs.getObject("created_at", LocalDateTime.class));
                unreadNotificationsList.add(notificationDto);
            }
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.NOTIFICATION_FETCH_FAILED, "Database interaction failure while retrieving notifications.", sqlException);
        } catch (Exception exception) {
            throw exception;
        }
        return unreadNotificationsList;
    }

    /** Đánh dấu đã đọc 1 notification. */
    public void markRead(String notificationId) throws DAOException, Exception {
        String sql = "UPDATE notification SET is_read = TRUE WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.NOTIFICATION_UPDATE_FAILED, "Database interaction failure while updating notification read status.", sqlException);
        } catch (Exception exception) {
            throw exception;
        }
    }

    /** Đánh dấu đã đọc toàn bộ của user. */
    public void markAllRead(String userId) throws DAOException, Exception {
        String sql = "UPDATE notification SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.NOTIFICATION_UPDATE_FAILED, "Database interaction failure while marking all notifications as read.", sqlException);
        } catch (Exception exception) {
            throw exception;
        }
    }
}