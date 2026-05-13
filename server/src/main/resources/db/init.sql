-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: cloud
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
INSERT INTO `art` VALUES ('300b3417-94ca-4ded-ac87-8e060366a281',1),('343e6724-7efb-4958-a05d-9a815ab6bf08',1);
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
INSERT INTO `auction` VALUES ('995ed873-5084-418f-8942-bdcb906ad320','RUNNING','c79f0cf0-b48e-4dc6-8025-59f3eadb0974','300b3417-94ca-4ded-ac87-8e060366a281'),('9c5e920e-54c5-4fdc-bb74-b6c1c6f8be4e','RUNNING','c79f0cf0-b48e-4dc6-8025-59f3eadb0974','343e6724-7efb-4958-a05d-9a815ab6bf08'),('b89ed511-03a0-4522-a45f-b8186265ef51','FINISHED','c79f0cf0-b48e-4dc6-8025-59f3eadb0974','0f898bfd-c02f-490c-af40-69763e6e74d8');
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
INSERT INTO `auction_config` VALUES ('995ed873-5084-418f-8942-bdcb906ad320',15000000,600000,'2026-05-12 08:00:00','2026-05-16 22:00:00',36),('9c5e920e-54c5-4fdc-bb74-b6c1c6f8be4e',50000,10000,'2026-05-13 02:14:16','2026-05-14 03:00:00',36),('b89ed511-03a0-4522-a45f-b8186265ef51',15000000,5000000,'2026-05-04 03:40:09','2026-05-07 20:00:00',36);
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
INSERT INTO `bid_transaction` VALUES ('995ed873-5084-418f-8942-bdcb906ad320','d6e71dcb-d078-4190-94bd-58d7038c0857','trungbeo',16000000,'2026-05-13 15:54:27.000000','MANUAL'),('9c5e920e-54c5-4fdc-bb74-b6c1c6f8be4e','d6e71dcb-d078-4190-94bd-58d7038c0857','trungbeo',100000,'2026-05-13 15:55:20.000000','MANUAL'),('9c5e920e-54c5-4fdc-bb74-b6c1c6f8be4e','d6e71dcb-d078-4190-94bd-58d7038c0857','trungbeo',170000,'2026-05-13 15:55:28.000000','MANUAL'),('995ed873-5084-418f-8942-bdcb906ad320','d6e71dcb-d078-4190-94bd-58d7038c0857','trungbeo',17000000,'2026-05-13 17:11:40.000000','MANUAL'),('995ed873-5084-418f-8942-bdcb906ad320','d6e71dcb-d078-4190-94bd-58d7038c0857','trungbeo',20000000,'2026-05-13 17:33:57.000000','MANUAL');
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
INSERT INTO `bidder` VALUES ('394f3d4d-4c98-4411-bf2e-bc0a887a3587',100000000),('8862d63e-f4ec-4b6e-9473-0137776d3be5',1000000),('b58ac62f-4a9f-4e4c-9bfb-5fa663c38bf5',1000000),('d6e71dcb-d078-4190-94bd-58d7038c0857',165884696),('dd9ef995-0125-4060-8a80-b8f1895dd875',0);
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
INSERT INTO `electronic` VALUES ('0f898bfd-c02f-490c-af40-69763e6e74d8',1,36);
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
INSERT INTO `entity` VALUES ('0f898bfd-c02f-490c-af40-69763e6e74d8','Beatrice'),('300b3417-94ca-4ded-ac87-8e060366a281','Beako plushy'),('343e6724-7efb-4958-a05d-9a815ab6bf08','Bã mía loại 3'),('394f3d4d-4c98-4411-bf2e-bc0a887a3587','hà anh'),('5903b82c-e5e0-426c-9ecd-e791edabfc0d','Tran Thi B'),('995ed873-5084-418f-8942-bdcb906ad320','Sell Beako plushy'),('9c5e920e-54c5-4fdc-bb74-b6c1c6f8be4e','Shop Độ Mimi'),('b58ac62f-4a9f-4e4c-9bfb-5fa663c38bf5','Nguyen Van A'),('b89ed511-03a0-4522-a45f-b8186265ef51','Sell Beako'),('c79f0cf0-b48e-4dc6-8025-59f3eadb0974','Trung Lê'),('d6e71dcb-d078-4190-94bd-58d7038c0857','Trung Lê'),('dd9ef995-0125-4060-8a80-b8f1895dd875','nguyen duc tue');
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
INSERT INTO `item` VALUES ('0f898bfd-c02f-490c-af40-69763e6e74d8','c79f0cf0-b48e-4dc6-8025-59f3eadb0974','IDK','yet ser','ELECTRONIC'),('300b3417-94ca-4ded-ac87-8e060366a281','c79f0cf0-b48e-4dc6-8025-59f3eadb0974','Subaru','Don\'t let Emilia see this','ART'),('343e6724-7efb-4958-a05d-9a815ab6bf08','c79f0cf0-b48e-4dc6-8025-59f3eadb0974','Độ mimi','IU A DO MIXI','ART');
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
  `image_url` varchar(255) NOT NULL,
  PRIMARY KEY (`item_id`,`image_url`),
  CONSTRAINT `fk_itemimage_item` FOREIGN KEY (`item_id`) REFERENCES `item` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `item_image_url`
--

LOCK TABLES `item_image_url` WRITE;
/*!40000 ALTER TABLE `item_image_url` DISABLE KEYS */;
INSERT INTO `item_image_url` VALUES ('0f898bfd-c02f-490c-af40-69763e6e74d8','https://cdn.donmai.us/original/b1/a8/b1a861a2321d635e7a0d6e452730f9d5.jpg'),('300b3417-94ca-4ded-ac87-8e060366a281','https://i.pinimg.com/736x/f6/e9/c2/f6e9c2a7b911b6e64c60147e6789dcae.jpg'),('343e6724-7efb-4958-a05d-9a815ab6bf08','https://down-vn.img.susercontent.com/file/vn-11134207-7r98o-lvy3123l4xcp65');
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
INSERT INTO `seller` VALUES ('5903b82c-e5e0-426c-9ecd-e791edabfc0d','MB-9999888',0),('c79f0cf0-b48e-4dc6-8025-59f3eadb0974','00000001234',600000);
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
INSERT INTO `user` VALUES ('394f3d4d-4c98-4411-bf2e-bc0a887a3587','onion','123456','into@gmail.com','BIDDER'),('5903b82c-e5e0-426c-9ecd-e791edabfc0d','tranthib','abcdef','b@gmail.com','SELLER'),('b58ac62f-4a9f-4e4c-9bfb-5fa663c38bf5','nguyenvana','123456','a@gmail.com','BIDDER'),('c79f0cf0-b48e-4dc6-8025-59f3eadb0974','trunglee','trung2007','tuantrungbeo@gmail.com','SELLER'),('d6e71dcb-d078-4190-94bd-58d7038c0857','trungbeo','trung2007','tuantrungbeo@gmail.com','BIDDER'),('dd9ef995-0125-4060-8a80-b8f1895dd875','tuetistoof','tue10092007','tuenopro@gmail.com','BIDDER');
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
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-14  0:03:01
