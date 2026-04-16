package com.ssscloud.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import com.ssscloud.auction.common.model.Art;
import com.ssscloud.auction.common.model.Electronic;
import com.ssscloud.auction.common.model.Vehicle;

public class ItemDAO extends BaseDAO {
    public boolean saveElectronic (Electronic electronic) {
        String sqlEntity = "INSERT INTO entity (id, name) VALUES (?, ?)";
        String sqlItem = "INSERT INTO item (id, seller_id, manufacturing_date, creator, description) VALUES (?, ?, ?, ?, ?)";
        String sqlElectronic = "INSERT INTO electronic (id, is_repaired, purchase_date, warranty_period) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement psEntity = null, psItem = null, psElectronic = null;
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
            psItem.executeUpdate();

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
        String sqlItem = "INSERT INTO item (id, seller_id, manufacturing_date, creator, description) VALUES (?, ?, ?, ?, ?)";
        String sqlVehicle = "INSERT INTO vehicle (id, is_repaired, purchase_date, warranty_period) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement psEntity = null, psItem = null, psVehicle = null;
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
            psItem.executeUpdate();

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
        String sqlItem = "INSERT INTO item (id, seller_id, manufacturing_date, creator, description) VALUES (?, ?, ?, ?, ?)";
        String sqlArt = "INSERT INTO art (id, certificate) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement psEntity = null, psItem = null, psArt = null;
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
            psItem.executeUpdate();

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

    
}
