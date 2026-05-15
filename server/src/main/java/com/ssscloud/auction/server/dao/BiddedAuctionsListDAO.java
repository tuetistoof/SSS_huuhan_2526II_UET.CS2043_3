package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.ssscloud.auction.common.dto.response.AuctionDisplayInfoDTO;

public class BiddedAuctionsListDAO extends BaseDAO{
    // ── Query ─────────────────────────────────────────────────────────────────
    public List<AuctionDisplayInfoDTO> findBiddedAuctionsDetailsByUser(String userId) {
        List<AuctionDisplayInfoDTO> list = new ArrayList<>();

        String sql =
                    "SELECT a.id, " +
            "       e.name                                              AS auction_name, " +
            "       ac.end_time, " +
            "       u.username                                          AS seller_username, " +
            "       ei.name                                             AS item_name, " +
            "       i.type                                              AS item_type, " +
            "       COALESCE(last_bid.bid_amount, ac.start_price)       AS current_price, " +
            "       GROUP_CONCAT(DISTINCT img.image_url SEPARATOR ', ') AS image_url, " +
            "       my_bid.bid_amount                                   AS my_last_bid, " +
            "       (my_bid.bid_amount = COALESCE(last_bid.bid_amount, ac.start_price)) AS is_leading " +
            "FROM auction a " +
            "JOIN bid_transaction bt_user ON bt_user.auction_id = a.id AND bt_user.bidder_id = ? " +
            "JOIN auction_config ac       ON a.id = ac.id " +
            "JOIN entity e                ON a.id = e.id " +
            "JOIN user u                  ON a.seller_id = u.id " +
            "JOIN item i                  ON a.item_id = i.id " +
            "JOIN entity ei               ON i.id = ei.id " +
            "LEFT JOIN item_image_url img ON a.item_id = img.item_id " +
            
            // Lấy bid cao nhất hiện tại
            "LEFT JOIN ( " +
            "    SELECT auction_id, MAX(bid_amount) AS bid_amount " +
            "    FROM bid_transaction " +
            "    WHERE (auction_id, bid_time) IN ( " +
            "        SELECT auction_id, MAX(bid_time) " +
            "        FROM bid_transaction " +
            "        GROUP BY auction_id " +
            "    ) " +
            "    GROUP BY auction_id " +
            ") AS last_bid ON last_bid.auction_id = a.id " +
            
            // Lấy bid mới nhất của user đang xem
            "LEFT JOIN ( " +
            "    SELECT auction_id, MAX(bid_amount) AS bid_amount " +
            "    FROM bid_transaction " +
            "    WHERE bidder_id = ? " +
            "      AND (auction_id, bid_time) IN ( " +
            "          SELECT auction_id, MAX(bid_time) " +
            "          FROM bid_transaction " +
            "          WHERE bidder_id = ? " +
            "          GROUP BY auction_id " +
            "      ) " +
            "    GROUP BY auction_id " +
            ") AS my_bid ON my_bid.auction_id = a.id " +
            
            "WHERE a.status IN ('OPEN', 'RUNNING') " +
            
            // Fix Bug 1: thêm các cột cần thiết vào GROUP BY
            "GROUP BY a.id, e.name, ac.end_time, u.username, ei.name, i.type, " +
            "         ac.start_price, last_bid.bid_amount, my_bid.bid_amount";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userId);  // bt_user.bidder_id
            ps.setString(2, userId);  // b3.bidder_id
            ps.setString(3, userId);  // b4.bidder_id
            rs = ps.executeQuery();

            while (rs.next()) {
                AuctionDisplayInfoDTO dto = new AuctionDisplayInfoDTO();
                dto.setId(rs.getString("id"));
                dto.setAuctionName(rs.getString("auction_name"));
                dto.setItemName(rs.getString("item_name"));
                dto.setItemType(rs.getString("item_type"));
                dto.setCurrentPrice(rs.getLong("current_price"));
                dto.setSellerUsername(rs.getString("seller_username"));
                dto.setMyLastBid(rs.getLong("my_last_bid"));
                dto.setLeading(rs.getBoolean("is_leading"));

                Timestamp ts = rs.getTimestamp("end_time");
                if (ts != null) dto.setEndTime(ts.toLocalDateTime());

                String imgs = rs.getString("image_url");
                if (imgs != null && !imgs.isEmpty()) {
                    dto.setImageUrl(List.of(imgs.split(", ")));
                }

                list.add(dto);
            }
            logger.info("[BiddedAuction] Loaded " + list.size() + " items for user " + userId);
        } catch (SQLException e) {
            logger.severe("Lỗi SQL BiddedAuction: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Lỗi kết nối BiddedAuction: " + e.getMessage());
        } finally {
            closeResource(rs, ps);
            closeConnect(conn);
        }
        return list;
    }
}
