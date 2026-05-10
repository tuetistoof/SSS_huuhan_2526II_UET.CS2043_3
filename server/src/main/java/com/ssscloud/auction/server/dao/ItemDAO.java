package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

import com.ssscloud.auction.common.model.Art;
import com.ssscloud.auction.common.model.Electronic;
import com.ssscloud.auction.common.model.Vehicle;
import com.ssscloud.auction.common.model.base.Item;

public class ItemDAO extends BaseDAO {
    public boolean saveElectronic(Electronic electronic) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlItem = "INSERT INTO item (id, seller_id, creator, description, type) VALUES (?, ?, ?, ?, ?)";
        String sqlItemImageUrl = "INSERT INTO item_image_url (item_id, image_url) VALUES (?,?)";
        String sqlElectronic = "INSERT INTO electronic (id, is_repaired, warranty_period) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement psEntity = null, psItem = null, psItemImageUrl = null, psElectronic = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString(1, electronic.getId());
            psEntity.setString(2, electronic.getName());
            psEntity.executeUpdate();

            psItem = conn.prepareStatement(sqlItem);
            psItem.setString(1, electronic.getId());
            psItem.setString(2, electronic.getSellerId());
            psItem.setString(3, electronic.getCreator());
            psItem.setString(4, electronic.getDescription());
            psItem.setString(5, electronic.getType());
            psItem.executeUpdate();

            psItemImageUrl = conn.prepareStatement(sqlItemImageUrl);
            for (String url : electronic.getImageUrl()) {
                psItemImageUrl.setString(1, electronic.getId());
                psItemImageUrl.setString(2, url);
                psItemImageUrl.addBatch();
            }
            psItemImageUrl.executeBatch();

            psElectronic = conn.prepareStatement(sqlElectronic);
            psElectronic.setString(1, electronic.getId());
            psElectronic.setBoolean(2, electronic.getIsRepaired());
            psElectronic.setInt(3, electronic.getWarrantyPeriod());
            psElectronic.executeUpdate();

            conn.commit();
            logger.info("da luu electronic: " + electronic.getId() + " - " + electronic.getName());
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("saveElectronic vi pham rang buoc (co the do id item trung hoac sellerid khong ton tai): "
                    + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi saveElectronic [" + electronic.getName() + "]: " + e.getMessage());
            return false;
        } finally {
            resetAutocommit(conn);
            closeConnect(conn);
            closeResource(psEntity, psItem, psItemImageUrl, psElectronic);
        }
    }

    public boolean saveVehicle(Vehicle vehicle) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlItem = "INSERT INTO item (id, seller_id, creator, description, type) VALUES (?, ?, ?, ?, ?)";
        String sqlItemImageUrl = "INSERT INTO item_image_url (item_id, image_url) VALUES (?,?)";
        String sqlVehicle = "INSERT INTO vehicle (id, is_repaired, warranty_period) VALUES (?, ?, ?)";

        Connection conn = null;
        PreparedStatement psEntity = null, psItem = null, psItemImageUrl = null, psVehicle = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString(1, vehicle.getId());
            psEntity.setString(2, vehicle.getName());
            psEntity.executeUpdate();

            psItem = conn.prepareStatement(sqlItem);
            psItem.setString(1, vehicle.getId());
            psItem.setString(2, vehicle.getSellerId());
            psItem.setString(3, vehicle.getCreator());
            psItem.setString(4, vehicle.getDescription());
            psItem.setString(5, vehicle.getType());
            psItem.executeUpdate();

            psItemImageUrl = conn.prepareStatement(sqlItemImageUrl);
            for (String url : vehicle.getImageUrl()) {
                psItemImageUrl.setString(1, vehicle.getId());
                psItemImageUrl.setString(2, url);
                psItemImageUrl.addBatch();
            }
            psItemImageUrl.executeBatch();

            psVehicle = conn.prepareStatement(sqlVehicle);
            psVehicle.setString(1, vehicle.getId());
            psVehicle.setBoolean(2, vehicle.getIsRepaired());
            psVehicle.setInt(3, vehicle.getWarrantyPeriod());
            psVehicle.executeUpdate();

            conn.commit();
            logger.info("da luu Vehicle: " + vehicle.getId() + " - " + vehicle.getName());
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("saveVehicle vi pham rang buoc (co the do id item trung hoac sellerid khong ton tai): "
                    + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi saveVehicle [" + vehicle.getName() + "]: " + e.getMessage());
            return false;
        } finally {
            resetAutocommit(conn);
            closeConnect(conn);
            closeResource(psEntity, psItem, psItemImageUrl, psVehicle);
        }
    }

    public boolean saveArt(Art art) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlItem = "INSERT INTO item (id, seller_id, creator, description, type) VALUES (?, ?, ?, ?, ?)";
        String sqlItemImageUrl = "INSERT INTO item_image_url (item_id, image_url) VALUES (?,?)";
        String sqlArt = "INSERT INTO art (id, certificate) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement psEntity = null, psItem = null, psItemImageUrl = null, psArt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString(1, art.getId());
            psEntity.setString(2, art.getName());
            psEntity.executeUpdate();

            psItem = conn.prepareStatement(sqlItem);
            psItem.setString(1, art.getId());
            psItem.setString(2, art.getSellerId());
            psItem.setString(3, art.getCreator());
            psItem.setString(4, art.getDescription());
            psItem.setString(5, art.getType());
            psItem.executeUpdate();

            psItemImageUrl = conn.prepareStatement(sqlItemImageUrl);
            for (String url : art.getImageUrl()) {
                psItemImageUrl.setString(1, art.getId());
                psItemImageUrl.setString(2, url);
                psItemImageUrl.addBatch();
            }
            psItemImageUrl.executeBatch();

            psArt = conn.prepareStatement(sqlArt);
            psArt.setString(1, art.getId());
            psArt.setBoolean(2, art.getCertificate());
            psArt.executeUpdate();

            conn.commit();
            logger.info("da luu art: " + art.getId() + " - " + art.getName());
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            logger.warning("saveArt vi pham rang buoc (co the do id item trung hoac sellerid khong ton tai): "
                    + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi saveArt [" + art.getName() + "]: " + e.getMessage());
            return false;
        } finally {
            resetAutocommit(conn);
            closeConnect(conn);
            closeResource(psEntity, psItem, psItemImageUrl, psArt);
        }
    }

    // lay danh sach cac item
    public List<Item> getItemList() {
        List<Item> item = new ArrayList<>();
        String sql = "SELECT " +
                "e.id, e.name, " +
                "i.seller_id, i.creator, i.description, i.type, " +
                "GROUP_CONCAT(img.image_url SEPARATOR ', ') AS item_image_url, " +
                "art.certificate AS art_certificate, " +
                "electronic.is_repaired AS electronic_is_repaired, electronic.warranty_period AS electronic_warranty_period, "
                +
                "vehicle.is_repaired AS vehicle_is_repaired, vehicle.warranty_period AS vehicle_warranty_period " +
                "FROM entity e " +
                "JOIN item i ON e.id = i.id " +
                "LEFT JOIN item_image_url img ON i.id = img.item_id " +
                "LEFT JOIN art art ON i.id = art.id " +
                "LEFT JOIN electronic electronic ON i.id = electronic.id " +
                "LEFT JOIN vehicle vehicle ON i.id = vehicle.id " +
                "GROUP BY e.id;";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                item.add(mapResultSetToItem(rs));
            }
            logger.info("getAll Item: " + item.size() + " kết quả");
            return item;
        } catch (SQLException e) {
            logger.severe("Lỗi khi lấy danh sách Item: " + e.getMessage());
            return item;
        } finally {
            closeConnect(conn);
            closeResource(rs, ps);
        }
    }

    public List<Item> findBySellerId(String sellerId) {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "i.seller_id, i.creator, i.description, i.type, " +
                "GROUP_CONCAT(img.image_url SEPARATOR ', ') AS item_image_url, " +
                "art.certificate AS art_certificate, " +
                "elec.is_repaired AS electronic_is_repaired, elec.warranty_period AS electronic_warranty_period, " +
                "vehicle.is_repaired AS vehicle_is_repaired, vehicle.warranty_period AS vehicle_warranty_period " +
                "FROM entity e " +
                "JOIN item i ON e.id = i.id " +
                "LEFT JOIN item_image_url img ON i.id = img.item_id " +
                "LEFT JOIN art art ON i.id = art.id " +
                "LEFT JOIN electronic elec ON i.id = elec.id " +
                "LEFT JOIN vehicle vehicle ON i.id = vehicle.id " +
                "WHERE i.seller_id = ? " +
                "GROUP BY e.id";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Item> list = new ArrayList<>();

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, sellerId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToItem(rs));
            }
            return list;

        } catch (SQLException e) {
            logger.severe("Lỗi findBySellerId [" + sellerId + "]: " + e.getMessage());
            return list;
        } finally {
            closeConnect(conn);
            closeResource(rs, ps);
        }
    }

    public Item findById(String id) {
        String sql = "SELECT " +
                "e.id, e.name, " +
                "i.seller_id, i.creator, i.description, i.type, " +
                "GROUP_CONCAT(img.image_url SEPARATOR ', ') AS item_image_url, " +
                "art.certificate AS art_certificate, " +
                "elec.is_repaired AS electronic_is_repaired, elec.warranty_period AS electronic_warranty_period, " +
                "vehicle.is_repaired AS vehicle_is_repaired, vehicle.warranty_period AS vehicle_warranty_period " +
                "FROM entity e " +
                "JOIN item i ON e.id = i.id " +
                "LEFT JOIN item_image_url img ON i.id = img.item_id " +
                "LEFT JOIN art art ON i.id = art.id " +
                "LEFT JOIN electronic elec ON i.id = elec.id " +
                "LEFT JOIN vehicle vehicle ON i.id = vehicle.id " +
                "WHERE e.id = ? " +
                "GROUP BY e.id";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToItem(rs);
            }
            return null;

        } catch (SQLException e) {
            logger.severe("Loi khi tim theo id " + id + " - " + e.getMessage());
            return null;
        } finally {
            closeConnect(conn);
            closeResource(rs, ps);
        }
    }

    public boolean deleteById(String itemId) {
        String sql = "DELETE FROM entity WHERE id = ? ";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, itemId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                logger.warning("deleteItemId id=" + itemId + " - không thể xóa ");
            }
            return rows > 0;

        } catch (SQLException e) {
            logger.severe("Lỗi deleteAuction id=" + itemId + ": " + e.getMessage());
            return false;
        } finally {
            closeConnect(conn);
            closeResource(ps);
        }
    }

    // ham ho tro
    public Item mapResultSetToItem(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String sellerId = rs.getString("seller_id");
        String creator = rs.getString("creator");
        String description = rs.getString("description");
        String type = rs.getString("type");
        List<String> imageUrl = parseStringToList(rs.getString("item_image_url"));
        switch (type) {
            case "ART": {
                boolean certificate = rs.getBoolean("art_certificate");
                Art art = new Art(id, name, sellerId, creator, description, type, imageUrl, certificate);
                return art;
            }
            case "VEHICLE": {
                boolean isRepaired = rs.getBoolean("vehicle_is_repaired");
                int warrantyPeriod = rs.getInt("vehicle_warranty_period");
                Vehicle vehicle = new Vehicle(id, name, sellerId, creator, description, type, imageUrl, isRepaired,
                        warrantyPeriod);
                return vehicle;
            }
            case "ELECTRONIC": {
                boolean isRepaired = rs.getBoolean("electronic_is_repaired");
                int warrantyPeriod = rs.getInt("electronic_warranty_period");
                Electronic electronic = new Electronic(id, name, sellerId, creator, description, type, imageUrl,
                        isRepaired, warrantyPeriod);
                return electronic;
            }
            default:
                throw new SQLException("ItemType khong xac dinh duoc type " + type);
        }
    }

    public List <String> parseStringToList (String input){
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = input.split(",");
        List<String> result = new ArrayList<>();
        for (String partString : parts) {
            String trimmedUrl = partString.trim();
            if (!trimmedUrl.isEmpty()) {
                result.add(trimmedUrl);
            }
        }
        return result;
    }

    public boolean updateItemImages(String itemId, List<String> newUrls) {
        String sqlDelete = "DELETE FROM item_image_url WHERE item_id = ?";
        String sqlInsert = "INSERT INTO item_image_url (item_id, image_url) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement psDelete = null;
        PreparedStatement psInsert = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction
            //Xóa ảnh cũ
            psDelete = conn.prepareStatement(sqlDelete);
            psDelete.setString(1, itemId);
            psDelete.executeUpdate();

            //Nhét mới
            if (newUrls != null && !newUrls.isEmpty()) {
                psInsert = conn.prepareStatement(sqlInsert);
                for (String url : newUrls) {
                    psInsert.setString(1, itemId);
                    psInsert.setString(2, url);
                    psInsert.addBatch();
                }
                psInsert.executeBatch(); // Chạy 1 phát insert tất cả
            }

            conn.commit();
            logger.info("Đã cập nhật ảnh thành công cho Item ID: " + itemId);
            return true;

        } catch (SQLException e) {
            safelyRollback(conn);
            logger.severe("Lỗi updateItemImages cho Item [" + itemId + "]: " + e.getMessage());
            return false;
        } finally {
            resetAutocommit(conn);
            closeConnect(conn);
            closeResource(psDelete, psInsert);
        }
    }
}
