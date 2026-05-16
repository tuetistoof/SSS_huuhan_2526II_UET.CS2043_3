package com.ssscloud.auction.server.dao;

import com.ssscloud.auction.common.dto.response.NotificationDTO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * NotificationDAO — CRUD bảng notification.
 * Lưu khi user offline, load khi user login.
 */
public class NotificationDAO extends BaseDAO {

    /** Lưu 1 notification vào DB. */
    public boolean save(NotificationDTO dto) {
        String sql = "INSERT INTO notification (id, user_id, type, auction_id, auction_name, price, winner, is_read, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, FALSE, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String id = UUID.randomUUID().toString();
            ps.setString(1, id);
            ps.setString(2, dto.getUserId());
            ps.setString(3, dto.getType());
            ps.setString(4, dto.getAuctionId());
            ps.setString(5, dto.getAuctionName());
            ps.setLong  (6, dto.getPrice());
            ps.setString(7, dto.getWinner());
            ps.setObject(8, dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "NotificationDAO.save lỗi: " + e.getMessage(), e);
            return false;
        }
    }

    /** Lấy tất cả notification chưa đọc của user — gọi ngay sau login. */
    public List<NotificationDTO> findUnreadByUserId(String userId) {
        String sql = "SELECT id, type, auction_id, auction_name, price, winner, is_read, created_at "
                   + "FROM notification WHERE user_id = ? AND is_read = FALSE "
                   + "ORDER BY created_at DESC";
        List<NotificationDTO> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                NotificationDTO dto = new NotificationDTO();
                dto.setId         (rs.getString("id"));
                dto.setType       (rs.getString("type"));
                dto.setAuctionId  (rs.getString("auction_id"));
                dto.setAuctionName(rs.getString("auction_name"));
                dto.setPrice      (rs.getLong  ("price"));
                dto.setWinner     (rs.getString("winner"));
                dto.setRead       (rs.getBoolean("is_read"));
                dto.setCreatedAt  (rs.getObject("created_at", LocalDateTime.class));
                list.add(dto);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "NotificationDAO.findUnread lỗi: " + e.getMessage(), e);
        }
        return list;
    }

    /** Đánh dấu đã đọc 1 notification. */
    public void markRead(String notificationId) {
        String sql = "UPDATE notification SET is_read = TRUE WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, notificationId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.log(Level.WARNING, "NotificationDAO.markRead lỗi: " + e.getMessage(), e);
        }
    }

    /** Đánh dấu đã đọc toàn bộ của user. */
    public void markAllRead(String userId) {
        String sql = "UPDATE notification SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.log(Level.WARNING, "NotificationDAO.markAllRead lỗi: " + e.getMessage(), e);
        }
    }
}