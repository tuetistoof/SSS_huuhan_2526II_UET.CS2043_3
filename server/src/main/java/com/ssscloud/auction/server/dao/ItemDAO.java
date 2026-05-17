package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ssscloud.auction.common.exception.DAOException;
import com.ssscloud.auction.common.exception.ErrorCode;
import com.ssscloud.auction.common.model.Art;
import com.ssscloud.auction.common.model.Electronic;
import com.ssscloud.auction.common.model.Vehicle;
import com.ssscloud.auction.common.model.base.Item;

public class ItemDAO extends BaseDAO {
    // Logging Standards: Declared first as a private static final attribute
    private static final Logger logger = Logger.getLogger(ItemDAO.class.getName());

    // --- PUBLIC METHODS ---

    public boolean saveElectronic(Electronic electronic) throws DAOException, Exception {
        logger.log(Level.INFO, "Initiating persistence for electronic item: {0}", electronic.getId());
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlItem = "INSERT INTO item (id, seller_id, creator, description, type) VALUES (?, ?, ?, ?, ?)";
        String sqlItemImageUrl = "INSERT INTO item_image_url (item_id, image_url) VALUES (?,?)";
        String sqlElectronic = "INSERT INTO electronic (id, is_repaired, warranty_period) VALUES (?, ?, ?)";
        
        Connection connection = null;
        PreparedStatement psEntity = null;
        PreparedStatement psItem = null;
        PreparedStatement psItemImageUrl = null;
        PreparedStatement psElectronic = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            psEntity = connection.prepareStatement(sqlEntity);
            psEntity.setString(1, electronic.getId());
            psEntity.setString(2, electronic.getName());
            psEntity.executeUpdate();

            psItem = connection.prepareStatement(sqlItem);
            psItem.setString(1, electronic.getId());
            psItem.setString(2, electronic.getSellerId());
            psItem.setString(3, electronic.getCreator());
            psItem.setString(4, electronic.getDescription());
            psItem.setString(5, electronic.getType());
            psItem.executeUpdate();

            psItemImageUrl = connection.prepareStatement(sqlItemImageUrl);
            for (String imageUrl : electronic.getImageUrl()) {
                psItemImageUrl.setString(1, electronic.getId());
                psItemImageUrl.setString(2, imageUrl);
                psItemImageUrl.addBatch();
            }
            psItemImageUrl.executeBatch();

            psElectronic = connection.prepareStatement(sqlElectronic);
            psElectronic.setString(1, electronic.getId());
            psElectronic.setBoolean(2, electronic.getIsRepaired());
            psElectronic.setInt(3, electronic.getWarrantyPeriod());
            psElectronic.executeUpdate();

            connection.commit();
            logger.log(Level.INFO, "Electronic item successfully persisted with ID: {0}", electronic.getId());
            return true;
        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.DATA_INTEGRITY_VIOLATION, "Constraint violation: Electronic item or entity already exists.");
        } catch (SQLException sqlException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.ITEM_SAVE_FAILED, "Database interaction failure while saving electronic item.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in ItemDAO.saveElectronic", exception);
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeConnect(connection);
            closeResource(psEntity, psItem, psItemImageUrl, psElectronic);
        }
    }

    public boolean saveVehicle(Vehicle vehicle) throws DAOException, Exception {
        logger.log(Level.INFO, "Initiating persistence for vehicle item: {0}", vehicle.getId());
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlItem = "INSERT INTO item (id, seller_id, creator, description, type) VALUES (?, ?, ?, ?, ?)";
        String sqlItemImageUrl = "INSERT INTO item_image_url (item_id, image_url) VALUES (?,?)";
        String sqlVehicle = "INSERT INTO vehicle (id, is_repaired, warranty_period) VALUES (?, ?, ?)";

        Connection connection = null;
        PreparedStatement psEntity = null;
        PreparedStatement psItem = null;
        PreparedStatement psItemImageUrl = null;
        PreparedStatement psVehicle = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            psEntity = connection.prepareStatement(sqlEntity);
            psEntity.setString(1, vehicle.getId());
            psEntity.setString(2, vehicle.getName());
            psEntity.executeUpdate();

            psItem = connection.prepareStatement(sqlItem);
            psItem.setString(1, vehicle.getId());
            psItem.setString(2, vehicle.getSellerId());
            psItem.setString(3, vehicle.getCreator());
            psItem.setString(4, vehicle.getDescription());
            psItem.setString(5, vehicle.getType());
            psItem.executeUpdate();

            psItemImageUrl = connection.prepareStatement(sqlItemImageUrl);
            for (String imageUrl : vehicle.getImageUrl()) {
                psItemImageUrl.setString(1, vehicle.getId());
                psItemImageUrl.setString(2, imageUrl);
                psItemImageUrl.addBatch();
            }
            psItemImageUrl.executeBatch();

            psVehicle = connection.prepareStatement(sqlVehicle);
            psVehicle.setString(1, vehicle.getId());
            psVehicle.setBoolean(2, vehicle.getIsRepaired());
            psVehicle.setInt(3, vehicle.getWarrantyPeriod());
            psVehicle.executeUpdate();

            connection.commit();
            logger.log(Level.INFO, "Vehicle item successfully persisted with ID: {0}", vehicle.getId());
            return true;
        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.DATA_INTEGRITY_VIOLATION, "Constraint violation: Vehicle item or entity already exists.");
        } catch (SQLException sqlException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.ITEM_SAVE_FAILED, "Database interaction failure while saving vehicle item.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in ItemDAO.saveVehicle", exception);
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeConnect(connection);
            closeResource(psEntity, psItem, psItemImageUrl, psVehicle);
        }
    }

    public boolean saveArt(Art art) throws DAOException, Exception {
        logger.log(Level.INFO, "Initiating persistence for art item: {0}", art.getId());
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlItem = "INSERT INTO item (id, seller_id, creator, description, type) VALUES (?, ?, ?, ?, ?)";
        String sqlItemImageUrl = "INSERT INTO item_image_url (item_id, image_url) VALUES (?,?)";
        String sqlArt = "INSERT INTO art (id, certificate) VALUES (?, ?)";

        Connection connection = null;
        PreparedStatement psEntity = null;
        PreparedStatement psItem = null;
        PreparedStatement psItemImageUrl = null;
        PreparedStatement psArt = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            psEntity = connection.prepareStatement(sqlEntity);
            psEntity.setString(1, art.getId());
            psEntity.setString(2, art.getName());
            psEntity.executeUpdate();

            psItem = connection.prepareStatement(sqlItem);
            psItem.setString(1, art.getId());
            psItem.setString(2, art.getSellerId());
            psItem.setString(3, art.getCreator());
            psItem.setString(4, art.getDescription());
            psItem.setString(5, art.getType());
            psItem.executeUpdate();

            psItemImageUrl = connection.prepareStatement(sqlItemImageUrl);
            for (String imageUrl : art.getImageUrl()) {
                psItemImageUrl.setString(1, art.getId());
                psItemImageUrl.setString(2, imageUrl);
                psItemImageUrl.addBatch();
            }
            psItemImageUrl.executeBatch();

            psArt = connection.prepareStatement(sqlArt);
            psArt.setString(1, art.getId());
            psArt.setBoolean(2, art.getCertificate());
            psArt.executeUpdate();

            connection.commit();
            logger.log(Level.INFO, "Art item successfully persisted with ID: {0}", art.getId());
            return true;
        } catch (SQLIntegrityConstraintViolationException sqlConstraintException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.DATA_INTEGRITY_VIOLATION, "Constraint violation: Art item or entity already exists.");
        } catch (SQLException sqlException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.ITEM_SAVE_FAILED, "Database interaction failure while saving art item.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in ItemDAO.saveArt", exception);
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeConnect(connection);
            closeResource(psEntity, psItem, psItemImageUrl, psArt);
        }
    }

    public List<Item> getItemList() throws DAOException, Exception {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT " +
                "e.id, e.name, " +
                "i.seller_id, i.creator, i.description, i.type, " +
                "GROUP_CONCAT(img.image_url SEPARATOR ', ') AS item_image_url, " +
                "art.certificate AS art_certificate, " +
                "electronic.is_repaired AS electronic_is_repaired, electronic.warranty_period AS electronic_warranty_period, " +
                "vehicle.is_repaired AS vehicle_is_repaired, vehicle.warranty_period AS vehicle_warranty_period " +
                "FROM entity e " +
                "JOIN item i ON e.id = i.id " +
                "LEFT JOIN item_image_url img ON i.id = img.item_id " +
                "LEFT JOIN art art ON i.id = art.id " +
                "LEFT JOIN electronic electronic ON i.id = electronic.id " +
                "LEFT JOIN vehicle vehicle ON i.id = vehicle.id " +
                "GROUP BY e.id;";

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                itemList.add(mapResultSetToItem(rs));
            }
            logger.log(Level.INFO, "Successfully retrieved Item list: {0} result(s).", itemList.size());
            return itemList;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.ITEM_FETCH_FAILED, "Database interaction failure while retrieving global item list.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in ItemDAO.getItemList", exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(rs, ps);
        }
    }

    public List<Item> findBySellerId(String sellerId) throws DAOException, Exception {
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

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Item> itemList = new ArrayList<>();

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, sellerId);
            rs = ps.executeQuery();
            while (rs.next()) {
                itemList.add(mapResultSetToItem(rs));
            }
            logger.log(Level.INFO, "Successfully retrieved {0} item(s) for sellerId: {1}", new Object[]{itemList.size(), sellerId});
            return itemList;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.ITEM_FETCH_FAILED, "Database interaction failure while retrieving items by sellerId.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in ItemDAO.findBySellerId", exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(rs, ps);
        }
    }

    public Item findById(String itemId) throws DAOException, Exception {
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

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, itemId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToItem(rs);
            }
            logger.log(Level.INFO, "No item found for identifier: {0}", itemId);
            return null;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.ITEM_FETCH_FAILED, "Database interaction failure while retrieving item by ID.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in ItemDAO.findById", exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(rs, ps);
        }
    }

    public boolean deleteById(String itemId) throws DAOException, Exception {
        String sql = "DELETE FROM entity WHERE id = ? ";
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, itemId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                logger.log(Level.WARNING, "Resource not found for deletion: itemId {0}", itemId);
            }
            logger.log(Level.INFO, "Item deletion completed for ID: {0}", itemId);
            return rows > 0;
        } catch (SQLException sqlException) {
            throw new DAOException(ErrorCode.ITEM_DELETE_FAILED, "Database interaction failure during item deletion.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in ItemDAO.deleteById", exception);
            throw exception;
        } finally {
            closeConnect(connection);
            closeResource(ps);
        }
    }

    public boolean updateItemImages(String itemId, List<String> newUrlsList) throws DAOException, Exception {
        String sqlDelete = "DELETE FROM item_image_url WHERE item_id = ?";
        String sqlInsert = "INSERT INTO item_image_url (item_id, image_url) VALUES (?, ?)";

        Connection connection = null;
        PreparedStatement psDelete = null;
        PreparedStatement psInsert = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            
            psDelete = connection.prepareStatement(sqlDelete);
            psDelete.setString(1, itemId);
            psDelete.executeUpdate();

            if (newUrlsList != null && !newUrlsList.isEmpty()) {
                psInsert = connection.prepareStatement(sqlInsert);
                for (String imageUrl : newUrlsList) {
                    psInsert.setString(1, itemId);
                    psInsert.setString(2, imageUrl);
                    psInsert.addBatch();
                }
                psInsert.executeBatch();
            }

            connection.commit();
            logger.log(Level.INFO, "Successfully updated image records for itemId: {0}", itemId);
            return true;
        } catch (SQLException sqlException) {
            safelyRollback(connection);
            throw new DAOException(ErrorCode.ITEM_UPDATE_FAILED, "Database interaction failure while updating item images.");
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "[SYSTEM_FAILURE] Unexpected system error in ItemDAO.updateItemImages", exception);
            throw exception;
        } finally {
            resetAutocommit(connection);
            closeConnect(connection);
            closeResource(psDelete, psInsert);
        }
    }

    // --- PRIVATE METHODS ---

    public Item mapResultSetToItem(ResultSet rs) throws SQLException {
        String itemId = rs.getString("id");
        String name = rs.getString("name");
        String sellerId = rs.getString("seller_id");
        String creator = rs.getString("creator");
        String description = rs.getString("description");
        String type = rs.getString("type");
        List<String> imageUrlsList = parseStringToList(rs.getString("item_image_url"));

        switch (type) {
            case "ART": {
                boolean certificate = rs.getBoolean("art_certificate");
                return new Art(itemId, name, sellerId, creator, description, type, imageUrlsList, certificate);
            }
            case "VEHICLE": {
                boolean isRepaired = rs.getBoolean("vehicle_is_repaired");
                int warrantyPeriod = rs.getInt("vehicle_warranty_period");
                return new Vehicle(itemId, name, sellerId, creator, description, type, imageUrlsList, isRepaired, warrantyPeriod);
            }
            case "ELECTRONIC": {
                boolean isRepaired = rs.getBoolean("electronic_is_repaired");
                int warrantyPeriod = rs.getInt("electronic_warranty_period");
                return new Electronic(itemId, name, sellerId, creator, description, type, imageUrlsList, isRepaired, warrantyPeriod);
            }
            default:
                throw new SQLException("Unknown Item type: " + type);
        }
    }

    public List<String> parseStringToList(String inputString) {
        if (inputString == null || inputString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = inputString.split(",");
        List<String> urlList = new ArrayList<>();
        for (String urlPart : parts) {
            String trimmedUrl = urlPart.trim();
            if (!trimmedUrl.isEmpty()) {
                urlList.add(trimmedUrl);
            }
        }
        return urlList;
    }
}
