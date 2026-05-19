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
INSERT INTO `art` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392',1),('2b628c25-a273-436b-99af-76ee0980df0a',1),('4560e136-4ac2-4c72-881b-2fd163e77bb9',0),('4d9d00c9-c30d-472d-ac50-9c5711b8432c',0),('9f6d6cbd-1253-4cbd-8df1-33bb487e9c5b',0),('c371600b-6af5-4672-9098-632e9c1d777c',0),('cdce54a8-7dc8-411d-b3a8-fbc304130cb0',0),('d59889f2-c31f-431d-a575-c2c9b529fdf7',1),('f2eb6323-e929-4749-b36a-d64166f5089c',1);
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
INSERT INTO `auction` VALUES ('46a05715-f3df-4b1d-aabc-c1f2b7f71362','CANCELED','dc35111b-344c-49ff-89ff-9426c50d3115','7f96a49c-fc5b-4c75-9044-1fbdaaf44490'),('4b42903f-d3eb-4552-b6d7-32e527cdfec2','FINISHED','8c73870a-ad7c-4b16-a9bd-4a44709fe494','cdce54a8-7dc8-411d-b3a8-fbc304130cb0'),('504e05dc-d70e-4465-950c-11a9a16655bb','CANCELED','dc35111b-344c-49ff-89ff-9426c50d3115','2b628c25-a273-436b-99af-76ee0980df0a'),('513d16af-2dde-4ea2-b3cc-bf018287ce74','RUNNING','dc35111b-344c-49ff-89ff-9426c50d3115','98da43dc-772d-4ecf-b787-b0ac133c6a46'),('516ccf3e-636e-4d4a-a943-fc0a1515e6e7','FINISHED','dc35111b-344c-49ff-89ff-9426c50d3115','9f6d6cbd-1253-4cbd-8df1-33bb487e9c5b'),('6c1da1ea-26e1-489f-905d-2754aa930c27','FINISHED','dc35111b-344c-49ff-89ff-9426c50d3115','c371600b-6af5-4672-9098-632e9c1d777c'),('89f4bdd4-8d31-42ea-a685-826489483fd4','FINISHED','dc35111b-344c-49ff-89ff-9426c50d3115','d59889f2-c31f-431d-a575-c2c9b529fdf7'),('9deaaa76-644e-4940-820d-7829fea0e351','FINISHED','dc35111b-344c-49ff-89ff-9426c50d3115','4560e136-4ac2-4c72-881b-2fd163e77bb9'),('a4fc27f0-0b5d-4812-9eda-4d70cf9590ee','FINISHED','8c73870a-ad7c-4b16-a9bd-4a44709fe494','4d9d00c9-c30d-472d-ac50-9c5711b8432c'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','FINISHED','dc35111b-344c-49ff-89ff-9426c50d3115','09a99fb0-64c7-47ef-9ee4-79415183b392'),('d369c0a6-1766-48e7-ac42-707c561c2895','FINISHED','dc35111b-344c-49ff-89ff-9426c50d3115','79f1a2db-c429-4cd4-aa90-1912915f85a7'),('d8005a28-5e7b-481b-96df-efa39e553ffd','FINISHED','dc35111b-344c-49ff-89ff-9426c50d3115','a9541de2-cc92-4b4d-b021-03ec54f1489e'),('e867b1d2-4b56-4507-bc14-5e080ab6ae0d','FINISHED','dc35111b-344c-49ff-89ff-9426c50d3115','f2eb6323-e929-4749-b36a-d64166f5089c');
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
INSERT INTO `auction_config` VALUES ('46a05715-f3df-4b1d-aabc-c1f2b7f71362',15000000,15000000,'2026-05-18 11:43:18','2026-05-19 12:00:00',36),('4b42903f-d3eb-4552-b6d7-32e527cdfec2',10000,5000,'2026-05-18 18:46:31','2026-05-18 19:30:00',36),('504e05dc-d70e-4465-950c-11a9a16655bb',12000,2000,'2026-05-18 14:37:28','2026-05-18 16:00:00',36),('513d16af-2dde-4ea2-b3cc-bf018287ce74',200000,50000,'2026-05-19 10:42:00','2026-05-22 14:00:00',36),('516ccf3e-636e-4d4a-a943-fc0a1515e6e7',200000,25000,'2026-05-17 00:00:00','2026-05-17 00:05:00',36),('6c1da1ea-26e1-489f-905d-2754aa930c27',1000000,200000,'2026-05-18 05:19:11','2026-05-18 05:25:00',36),('89f4bdd4-8d31-42ea-a685-826489483fd4',1000000,200000,'2026-05-18 05:41:08','2026-05-18 05:47:00',36),('9deaaa76-644e-4940-820d-7829fea0e351',125000,15000,'2026-05-16 17:11:11','2026-05-16 17:20:00',36),('a4fc27f0-0b5d-4812-9eda-4d70cf9590ee',10000,5000,'2026-05-18 17:15:26','2026-05-18 17:22:00',36),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3',400000,50000,'2026-05-14 01:10:10','2026-05-15 12:30:00',36),('d369c0a6-1766-48e7-ac42-707c561c2895',300000,50000,'2026-05-14 01:38:37','2026-05-19 14:30:00',36),('d8005a28-5e7b-481b-96df-efa39e553ffd',15000000,5000000,'2026-05-16 17:03:59','2026-05-16 18:00:00',36),('e867b1d2-4b56-4507-bc14-5e080ab6ae0d',15000000,20000,'2026-05-18 05:08:28','2026-05-18 07:00:00',36);
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
  `locked_balance` bigint NOT NULL DEFAULT '0',
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
INSERT INTO `bid_transaction` VALUES ('513d16af-2dde-4ea2-b3cc-bf018287ce74','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',250000,250000,'2026-05-19 20:30:14.023583','MANUAL'),('513d16af-2dde-4ea2-b3cc-bf018287ce74','9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo',300000,300000,'2026-05-19 20:30:33.622170','MANUAL'),('513d16af-2dde-4ea2-b3cc-bf018287ce74','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',350000,500000,'2026-05-19 20:30:47.301922','AUTO'),('513d16af-2dde-4ea2-b3cc-bf018287ce74','9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo',400000,400000,'2026-05-19 20:30:55.783298','MANUAL'),('513d16af-2dde-4ea2-b3cc-bf018287ce74','d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion',450000,500000,'2026-05-19 20:30:55.825064','AUTO');
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
INSERT INTO `bidder` VALUES ('1e0db604-0b82-4a00-8ce5-aafe9b58ff2e',1000000000,0),('9171ac52-84f8-4cf1-b248-e2cab9520dd0',1000000000,1850000),('d0ffa144-26f1-4f86-a877-ecc952e72c3d',18050000,500000);
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
INSERT INTO `electronic` VALUES ('98da43dc-772d-4ecf-b787-b0ac133c6a46',0,12);
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
INSERT INTO `entity` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392','Beako plushy'),('1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','Phong Nguyen'),('2b628c25-a273-436b-99af-76ee0980df0a','none'),('4560e136-4ac2-4c72-881b-2fd163e77bb9','croptop'),('46a05715-f3df-4b1d-aabc-c1f2b7f71362','ĐẤU THẦY DỰ ÁN NHÀ SÁCH'),('4b42903f-d3eb-4552-b6d7-32e527cdfec2','hành'),('4d9d00c9-c30d-472d-ac50-9c5711b8432c','onion'),('504e05dc-d70e-4465-950c-11a9a16655bb','test cancel'),('513d16af-2dde-4ea2-b3cc-bf018287ce74','Game'),('516ccf3e-636e-4d4a-a943-fc0a1515e6e7','babytee'),('6c1da1ea-26e1-489f-905d-2754aa930c27','starry night'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','TETO'),('7f96a49c-fc5b-4c75-9044-1fbdaaf44490','Khu đất ngàn vàng'),('89f4bdd4-8d31-42ea-a685-826489483fd4','starry night'),('8c73870a-ad7c-4b16-a9bd-4a44709fe494','hành củ'),('9171ac52-84f8-4cf1-b248-e2cab9520dd0','Trung Lee'),('98da43dc-772d-4ecf-b787-b0ac133c6a46','Silksong'),('9deaaa76-644e-4940-820d-7829fea0e351','Croptop Denim'),('9f6d6cbd-1253-4cbd-8df1-33bb487e9c5b','babytee'),('a4fc27f0-0b5d-4812-9eda-4d70cf9590ee','onion'),('a9541de2-cc92-4b4d-b021-03ec54f1489e','Subaru WRX STI S210'),('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1','Admin Twe'),('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2','Admin Onion'),('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3','Admin KPhong'),('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4','Admin Trung'),('c371600b-6af5-4672-9098-632e9c1d777c','starry night'),('ccd6339f-e08e-4f05-bcbd-a0b0983cd3d3','bán loli'),('cdce54a8-7dc8-411d-b3a8-fbc304130cb0','onion'),('d0ffa144-26f1-4f86-a877-ecc952e72c3d','hà anh'),('d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO'),('d59889f2-c31f-431d-a575-c2c9b529fdf7','starry'),('d8005a28-5e7b-481b-96df-efa39e553ffd','Bán xe'),('dc35111b-344c-49ff-89ff-9426c50d3115','trung lê'),('e867b1d2-4b56-4507-bc14-5e080ab6ae0d','Lên cho ae lô đát gần BK'),('ef532146-0c6f-4637-ae82-f35612861d6d','TETO'),('f2eb6323-e929-4749-b36a-d64166f5089c','đất');
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
INSERT INTO `item` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392','dc35111b-344c-49ff-89ff-9426c50d3115','internet','mỗi người nên có ít nhất 1 bé','ART'),('2b628c25-a273-436b-99af-76ee0980df0a','dc35111b-344c-49ff-89ff-9426c50d3115','admin','none','ART'),('4560e136-4ac2-4c72-881b-2fd163e77bb9','dc35111b-344c-49ff-89ff-9426c50d3115','CORTIS','nothing beats a jet2 holiday','ART'),('4d9d00c9-c30d-472d-ac50-9c5711b8432c','8c73870a-ad7c-4b16-a9bd-4a44709fe494','ozone','nothing','ART'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','dc35111b-344c-49ff-89ff-9426c50d3115','INTERNET','Ai cũng cần 1 con','VEHICLE'),('7f96a49c-fc5b-4c75-9044-1fbdaaf44490','dc35111b-344c-49ff-89ff-9426c50d3115','earth','yetj sơ','VEHICLE'),('98da43dc-772d-4ecf-b787-b0ac133c6a46','dc35111b-344c-49ff-89ff-9426c50d3115','team cherry','none','ELECTRONIC'),('9f6d6cbd-1253-4cbd-8df1-33bb487e9c5b','dc35111b-344c-49ff-89ff-9426c50d3115','judydoll','nothing','ART'),('a9541de2-cc92-4b4d-b021-03ec54f1489e','dc35111b-344c-49ff-89ff-9426c50d3115','Subaru','The Subaru WRX STI is a high-performance, rally-inspired sports sedan featuring a turbocharged 2.5-liter boxer engine, symmetrical all-wheel drive, and STI-tuned suspension for exceptional handling and driving excitement.\n\nOverview\nThe Subaru WRX STI is a performance variant of the Subaru WRX, developed by Subaru Tecnica International (STI) between 2004 and 2021, succeeding the Impreza WRX STI. It is renowned for its rally heritage, aggressive styling, and track-capable performance, making it a favorite among enthusiasts seeking a sporty, all-weather sedan. \n\nEngine and Performance\nThe WRX STI is powered by a 2.5-liter turbocharged horizontally opposed four-cylinder (boxer) engine, producing 310 horsepower at 6,000 rpm and 290 lb-ft of torque at 3,600 rpm. This engine is paired with a six-speed manual transmission and Subaru’s symmetrical all-wheel-drive system, providing excellent traction, cornering stability, and responsive handling. Performance features include Brembo four-piston brakes, Bilstein performance dampers, and a double wishbone rear suspension combined with MacPherson struts in the front. The WRX STI also incorporates Active Torque Vectoring to dynamically distribute power between wheels for optimal cornering grip. \n\nDimensions and Handling\nThe WRX STI measures approximately 179.9 inches in length, 70.7 inches in width, and 58.1 inches in height, with a wheelbase of 104.3 inches and a curb weight around 3,300–3,400 pounds. Its ground clearance of 4.9 inches balances sporty handling with moderate off-road capability. The suspension and chassis are reinforced for rigidity, enhancing stability during aggressive driving. \n\nInterior and Features\nInside, the WRX STI features Recaro sport seats, a driver-focused cockpit, and a multifunction display. STI-specific touches include a unique gauge cluster, shifter knob, and red-accented interior trim. Advanced safety features include Subaru’s EyeSight system with adaptive cruise control and lane-keep assist. \n\nSpecial Editions and Updates\nOver the years, the WRX STI received several updates, including the 2018 Type RA and 2019 S209, which offered increased horsepower up to 341 hp and torque of 330 lb-ft. The EJ25 Final Edition marked the end of the EJ engine era, featuring BBS 19-inch gold forged wheels, Brembo calipers, and a numbered badge, with only 75 units produced globally. STI also offers tuning accessories and limited editions like the STI S210 prototype in Japan.\n\nLegacy\nThe WRX STI is celebrated for its rally-inspired performance, combining turbocharged power, precise handling, and all-wheel-drive capability. Its design and engineering reflect Subaru’s motorsport heritage, making it a standout in the sports sedan segment.','VEHICLE'),('c371600b-6af5-4672-9098-632e9c1d777c','dc35111b-344c-49ff-89ff-9426c50d3115','van gogh','nnnnn','ART'),('cdce54a8-7dc8-411d-b3a8-fbc304130cb0','8c73870a-ad7c-4b16-a9bd-4a44709fe494','o3','nothing','ART'),('d59889f2-c31f-431d-a575-c2c9b529fdf7','dc35111b-344c-49ff-89ff-9426c50d3115','van gogh','nothing','ART'),('ef532146-0c6f-4637-ae82-f35612861d6d','dc35111b-344c-49ff-89ff-9426c50d3115','internet','TETO123','ELECTRONIC'),('f2eb6323-e929-4749-b36a-d64166f5089c','dc35111b-344c-49ff-89ff-9426c50d3115','vũ trụ','bán đất let sờ go','ART');
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
INSERT INTO `item_image_url` VALUES ('09a99fb0-64c7-47ef-9ee4-79415183b392','https://th.bing.com/th/id/OIP.8v16bx0DQxWJFDZbKeOrpwHaHh?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('ef532146-0c6f-4637-ae82-f35612861d6d','https://th.bing.com/th/id/OIP.hJzaTf6QcFEaBgShJ8EIcgHaHa?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','https://thuvienmeme.com/wp-content/uploads/2024/04/meme-sieu-nhan-ngoi-chong-cam-hanh-phuc.jpg'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','https://th.bing.com/th/id/OIP.YovTjY1b8reVgfROUVVubwHaHa?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('79f1a2db-c429-4cd4-aa90-1912915f85a7','https://th.bing.com/th/id/OIP.stBsb0sfY-NXgqF8hVWgCAHaHa?o=7rm=3&rs=1&pid=ImgDetMain&o=7&rm=3'),('a9541de2-cc92-4b4d-b021-03ec54f1489e','https://www.carexplore.com.au/content/images/size/w1200/format/webp/2025/01/Subaru-WRX-STI-S210-3.png'),('a9541de2-cc92-4b4d-b021-03ec54f1489e','https://www.carexplore.com.au/content/images/size/w1000/2025/01/Subaru-WRX-STI-S210.png'),('a9541de2-cc92-4b4d-b021-03ec54f1489e','https://www.carexplore.com.au/content/images/size/w1000/2025/01/Subaru-WRX-STI-S210-2.png'),('4560e136-4ac2-4c72-881b-2fd163e77bb9','https://emix.vn/ao-jean-croptop-tay-ngan-phoi-cuc-noi-bat-kem-2-tui-nho-ca-tinh-aojeancrt7019-i2983'),('9f6d6cbd-1253-4cbd-8df1-33bb487e9c5b','https://cdn.donmai.us/original/b1/a8/b1a861a2321d635e7a0d6e452730f9d5.jpg'),('f2eb6323-e929-4749-b36a-d64166f5089c','blob:https://www.messenger.com/93a1dbfd-510b-43f1-8eb0-3001df34ccab'),('c371600b-6af5-4672-9098-632e9c1d777c','https://cdn.donmai.us/original/b1/a8/b1a861a2321d635e7a0d6e452730f9d5.jpg'),('d59889f2-c31f-431d-a575-c2c9b529fdf7','https://cdn.donmai.us/original/b1/a8/b1a861a2321d635e7a0d6e452730f9d5.jpg'),('7f96a49c-fc5b-4c75-9044-1fbdaaf44490','https://i.pinimg.com/736x/b3/4b/c8/b34bc8bd20cbe72fdc80906ce4b9ff97.jpg'),('2b628c25-a273-436b-99af-76ee0980df0a','https://i1-c.pinimg.com/1200x/33/3d/bc/333dbc5a931db46c182a1616e1fb3106.jpg'),('4d9d00c9-c30d-472d-ac50-9c5711b8432c','https://cdn.donmai.us/original/b1/a8/b1a861a2321d635e7a0d6e452730f9d5.jpg'),('cdce54a8-7dc8-411d-b3a8-fbc304130cb0','https://cdn.donmai.us/original/b1/a8/b1a861a2321d635e7a0d6e452730f9d5.jpg'),('98da43dc-772d-4ecf-b787-b0ac133c6a46','https://images.squarespace-cdn.com/content/v1/606d4bb793879d12d807d4c8/1617942960534-H22JN6ZUWLYLG6E7MAEE/logo_main.png?format=2500w');
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
INSERT INTO `notification` VALUES ('0c07e937-5e88-4f50-a296-8c8fcaabf3f6','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',2300000,NULL,0,'2026-05-18 22:14:47'),('0e3bb445-0723-4de1-b037-43e5b4a57369','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1750000,NULL,0,'2026-05-18 16:54:24'),('1488afc3-d432-41af-9db8-a38e891ff197','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',2460000,NULL,0,'2026-05-18 22:39:35'),('18ec2e94-73cc-4e3a-86f7-2a4317f840ad','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',500000,NULL,0,'2026-05-19 12:50:07'),('1b31ad51-287d-4cb0-9d15-fe5ed6a7cea4','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',3010000,NULL,0,'2026-05-18 23:36:00'),('20e0f90a-1d87-4566-904d-1c5b8d0350d5','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',760000,NULL,0,'2026-05-18 02:41:47'),('243fe762-449f-41a5-b974-9c6fb0a8287f','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',550000,NULL,0,'2026-05-19 12:51:15'),('291ea94c-6082-46cb-9375-09cde01b6b82','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1300000,NULL,0,'2026-05-18 04:40:19'),('294e40cf-eba2-41d2-ba26-b9ecca282429','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',400000,NULL,0,'2026-05-19 11:28:42'),('2e46edf4-b147-41fe-a45b-56f8b7231aa2','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',3100000,NULL,0,'2026-05-19 10:17:24'),('3293616e-007a-4e36-a50b-f1ac2d9b9832','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1900000,NULL,0,'2026-05-18 17:53:12'),('41030019-8315-41b4-94d3-240134bf4cf1','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1600000,NULL,0,'2026-05-18 04:57:07'),('471de7ac-769c-49a8-a4e4-8fee8386403c','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1200000,NULL,0,'2026-05-18 04:39:06'),('497527fc-07ef-4e2c-8966-54c35c6fb436','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',938000,NULL,0,'2026-05-18 04:11:51'),('4b3dded0-501b-4cc9-a59f-b558d5595ebe','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',2350000,NULL,0,'2026-05-18 22:14:49'),('56816034-b74c-4c4d-a7ee-a616255dbb07','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',2100000,NULL,0,'2026-05-18 17:53:40'),('62a09c5d-27d1-460e-a52b-548322014d77','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',988000,NULL,0,'2026-05-18 04:15:07'),('6a194ff3-6d8e-4714-b874-cc6344b88e65','d0ffa144-26f1-4f86-a877-ecc952e72c3d','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',600001,NULL,0,'2026-05-17 00:30:42'),('70a04c43-caba-42d4-9a8a-3df3afdadb06','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1350000,NULL,0,'2026-05-18 04:42:19'),('72e16d07-ea89-489b-b791-f66db5c2b6ca','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',710000,NULL,0,'2026-05-18 02:41:46'),('74b94565-9b3a-4b88-ac0d-df9f0eab4603','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',888000,NULL,0,'2026-05-18 02:46:09'),('7d622de8-e8a4-4730-8c6c-9a2c028da1ac','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1800000,NULL,0,'2026-05-18 16:54:42'),('83fdec43-b008-4da8-a772-b1affab7eec9','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',2150000,NULL,0,'2026-05-18 17:53:40'),('8a6c4d2c-be8b-40c6-9152-d1022f8abbfa','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1400000,NULL,0,'2026-05-18 04:42:35'),('8d3fc003-88a2-4408-b03a-19f4658b77c0','9171ac52-84f8-4cf1-b248-e2cab9520dd0','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',600001,NULL,0,'2026-05-17 00:30:41'),('90c2f4e6-b43b-4ae0-8153-c35fd0e207e4','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',650001,NULL,0,'2026-05-18 02:41:13'),('94b3ada8-a8f4-4dd0-9ed4-b5c5ee84cce2','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1150000,NULL,0,'2026-05-18 04:18:59'),('98098cde-a057-4fc9-a516-ecc045906430','d0ffa144-26f1-4f86-a877-ecc952e72c3d','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',450000,NULL,0,'2026-05-19 12:39:47'),('9cc1ddfa-6f74-40b9-898c-58e5360ce2b2','d0ffa144-26f1-4f86-a877-ecc952e72c3d','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1700000,NULL,0,'2026-05-18 16:45:31'),('af7e5f9e-c43a-4ca1-ae8e-53f15ecca0bb','d0ffa144-26f1-4f86-a877-ecc952e72c3d','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',550001,NULL,0,'2026-05-16 15:42:00'),('b098eec1-fdcb-440e-9828-ddd330ed46cc','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1100000,NULL,0,'2026-05-18 04:18:57'),('b241711f-a09d-4e4b-acb2-a1dad1b37d54','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',2250000,NULL,0,'2026-05-18 22:14:29'),('b5853389-63ac-4115-99e9-e10d40bf288f','d0ffa144-26f1-4f86-a877-ecc952e72c3d','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',3010000,NULL,0,'2026-05-18 23:36:00'),('ba65adcc-8148-4f50-bbac-c74e2e8a292c','9171ac52-84f8-4cf1-b248-e2cab9520dd0','ENDED','516ccf3e-636e-4d4a-a943-fc0a1515e6e7','babytee',200000,'No bids placed',0,'2026-05-17 00:05:01'),('bb4cb891-bbd7-48b2-9d29-7288f8341f8b','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',2400000,NULL,0,'2026-05-18 22:39:34'),('bb5857ea-b88c-4f5b-8eb3-b2e784f5eeb8','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',450000,NULL,0,'2026-05-19 12:39:47'),('ce0af887-0d43-4b78-aa80-f398f63fe4a3','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',2960000,NULL,0,'2026-05-18 22:40:33'),('d8ac400c-9c64-422f-a1af-0aa2f90db27e','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',2200000,NULL,0,'2026-05-18 21:06:25'),('d927e04d-5330-4c36-ae90-ef3e06223895','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',600000,NULL,0,'2026-05-19 12:51:35'),('e24a0b90-18b3-449f-9293-bbff41ee6c1d','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1850000,NULL,0,'2026-05-18 16:54:43'),('e7fb67e7-7182-4982-9fec-a91ebb913e07','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',1500000,NULL,0,'2026-05-18 04:47:59'),('f142aaaf-fff5-4eb7-a0d9-4c9bf88dd016','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',550001,NULL,0,'2026-05-16 15:42:00'),('f61172db-c7f9-4850-af66-91e292c130de','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','OUTBID','d369c0a6-1766-48e7-ac42-707c561c2895','Bán TETO',350000,NULL,0,'2026-05-19 11:27:47');
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
  `pending_balance` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_seller_user` FOREIGN KEY (`id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `seller`
--

LOCK TABLES `seller` WRITE;
/*!40000 ALTER TABLE `seller` DISABLE KEYS */;
INSERT INTO `seller` VALUES ('8c73870a-ad7c-4b16-a9bd-4a44709fe494','0819908626',0,0),('dc35111b-344c-49ff-89ff-9426c50d3115','123456',1200000,250000);
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
INSERT INTO `user` VALUES ('1e0db604-0b82-4a00-8ce5-aafe9b58ff2e','Kphong','123456','123@gmail.com','BIDDER'),('8c73870a-ad7c-4b16-a9bd-4a44709fe494','ozone','123456','intothe@gmail.com','SELLER'),('9171ac52-84f8-4cf1-b248-e2cab9520dd0','trungbeo','trung2007','ainfo@gmail.com','BIDDER'),('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1','adminTW','admin123','admin1@gmail.com','ADMIN'),('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2','adminHA','admin123','admin2@gmail.com','ADMIN'),('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3','adminKP','admin123','admin3@gmail.com','ADMIN'),('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4','adminTT','admin123','admin4@gmail.com','ADMIN'),('d0ffa144-26f1-4f86-a877-ecc952e72c3d','onion','123456','into@gmail.com','BIDDER'),('dc35111b-344c-49ff-89ff-9426c50d3115','trunglee','trung2007','ainfo@gmail.com','SELLER');
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
INSERT INTO `vehicle` VALUES ('79f1a2db-c429-4cd4-aa90-1912915f85a7',0,36),('7f96a49c-fc5b-4c75-9044-1fbdaaf44490',0,36),('a9541de2-cc92-4b4d-b021-03ec54f1489e',0,60);
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
INSERT INTO `watchlist` VALUES ('d369c0a6-1766-48e7-ac42-707c561c2895','1e0db604-0b82-4a00-8ce5-aafe9b58ff2e'),('516ccf3e-636e-4d4a-a943-fc0a1515e6e7','9171ac52-84f8-4cf1-b248-e2cab9520dd0'),('516ccf3e-636e-4d4a-a943-fc0a1515e6e7','d0ffa144-26f1-4f86-a877-ecc952e72c3d'),('6c1da1ea-26e1-489f-905d-2754aa930c27','d0ffa144-26f1-4f86-a877-ecc952e72c3d'),('89f4bdd4-8d31-42ea-a685-826489483fd4','d0ffa144-26f1-4f86-a877-ecc952e72c3d'),('d369c0a6-1766-48e7-ac42-707c561c2895','d0ffa144-26f1-4f86-a877-ecc952e72c3d');
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

-- Dump completed on 2026-05-19 20:32:34
