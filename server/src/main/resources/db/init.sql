-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: cloud
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admin`
--

DROP TABLE IF EXISTS `admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_admin_user` FOREIGN KEY (`id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin`
--

LOCK TABLES `admin` WRITE;
/*!40000 ALTER TABLE `admin` DISABLE KEYS */;
/*!40000 ALTER TABLE `admin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `art`
--

DROP TABLE IF EXISTS `art`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `art` (
  `id` varchar(36) NOT NULL,
  `certificate` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_art_item` FOREIGN KEY (`id`) REFERENCES `item` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `art`
--

LOCK TABLES `art` WRITE;
/*!40000 ALTER TABLE `art` DISABLE KEYS */;
INSERT INTO `art` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392',1);
/*!40000 ALTER TABLE `art` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auction`
--

DROP TABLE IF EXISTS `auction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auction` (
  `id` varchar(36) NOT NULL,
  `status` varchar(50) NOT NULL,
  `seller_id` varchar(36) NOT NULL,
  `item_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_auction_seller` (`seller_id`),
  KEY `fk_auction_item` (`item_id`),
  CONSTRAINT `fk_auction_item` FOREIGN KEY (`item_id`) REFERENCES `item` (`id`),
  CONSTRAINT `fk_auction_seller` FOREIGN KEY (`seller_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_auction_to_config` FOREIGN KEY (`id`) REFERENCES `auction_config` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auction`
--

LOCK TABLES `auction` WRITE;
/*!40000 ALTER TABLE `auction` DISABLE KEYS */;
INSERT INTO `auction` VALUES ('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','RUNNING','dc35111b-344c-49ff-89ff-9426c50d3115','09a99fb0-64c7-47ef-9ee4-79415183b392'),('d369c0a6-1766-48e7-ac42-707c561c2895','RUNNING','dc35111b-344c-49ff-89ff-9426c50d3115','79f1a2db-c429-4cd4-aa90-1912915f85a7');
/*!40000 ALTER TABLE `auction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auction_config`
--

DROP TABLE IF EXISTS `auction_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auction_config` (
  `id` varchar(36) NOT NULL,
  `start_price` bigint DEFAULT '0',
  `min_increment` bigint NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `extend_second` int DEFAULT '0',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_auction_config_entity` FOREIGN KEY (`id`) REFERENCES `entity` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auction_config`
--

LOCK TABLES `auction_config` WRITE;
/*!40000 ALTER TABLE `auction_config` DISABLE KEYS */;
INSERT INTO `auction_config` VALUES ('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3',400000,50000,'2026-05-14 01:10:10','2026-05-15 12:30:00',36),('d369c0a6-1766-48e7-ac42-707c561c2895',300000,50000,'2026-05-14 01:38:37','2026-05-19 14:30:00',36);
/*!40000 ALTER TABLE `auction_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bid_transaction`
--

DROP TABLE IF EXISTS `bid_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bid_transaction` (
  `auction_id` varchar(36) NOT NULL,
  `bidder_id` varchar(36) NOT NULL,
  `bidder_username` varchar(255) DEFAULT NULL,
  `bid_amount` bigint DEFAULT NULL,
  `bid_time` datetime(6) DEFAULT NULL,
  `bid_type` varchar(50) DEFAULT NULL,
  KEY `fk_bid_transaction_auction` (`auction_id`),
  KEY `fk_bid_transactiond_user` (`bidder_id`),
  CONSTRAINT `fk_bid_transaction_auction` FOREIGN KEY (`auction_id`) REFERENCES `auction` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_bid_transactiond_user` FOREIGN KEY (`bidder_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bid_transaction`
--

LOCK TABLES `bid_transaction` WRITE;
/*!40000 ALTER TABLE `bid_transaction` DISABLE KEYS */;
INSERT INTO `bid_transaction` VALUES ('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',450000,'2026-05-14 01:12:01.163345','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',600000,'2026-05-14 01:20:09.213241','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo',1500000,'2026-05-14 01:21:35.638500','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',1600000,'2026-05-14 01:33:08.420175','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',1700000,'2026-05-14 01:35:54.159168','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',1800000,'2026-05-14 01:36:02.577750','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo',3500000,'2026-05-14 01:36:45.867124','MANUAL'),('d369c0a6-1766-48e7-ac42-707c561c2895','9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo',400000,'2026-05-14 01:40:10.974869','MANUAL');
/*!40000 ALTER TABLE `bid_transaction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bidder`
--

DROP TABLE IF EXISTS `bidder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bidder` (
  `id` varchar(36) NOT NULL,
  `account_balance` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_bidder_user` FOREIGN KEY (`id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bidder`
--

LOCK TABLES `bidder` WRITE;
/*!40000 ALTER TABLE `bidder` DISABLE KEYS */;
INSERT INTO `bidder` VALUES ('9171ac52-84f8-4cf1-b248-e2cab9520dd0',1000000000),('d0ffa144-26f1-4f86-a877-ecc952e72c3d',10450000);
/*!40000 ALTER TABLE `bidder` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `electronic`
--

DROP TABLE IF EXISTS `electronic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `electronic` (
  `id` varchar(36) NOT NULL,
  `is_repaired` tinyint(1) DEFAULT '0',
  `warranty_period` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_electronic_item` FOREIGN KEY (`id`) REFERENCES `item` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `electronic`
--

LOCK TABLES `electronic` WRITE;
/*!40000 ALTER TABLE `electronic` DISABLE KEYS */;
/*!40000 ALTER TABLE `electronic` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `entity`
--

DROP TABLE IF EXISTS `entity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `entity` (
  `id` varchar(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `entity`
--

LOCK TABLES `entity` WRITE;
/*!40000 ALTER TABLE `entity` DISABLE KEYS */;
INSERT INTO `entity` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392','Beako plushy'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','TETO'),('9171ac52-84f8-4cf1-b248-e2cab9520dd0','Trung Lee'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','bán loli'),('d0ffa144-26f1-4f86-a877-ecc952e72c3d','hà anh'),('d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO'),('dc35111b-344c-49ff-89ff-9426c50d3115','trung lê'),('ef532146-0c6f-4637-ae82-f35612861d6d','TETO');
/*!40000 ALTER TABLE `entity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `item`
--

DROP TABLE IF EXISTS `item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item` (
  `id` varchar(36) NOT NULL,
  `seller_id` varchar(36) DEFAULT NULL,
  `creator` varchar(255) DEFAULT NULL,
  `description` text,
  `type` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_item_seller` (`seller_id`),
  CONSTRAINT `fk_item_entity` FOREIGN KEY (`id`) REFERENCES `entity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_item_seller` FOREIGN KEY (`seller_id`) REFERENCES `seller` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `item`
--

LOCK TABLES `item` WRITE;
/*!40000 ALTER TABLE `item` DISABLE KEYS */;
INSERT INTO `item` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392','dc35111b-344c-49ff-89ff-9426c50d3115','internet','mỗi người nên có ít nhất 1 bé','ART'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','dc35111b-344c-49ff-89ff-9426c50d3115','INTERNET','Ai cũng cần 1 con','VEHICLE'),('ef532146-0c6f-4637-ae82-f35612861d6d','dc35111b-344c-49ff-89ff-9426c50d3115','internet','TETO123','ELECTRONIC');
/*!40000 ALTER TABLE `item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `item_image_url`
--

DROP TABLE IF EXISTS `item_image_url`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_image_url` (
  `item_id` varchar(36) NOT NULL,
  `image_url` text,
  KEY `fk_itemimage_item` (`item_id`),
  CONSTRAINT `fk_itemimage_item` FOREIGN KEY (`item_id`) REFERENCES `item` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `item_image_url`
--

LOCK TABLES `item_image_url` WRITE;
/*!40000 ALTER TABLE `item_image_url` DISABLE KEYS */;
INSERT INTO `item_image_url` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392','https://th.bing.com/th/id/OIP.8v16bx0DQxWJFDZbKeOrpwHaHh?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('ef532146-0c6f-4637-ae82-f35612861d6d','https://th.bing.com/th/id/OIP.hJzaTf6QcFEaBgShJ8EIcgHaHa?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','https://thuvienmeme.com/wp-content/uploads/2024/04/meme-sieu-nhan-ngoi-chong-cam-hanh-phuc.jpg'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','https://th.bing.com/th/id/OIP.YovTjY1b8reVgfROUVVubwHaHa?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','https://th.bing.com/th/id/OIP.stBsb0sfY-NXgqF8hVWgCAHaHa?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3');
/*!40000 ALTER TABLE `item_image_url` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `seller`
--

DROP TABLE IF EXISTS `seller`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seller` (
  `id` varchar(36) NOT NULL,
  `bank_account` varchar(255) NOT NULL,
  `account_balance` bigint DEFAULT '0',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_seller_user` FOREIGN KEY (`id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `seller`
--

LOCK TABLES `seller` WRITE;
/*!40000 ALTER TABLE `seller` DISABLE KEYS */;
INSERT INTO `seller` VALUES ('dc35111b-344c-49ff-89ff-9426c50d3115','123456',0);
/*!40000 ALTER TABLE `seller` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` varchar(36) NOT NULL,
  `username` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(150) NOT NULL,
  `role` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_name` (`username`),
  CONSTRAINT `fk_user_entity` FOREIGN KEY (`id`) REFERENCES `entity` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES ('9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo','trung2007','ainfo@gmail.com','BIDDER'),('d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion','123456','into@gmail.com','BIDDER'),('dc35111b-344c-49ff-89ff-9426c50d3115','trunglee','trung2007','ainfo@gmail.com','SELLER');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vehicle`
--

DROP TABLE IF EXISTS `vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle` (
  `id` varchar(36) NOT NULL,
  `is_repaired` tinyint(1) DEFAULT '0',
  `warranty_period` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vehicle_item` FOREIGN KEY (`id`) REFERENCES `item` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicle`
--

LOCK TABLES `vehicle` WRITE;
/*!40000 ALTER TABLE `vehicle` DISABLE KEYS */;
INSERT INTO `vehicle` VALUES ('79f1a2db-c429-4cd4-aa90-1912915f85a7',0,36);
/*!40000 ALTER TABLE `vehicle` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `watchlist`
--

DROP TABLE IF EXISTS `watchlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `watchlist` (
  `auction_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  PRIMARY KEY (`auction_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `watchlist_ibfk_1` FOREIGN KEY (`auction_id`) REFERENCES `auction` (`id`),
  CONSTRAINT `watchlist_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `watchlist`
--

LOCK TABLES `watchlist` WRITE;
/*!40000 ALTER TABLE `watchlist` DISABLE KEYS */;
/*!40000 ALTER TABLE `watchlist` ENABLE KEYS */;
UNLOCK TABLES;
--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id`           VARCHAR(36)  NOT NULL,
  `user_id`      VARCHAR(36)  NOT NULL,
  `type`         VARCHAR(20)  NOT NULL,
  `auction_id`   VARCHAR(36)  NOT NULL,
  `auction_name` VARCHAR(255) NOT NULL,
  `price`        BIGINT       NOT NULL,
  `winner`       VARCHAR(100) DEFAULT NULL,
  `is_read`      TINYINT(1)   NOT NULL DEFAULT '0',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_unread` (`user_id`, `is_read`),
  CONSTRAINT `notification_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification`
--

LOCK TABLES `notification` WRITE;
/*!40000 ALTER TABLE `notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification` ENABLE KEYS */;
UNLOCK TABLES;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-14  1:41:56