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
INSERT INTO `art` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392',1),('4560e136-4ac2-4c72-881b-2fd163e77bb9',0);
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
INSERT INTO `auction` VALUES ('9deaaa76-644e-4940-820d-7829fea0e351','RUNNING','dc35111b-344c-49ff-89ff-9426c50d3115','4560e136-4ac2-4c72-881b-2fd163e77bb9'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','FINISHED','dc35111b-344c-49ff-89ff-9426c50d3115','09a99fb0-64c7-47ef-9ee4-79415183b392'),('d369c0a6-1766-48e7-ac42-707c561c2895','RUNNING','dc35111b-344c-49ff-89ff-9426c50d3115','79f1a2db-c429-4cd4-aa90-1912915f85a7'),('d8005a28-5e7b-481b-96df-efa39e553ffd','FINISHED','dc35111b-344c-49ff-89ff-9426c50d3115','a9541de2-cc92-4b4d-b021-03ec54f1489e');
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
INSERT INTO `auction_config` VALUES ('9deaaa76-644e-4940-820d-7829fea0e351',125000,15000,'2026-05-16 17:11:11','2026-05-16 17:20:00',36),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3',400000,50000,'2026-05-14 01:10:10','2026-05-15 12:30:00',36),('d369c0a6-1766-48e7-ac42-707c561c2895',300000,50000,'2026-05-14 01:38:37','2026-05-19 14:30:00',36),('d8005a28-5e7b-481b-96df-efa39e553ffd',15000000,5000000,'2026-05-16 17:03:59','2026-05-16 18:00:00',36);
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
INSERT INTO `bid_transaction` VALUES ('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',450000,'2026-05-14 01:12:01.163345','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',600000,'2026-05-14 01:20:09.213241','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo',1500000,'2026-05-14 01:21:35.638500','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',1600000,'2026-05-14 01:33:08.420175','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',1700000,'2026-05-14 01:35:54.159168','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',1800000,'2026-05-14 01:36:02.577750','MANUAL'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo',3500000,'2026-05-14 01:36:45.867124','MANUAL'),('d369c0a6-1766-48e7-ac42-707c561c2895','9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo',400000,'2026-05-14 01:40:10.974869','MANUAL'),('d369c0a6-1766-48e7-ac42-707c561c2895','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','Kphong',500000,'2026-05-16 15:15:59.163865','MANUAL'),('d369c0a6-1766-48e7-ac42-707c561c2895','9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo',550001,'2026-05-16 15:41:59.464996','MANUAL'),('9deaaa76-644e-4940-820d-7829fea0e351','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',140000,'2026-05-16 17:12:12.475211','MANUAL'),('9deaaa76-644e-4940-820d-7829fea0e351','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',160000,'2026-05-16 18:06:22.273066','MANUAL');
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
  `locked_balance` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_bidder_user` FOREIGN KEY (`id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bidder`
--

LOCK TABLES `bidder` WRITE;
/*!40000 ALTER TABLE `bidder` DISABLE KEYS */;
INSERT INTO `bidder` VALUES ('1e0db604-0b82-4a00-8ce5-aafe9b58ff2e',1000000000,0),('9171ac52-84f8-4cf1-b248-e2cab9520dd0',1000000000,0),('d0ffa144-26f1-4f86-a877-ecc952e72c3d',10450000,0);
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
INSERT INTO `entity` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392','Beako plushy'),('1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','Phong Nguyen'),('4560e136-4ac2-4c72-881b-2fd163e77bb9','croptop'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','TETO'),('9171ac52-84f8-4cf1-b248-e2cab9520dd0','Trung Lee'),('9deaaa76-644e-4940-820d-7829fea0e351','Croptop Denim'),('a9541de2-cc92-4b4d-b021-03ec54f1489e','Subaru WRX STI S210'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','bán loli'),('d0ffa144-26f1-4f86-a877-ecc952e72c3d','hà anh'),('d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO'),('d8005a28-5e7b-481b-96df-efa39e553ffd','Bán xe'),('dc35111b-344c-49ff-89ff-9426c50d3115','trung lê'),('ef532146-0c6f-4637-ae82-f35612861d6d','TETO');
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
INSERT INTO `item` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392','dc35111b-344c-49ff-89ff-9426c50d3115','internet','mỗi người nên có ít nhất 1 bé','ART'),('4560e136-4ac2-4c72-881b-2fd163e77bb9','dc35111b-344c-49ff-89ff-9426c50d3115','CORTIS','nothing beats a jet2 holiday','ART'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','dc35111b-344c-49ff-89ff-9426c50d3115','INTERNET','Ai cũng cần 1 con','VEHICLE'),('a9541de2-cc92-4b4d-b021-03ec54f1489e','dc35111b-344c-49ff-89ff-9426c50d3115','Subaru','The Subaru WRX STI is a high-performance, rally-inspired sports sedan featuring a turbocharged 2.5-liter boxer engine, symmetrical all-wheel drive, and STI-tuned suspension for exceptional handling and driving excitement.\n\nOverview\nThe Subaru WRX STI is a performance variant of the Subaru WRX, developed by Subaru Tecnica International (STI) between 2004 and 2021, succeeding the Impreza WRX STI. It is renowned for its rally heritage, aggressive styling, and track-capable performance, making it a favorite among enthusiasts seeking a sporty, all-weather sedan. \n\nEngine and Performance\nThe WRX STI is powered by a 2.5-liter turbocharged horizontally opposed four-cylinder (boxer) engine, producing 310 horsepower at 6,000 rpm and 290 lb-ft of torque at 3,600 rpm. This engine is paired with a six-speed manual transmission and Subaru’s symmetrical all-wheel-drive system, providing excellent traction, cornering stability, and responsive handling. Performance features include Brembo four-piston brakes, Bilstein performance dampers, and a double wishbone rear suspension combined with MacPherson struts in the front. The WRX STI also incorporates Active Torque Vectoring to dynamically distribute power between wheels for optimal cornering grip. \n\nDimensions and Handling\nThe WRX STI measures approximately 179.9 inches in length, 70.7 inches in width, and 58.1 inches in height, with a wheelbase of 104.3 inches and a curb weight around 3,300–3,400 pounds. Its ground clearance of 4.9 inches balances sporty handling with moderate off-road capability. The suspension and chassis are reinforced for rigidity, enhancing stability during aggressive driving. \n\nInterior and Features\nInside, the WRX STI features Recaro sport seats, a driver-focused cockpit, and a multifunction display. STI-specific touches include a unique gauge cluster, shifter knob, and red-accented interior trim. Advanced safety features include Subaru’s EyeSight system with adaptive cruise control and lane-keep assist. \n\nSpecial Editions and Updates\nOver the years, the WRX STI received several updates, including the 2018 Type RA and 2019 S209, which offered increased horsepower up to 341 hp and torque of 330 lb-ft. The EJ25 Final Edition marked the end of the EJ engine era, featuring BBS 19-inch gold forged wheels, Brembo calipers, and a numbered badge, with only 75 units produced globally. STI also offers tuning accessories and limited editions like the STI S210 prototype in Japan.\n\nLegacy\nThe WRX STI is celebrated for its rally-inspired performance, combining turbocharged power, precise handling, and all-wheel-drive capability. Its design and engineering reflect Subaru’s motorsport heritage, making it a standout in the sports sedan segment.','VEHICLE'),('ef532146-0c6f-4637-ae82-f35612861d6d','dc35111b-344c-49ff-89ff-9426c50d3115','internet','TETO123','ELECTRONIC');
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
INSERT INTO `item_image_url` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392','https://th.bing.com/th/id/OIP.8v16bx0DQxWJFDZbKeOrpwHaHh?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('ef532146-0c6f-4637-ae82-f35612861d6d','https://th.bing.com/th/id/OIP.hJzaTf6QcFEaBgShJ8EIcgHaHa?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','https://thuvienmeme.com/wp-content/uploads/2024/04/meme-sieu-nhan-ngoi-chong-cam-hanh-phuc.jpg'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','https://th.bing.com/th/id/OIP.YovTjY1b8reVgfROUVVubwHaHa?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','https://th.bing.com/th/id/OIP.stBsb0sfY-NXgqF8hVWgCAHaHa?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('a9541de2-cc92-4b4d-b021-03ec54f1489e','https://www.carexplore.com.au/content/images/size/w1200/format/webp/2025/01/Subaru-WRX-STI-S210-3.png'),('a9541de2-cc92-4b4d-b021-03ec54f1489e','https://www.carexplore.com.au/content/images/size/w1000/2025/01/Subaru-WRX-STI-S210.png'),('a9541de2-cc92-4b4d-b021-03ec54f1489e','https://www.carexplore.com.au/content/images/size/w1000/2025/01/Subaru-WRX-STI-S210-2.png'),('4560e136-4ac2-4c72-881b-2fd163e77bb9','https://emix.vn/ao-jean-croptop-tay-ngan-phoi-cuc-noi-bat-kem-2-tui-nho-ca-tinh-aojeancrt7019-i2983');
/*!40000 ALTER TABLE `item_image_url` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `type` varchar(20) NOT NULL,
  `auction_id` varchar(36) NOT NULL,
  `auction_name` varchar(255) NOT NULL,
  `price` bigint NOT NULL,
  `winner` varchar(100) DEFAULT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_unread` (`user_id`,`is_read`),
  CONSTRAINT `notification_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification`
--

LOCK TABLES `notification` WRITE;
/*!40000 ALTER TABLE `notification` DISABLE KEYS */;
INSERT INTO `notification` VALUES ('af7e5f9e-c43a-4ca1-ae8e-53f15ecca0bb','d0ffa144-26f1-4f86-a877-ecc952e72c3d','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',550001,NULL,0,'2026-05-16 15:42:00'),('f142aaaf-fff5-4eb7-a0d9-4c9bf88dd016','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',550001,NULL,0,'2026-05-16 15:42:00');
/*!40000 ALTER TABLE `notification` ENABLE KEYS */;
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
INSERT INTO `user` VALUES ('1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','Kphong','123456','123@gmail.com','BIDDER'),('9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo','trung2007','ainfo@gmail.com','BIDDER'),('d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion','123456','into@gmail.com','BIDDER'),('dc35111b-344c-49ff-89ff-9426c50d3115','trunglee','trung2007','ainfo@gmail.com','SELLER');
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
INSERT INTO `vehicle` VALUES ('79f1a2db-c429-4cd4-aa90-1912915f85a7',0,36),('a9541de2-cc92-4b4d-b021-03ec54f1489e',0,60);
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
INSERT INTO `watchlist` VALUES ('d369c0a6-1766-48e7-ac42-707c561c2895','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e'),('d369c0a6-1766-48e7-ac42-707c561c2895','9171ac52-84f8-4cf1-b248-e2cab9520dd0'),('9deaaa76-644e-4940-820d-7829fea0e351','d0ffa144-26f1-4f86-a877-ecc952e72c3d'),('d369c0a6-1766-48e7-ac42-707c561c2895','d0ffa144-26f1-4f86-a877-ecc952e72c3d');
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

-- Dump completed on 2026-05-16 23:44:53
