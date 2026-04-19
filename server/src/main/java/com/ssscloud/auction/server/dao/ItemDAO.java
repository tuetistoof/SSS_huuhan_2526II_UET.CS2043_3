package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ssscloud.auction.common.enums.ItemType;
import com.ssscloud.auction.common.model.Art;
import com.ssscloud.auction.common.model.Electronic;
import com.ssscloud.auction.common.model.Vehicle;
import com.ssscloud.auction.common.model.base.Item;

public class ItemDAO extends BaseDAO {
    public boolean saveElectronic (Electronic electronic) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlItem = "INSERT INTO item (id, seller_id, manufacturing_date, creator, description, type) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlItemImageUrl = "INSERT INTO item_image_url (item_id, image_url) VALUES (?,?)";
        String sqlElectronic = "INSERT INTO electronic (id, is_repaired, purchase_date, warranty_period) VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement psEntity = null, psItem = null, psIemImageUrl = null, psElectronic = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString (1, electronic.getId());
            psEntity.setString (2, electronic.getName());
            psEntity.executeUpdate();

            psItem = conn.prepareStatement(sqlItem);
            psItem.setString (1, electronic.getId());
            psItem.setLong(2, electronic.getBasePrice());
            psItem.setObject(3, electronic.getManufacturingDate()); // de RTE hoac bi nem ngoaoi le
            psItem.setString(4, electronic.getCreator());
            psItem.setString(5, electronic.getDescription());
            psItem.setString(6, electronic.getType().name());
            psItem.executeUpdate();
            
            for (String url: electronic.getImageUrl())
            {
                psIemImageUrl = conn.prepareStatement(sqlItemImageUrl);
                psIemImageUrl.setString(1, electronic.getId());
                psIemImageUrl.setString(2, url);
                psIemImageUrl.executeUpdate();
            }

            psElectronic = conn.prepareStatement(sqlElectronic);
            psElectronic.setString (1,electronic.getId());
            psElectronic.setBoolean(2, electronic.getIsRepair());
            psElectronic.setObject(3, electronic.getPurchaseDate());
            psElectronic.setInt(4, electronic.getWarrantyPeriod());
            psElectronic.executeUpdate();

            conn.commit();
            logger.info("da luu electronic: " + electronic.getId() + " - " + electronic.getName());
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
             logger.warning("saveElectronic vi pham rang buoc (co the do id item trung hoac sellerid khong ton tai): " + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi saveElectronic [" + electronic.getName() + "]: " + e.getMessage());
            return false;
        } finally {
            resetAutocommit(conn);
            closeResource(psEntity, psItem, psElectronic);
        }
    }
    public boolean saveVehicle (Vehicle vehicle) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
       String sqlItem = "INSERT INTO item (id, seller_id, manufacturing_date, creator, description, type) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlItemImageUrl = "INSERT INTO item_image_url (item_id, image_url) VALUES (?,?)";
        String sqlVehicle = "INSERT INTO vehicle (id, is_repaired, purchase_date, warranty_period) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement psEntity = null, psItem = null, psIemImageUrl = null, psVehicle = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString (1, vehicle.getId());
            psEntity.setString (2, vehicle.getName());
            psEntity.executeUpdate();

            psItem = conn.prepareStatement(sqlItem);
            psItem.setString (1, vehicle.getId());
            psItem.setLong(2, vehicle.getBasePrice());
            psItem.setObject(3, vehicle.getManufacturingDate()); // de RTE hoac bi nem ngoaoi le
            psItem.setString(4, vehicle.getCreator());
            psItem.setString(5, vehicle.getDescription());
            psItem.setString(6, vehicle.getType().name());
            psItem.executeUpdate();

            for (String url: vehicle.getImageUrl())
            {
                psIemImageUrl = conn.prepareStatement(sqlItemImageUrl);
                psIemImageUrl.setString(1, vehicle.getId());
                psIemImageUrl.setString(2, url);
                psIemImageUrl.executeUpdate();
            }

            psVehicle = conn.prepareStatement(sqlVehicle);
            psVehicle.setString (1,vehicle.getId());
            psVehicle.setBoolean(2, vehicle.getIsRepaired());
            psVehicle.setObject(3, vehicle.getPurchaseDate());
            psVehicle.setInt(4, vehicle.getWarrantyPeriod());
            psVehicle.executeUpdate();

            conn.commit();
            logger.info("da luu Vehicle: " + vehicle.getId() + " - " + vehicle.getName());
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
             logger.warning("saveVehicle vi pham rang buoc (co the do id item trung hoac sellerid khong ton tai): " + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi saveVehicle [" + vehicle.getName() + "]: " + e.getMessage());
            return false;
        } finally {
            resetAutocommit(conn);
            closeResource(psEntity, psItem, psVehicle);
        }
    }

    public boolean saveArt (Art art) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
       String sqlItem = "INSERT INTO item (id, seller_id, manufacturing_date, creator, description, type) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlItemImageUrl = "INSERT INTO item_image_url (item_id, image_url) VALUES (?,?)";
        String sqlArt = "INSERT INTO art (id, certificate) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement psEntity = null, psItem = null, psIemImageUrl = null, psArt = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psEntity = conn.prepareStatement(sqlEntity);
            psEntity.setString (1, art.getId());
            psEntity.setString (2, art.getName());
            psEntity.executeUpdate();

            psItem = conn.prepareStatement(sqlItem);
            psItem.setString (1, art.getId());
            psItem.setLong(2, art.getBasePrice());
            psItem.setObject(3, art.getManufacturingDate()); // de RTE hoac bi nem ngoaoi le
            psItem.setString(4, art.getCreator());
            psItem.setString(5, art.getDescription());
            psItem.setString(6, art.getType().name());
            psItem.executeUpdate();

            for (String url: art.getImageUrl())
            {
                psIemImageUrl = conn.prepareStatement(sqlItemImageUrl);
                psIemImageUrl.setString(1, art.getId());
                psIemImageUrl.setString(2, url);
                psIemImageUrl.executeUpdate();
            }

            psArt = conn.prepareStatement(sqlArt);
            psArt.setString (1,art.getId());
            psArt.setBoolean(2,art.getCertificate());
            psArt.executeUpdate();

            conn.commit();
            logger.info("da luu art: " + art.getId() + " - " + art.getName());
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
             logger.warning("saveArt vi pham rang buoc (co the do id item trung hoac sellerid khong ton tai): " + e.getMessage());
            safelyRollback(conn);
            return false;
        } catch (SQLException e) {
            logger.severe("Loi saveArt [" + art.getName() + "]: " + e.getMessage());
            return false;
        } finally {
            resetAutocommit(conn);
            closeResource(psEntity, psItem, psArt);
        }
    }

    // lay danh sach cac item
    public List <Item> getItemList() {
        List <Item> item = new ArrayList<>();
        String sql = "SELECT " +
                    "e.id, e.name, " +
                    "i.seller_id, i.base_price, i.manufacturing_date, i.creator, i.decription, i.type " + 
                    "GROUP_CONCAT(img.image_url SEPARATOR ', ') AS item_image_url, " +
                    "art.certificate AS art_certificate, " + 
                    "electronic.is_repaired AS electronic_is_repaired, electronic.purchase_date AS electronic_purchase_date, electronic.warranty_period AS electronic_warranty_period, " + 
                    "vehicle.is_repaired AS vehicle_is_repaired, vehicle.purchase_date AS vehicle_purchase_date, vehicle.warranty_period AS vehicle_warranty_period " +
                    "FROM entity e " +
                    "JOIN item i ON e.id = i.id " +
                    "LEFT JOIN item_image img ON i.id = img.item_id " +
                    "LEFT JOIN art art ON i.id = art.id " +
                    "LEFT JOIN electronic s ON i.id = electronic.id " +
                    "LEFT JOIN vehicle vehicle ON i.id = vehicle.id ";
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
        }
        catch (SQLException e) {
            logger.severe("Lỗi khi lấy danh sách Item: " + e.getMessage());
            return item;
        } finally {
            closeResource(rs, ps);
        }
    } 

    public List <Item> findBySellerId (String sellerId){
        String sql = "SELECT i.id FROM item i WHERE i.seller_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Item> list = new ArrayList<>();

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, sellerId);
            rs = ps.executeQuery();

            List<String> rows = new ArrayList<>();
            while (rs.next()) {
                String id = rs.getString("id");
                rows.add(id);
            }
            

            for (String row : rows) {
                Item item = findById (row);
                if (item != null) list.add(item);
            }
            return list;

        } catch (SQLException e) {
            logger.severe("Lỗi findBySellerId [" + sellerId + "]: " + e.getMessage());
            return list;
        } finally {
            closeResource(rs, ps);
        }
    }
    
    public Item findById(String id) {
        String sql = "SELECT " +
                    "e.id, e.name, " +
                    "i.seller_id, i.base_price, i.manufacturing_date, i.creator, i.decription, i.type " + 
                    "GROUP_CONCAT(img.image_url SEPARATOR ', ') AS item_image_url, " +
                    "art.certificate AS art_certificate, " + 
                    "electronic.is_repaired AS electronic_is_repaired, electronic.purchase_date AS electronic_purchase_date, electronic.warranty_period AS electronic_warranty_period, " + 
                    "vehicle.is_repaired AS vehicle_is_repaired, vehicle.purchase_date AS vehicle_purchase_date, vehicle.warranty_period AS vehicle_warranty_period " +
                    "FROM entity e " +
                    "JOIN item i ON e.id = i.id " +
                    "LEFT JOIN item_image img ON i.id = img.item_id " +
                    "LEFT JOIN art art ON i.id = art.id " +
                    "LEFT JOIN electronic s ON i.id = electronic.id " +
                    "LEFT JOIN vehicle vehicle ON i.id = vehicle.id " +
                    "WHERE e.id = ?";

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
            closeResource(rs, ps);
        }
    }
    
    // ham ho tro
    public Item mapResultSetToItem(ResultSet rs) throws SQLException {
        String id = rs.getString ("id");
        String name = rs.getString ("name");
        String sellerId = rs.getString ("seller_id");
        Long basePrice = rs.getLong("base_price");
        LocalDate manufacturingDate = toLocalDate(rs.getDate("manufacturing_date"));
        String creator = rs.getString("creator");
        String decription = rs.getString ("decription");
        ItemType type = ItemType.valueOf(rs.getString("type"));
        List <String> imageUrl = parseStringToList(rs.getString ("item_image_url"));
        switch (type) {
            case ART: {
                boolean certificate = rs.getBoolean("art_certificate");
                Art art = new Art(id, name, sellerId, basePrice, manufacturingDate, creator, decription,type, imageUrl, certificate);
                return art;
            }
            case VEHICLE: {
                boolean isRepaired = rs.getBoolean("vehicle_is_repaired");
                LocalDate purchasDate = rs.getObject("vehicle_purchase_date", LocalDate.class);
                int warrantyPeriod = rs.getInt("vehicle_warranty_period");
                Vehicle vehicle = new Vehicle(id, name, sellerId, basePrice, manufacturingDate, creator, decription,type, imageUrl, isRepaired, purchasDate, warrantyPeriod);
                return vehicle;
            }
            case ELECTRONIC: {
                boolean isRepaired = rs.getBoolean("electronic_is_repaired");
                LocalDate purchasDate = rs.getObject("electronic_purchase_date", LocalDate.class);
                int warrantyPeriod = rs.getInt("electronic_warranty_period");
                Electronic electronic = new Electronic(id, name, sellerId, basePrice, manufacturingDate, creator, decription,type, imageUrl, isRepaired, purchasDate, warrantyPeriod);
                return electronic;
            }
            default:
                throw new SQLException("ItemType khong xac dinh duoc type " + type);
        }
    }

    public List <String> parseStringToList (String input){
        if (input == null || input.equals("[]") || input.isEmpty())
            return new ArrayList<>();
        String content = input.substring(1, input.length() - 1);
        String[] parts = content.split(",");
        List <String> result = new ArrayList<>();
        for (String partString: parts)
            if (!partString.isEmpty())
                result.add (partString.trim());
        return result;
    }
    private LocalDate toLocalDate(Date date) {
        return date != null ? date.toLocalDate() : null;
    }
}
