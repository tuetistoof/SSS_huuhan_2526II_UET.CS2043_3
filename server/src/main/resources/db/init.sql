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
INSERT INTO `admin` VALUES ('000000000000000000000000000000000001'),('000000000000000000000000000000000002'),('000000000000000000000000000000000003'),('000000000000000000000000000000000004');
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
INSERT INTO `art` VALUES ('2fd3bcb7-a4c7-40d6-a896-10e1eb81bf07',1),('79c175c3-2050-4798-a1cc-df156d166496',1),('84308354-5686-4896-81f5-a7fbe7433882',1),('b34f3dde-9d37-4fd6-a62c-311b8f1a5494',0),('e6d9cdd0-2f14-4bfa-9691-2b6a40e00def',0),('f917243a-4d5f-40c4-baf5-8bf57f7bbbe8',1);
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
INSERT INTO `auction` VALUES ('10b454c4-92e7-4673-aaca-ae9e81e69997','RUNNING','919d2586-bed7-4b3d-8628-1d8f7d91398c','b34f3dde-9d37-4fd6-a62c-311b8f1a5494'),('3d948586-c722-41c1-a953-793711627d12','FINISHED','919d2586-bed7-4b3d-8628-1d8f7d91398c','b3a3f83a-f0a6-40e0-9b2a-27e79c760d4b'),('62741609-feac-40e5-9eb8-dacb4c276a74','FINISHED','919d2586-bed7-4b3d-8628-1d8f7d91398c','a7b52af6-0322-4c25-9cb0-85b748ea4689'),('7b6094f0-693f-486e-ad49-952da730384d','FINISHED','919d2586-bed7-4b3d-8628-1d8f7d91398c','0d26ddb6-8a25-45eb-86f8-cab200ddc4b2'),('80a85000-667a-43dc-8958-28ef8389168a','OPEN','919d2586-bed7-4b3d-8628-1d8f7d91398c','dcddfe84-da38-4eea-a458-5fd8ca3b2f63'),('90632913-1404-48f4-b694-0a02efa11d4c','FINISHED','8bdb7ff8-c804-4514-8f49-8500f47df8e8','eb93690f-942b-46e4-bba0-8a18b49a11c2'),('99ca13c5-b07f-46d2-9a62-fb5475014da8','FINISHED','919d2586-bed7-4b3d-8628-1d8f7d91398c','2fd3bcb7-a4c7-40d6-a896-10e1eb81bf07'),('abb41140-35f7-48a7-8fd9-216afdd28dbd','OPEN','919d2586-bed7-4b3d-8628-1d8f7d91398c','f04fe671-9b40-499e-b7d6-5ca430b440ff'),('c1a15c23-d100-4473-9afe-65aa388d8f8f','FINISHED','919d2586-bed7-4b3d-8628-1d8f7d91398c','36575a93-afc2-4976-bebf-7a5ab8e4f9d6'),('c4b2cab1-64d9-43c4-8093-72c074252ffd','FINISHED','919d2586-bed7-4b3d-8628-1d8f7d91398c','57978ce8-e615-4670-a8a0-785780e44389'),('c9a34244-4a4c-49f4-8fec-82ca9af2f50b','RUNNING','919d2586-bed7-4b3d-8628-1d8f7d91398c','84308354-5686-4896-81f5-a7fbe7433882'),('cf5272f9-dff2-4833-a331-e5946f11f355','CANCELED','8bdb7ff8-c804-4514-8f49-8500f47df8e8','79c175c3-2050-4798-a1cc-df156d166496'),('dceb6cc5-d7e6-4e4f-886b-75ac4df1ff1f','FINISHED','919d2586-bed7-4b3d-8628-1d8f7d91398c','e6d9cdd0-2f14-4bfa-9691-2b6a40e00def'),('e6a1f0d1-32c1-4e84-b84b-da45e2238069','CANCELED','919d2586-bed7-4b3d-8628-1d8f7d91398c','7e78f891-d4e1-4cbd-a791-c2dc7a0b5e74'),('ec70ad7b-df96-45a2-8c5e-9caad0a93dd4','FINISHED','919d2586-bed7-4b3d-8628-1d8f7d91398c','5bff5db0-e229-4a68-8381-586bf48e006b'),('f95bd70e-9bd0-4365-9859-255c2be50855','FINISHED','919d2586-bed7-4b3d-8628-1d8f7d91398c','229bff7f-428b-4978-bebe-6239be4eefb4'),('fec278dc-a9d0-4ece-9bbe-14b16688cbeb','FINISHED','919d2586-bed7-4b3d-8628-1d8f7d91398c','f917243a-4d5f-40c4-baf5-8bf57f7bbbe8'),('ff8911a2-a704-4398-b789-437ec3bc189b','CANCELED','919d2586-bed7-4b3d-8628-1d8f7d91398c','3d31f243-1d93-47ae-91e9-58d008458c69');
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
INSERT INTO `auction_config` VALUES ('10b454c4-92e7-4673-aaca-ae9e81e69997',150000,13000,'2026-05-29 02:29:42','2026-06-01 07:00:00',36),('3d948586-c722-41c1-a953-793711627d12',50000,15000,'2026-05-29 02:38:32','2026-05-30 23:00:00',36),('62741609-feac-40e5-9eb8-dacb4c276a74',20360000,500000,'2026-05-29 02:50:07','2026-05-29 16:00:00',36),('7b6094f0-693f-486e-ad49-952da730384d',250000,30000,'2026-05-29 02:19:17','2026-05-30 23:59:00',36),('80a85000-667a-43dc-8958-28ef8389168a',10000,1000,'2026-05-30 00:00:00','2026-06-29 12:00:00',36),('90632913-1404-48f4-b694-0a02efa11d4c',15000000,5000000,'2026-05-31 02:27:57','2026-05-31 02:36:36',36),('99ca13c5-b07f-46d2-9a62-fb5475014da8',200000,15000,'2026-05-29 02:31:28','2026-05-30 17:00:00',36),('abb41140-35f7-48a7-8fd9-216afdd28dbd',150000000,10000000,'2026-05-29 02:33:45','2026-06-03 19:00:00',36),('c1a15c23-d100-4473-9afe-65aa388d8f8f',32000,10000,'2026-05-29 02:40:01','2026-05-30 16:00:00',36),('c4b2cab1-64d9-43c4-8093-72c074252ffd',46000,11000,'2026-05-29 02:21:06','2026-05-30 14:00:00',36),('c9a34244-4a4c-49f4-8fec-82ca9af2f50b',555000,55000,'2026-05-29 02:17:22','2026-06-01 16:00:00',36),('cf5272f9-dff2-4833-a331-e5946f11f355',50000,15000,'2026-05-31 02:18:26','2026-05-31 18:49:00',36),('dceb6cc5-d7e6-4e4f-886b-75ac4df1ff1f',1100000,120000,'2026-05-29 02:43:52','2026-05-29 14:00:00',36),('e6a1f0d1-32c1-4e84-b84b-da45e2238069',36000,18000,'2026-05-30 00:00:00','2026-05-31 15:00:00',36),('ec70ad7b-df96-45a2-8c5e-9caad0a93dd4',123000,24000,'2026-05-30 00:00:00','2026-05-30 00:00:00',36),('f95bd70e-9bd0-4365-9859-255c2be50855',345000,20000,'2026-05-29 02:46:23','2026-05-29 12:00:00',36),('fec278dc-a9d0-4ece-9bbe-14b16688cbeb',40000,5000,'2026-05-29 02:53:22','2026-05-29 19:00:00',36),('ff8911a2-a704-4398-b789-437ec3bc189b',1200000,120000,'2026-05-30 00:00:00','2026-05-31 15:00:00',36);
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
INSERT INTO `bid_transaction` VALUES ('10b454c4-92e7-4673-aaca-ae9e81e69997','32841bb2-34a9-4612-8ed6-efa776de8845','onion',150000,150000,'2026-05-29 04:44:59.896838','MANUAL'),('10b454c4-92e7-4673-aaca-ae9e81e69997','32841bb2-34a9-4612-8ed6-efa776de8845','onion',170000,170000,'2026-05-29 04:45:26.269184','MANUAL'),('10b454c4-92e7-4673-aaca-ae9e81e69997','32841bb2-34a9-4612-8ed6-efa776de8845','onion',185000,185000,'2026-05-29 04:45:39.176448','MANUAL'),('c9a34244-4a4c-49f4-8fec-82ca9af2f50b','32841bb2-34a9-4612-8ed6-efa776de8845','onion',560000,560000,'2026-05-29 04:48:14.924113','MANUAL'),('c9a34244-4a4c-49f4-8fec-82ca9af2f50b','32841bb2-34a9-4612-8ed6-efa776de8845','onion',615000,650000,'2026-05-29 04:48:37.351958','AUTO'),('10b454c4-92e7-4673-aaca-ae9e81e69997','32841bb2-34a9-4612-8ed6-efa776de8845','onion',210000,210000,'2026-05-29 13:11:10.995846','MANUAL'),('10b454c4-92e7-4673-aaca-ae9e81e69997','0e94d4ef-cb2f-4315-9a4a-538323d6e022','tue',223000,350000,'2026-05-29 13:12:14.900593','AUTO'),('10b454c4-92e7-4673-aaca-ae9e81e69997','32841bb2-34a9-4612-8ed6-efa776de8845','onion',280000,280000,'2026-05-29 13:12:19.458300','MANUAL'),('10b454c4-92e7-4673-aaca-ae9e81e69997','0e94d4ef-cb2f-4315-9a4a-538323d6e022','tue',293000,350000,'2026-05-29 13:12:19.504321','AUTO'),('10b454c4-92e7-4673-aaca-ae9e81e69997','32841bb2-34a9-4612-8ed6-efa776de8845','onion',363000,450000,'2026-05-29 13:12:36.311256','AUTO'),('10b454c4-92e7-4673-aaca-ae9e81e69997','0e94d4ef-cb2f-4315-9a4a-538323d6e022','tue',463000,500000,'2026-05-29 13:12:36.388886','AUTO'),('3d948586-c722-41c1-a953-793711627d12','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',50000,50000,'2026-05-30 22:02:37.106348','MANUAL'),('3d948586-c722-41c1-a953-793711627d12','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',65000,960000,'2026-05-30 22:48:44.162170','AUTO'),('3d948586-c722-41c1-a953-793711627d12','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',80000,90000,'2026-05-30 22:49:42.079633','AUTO'),('3d948586-c722-41c1-a953-793711627d12','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',95000,800000,'2026-05-30 22:55:25.017877','AUTO'),('7b6094f0-693f-486e-ad49-952da730384d','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',300000,300000,'2026-05-30 23:02:08.520463','MANUAL'),('7b6094f0-693f-486e-ad49-952da730384d','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',330000,600000,'2026-05-30 23:03:26.892747','AUTO'),('7b6094f0-693f-486e-ad49-952da730384d','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',360000,500000,'2026-05-30 23:05:32.986928','AUTO'),('7b6094f0-693f-486e-ad49-952da730384d','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',390000,900000,'2026-05-30 23:19:42.653564','AUTO'),('7b6094f0-693f-486e-ad49-952da730384d','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',420000,600000,'2026-05-30 23:19:55.454176','AUTO'),('cf5272f9-dff2-4833-a331-e5946f11f355','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',50000,50000,'2026-05-31 02:18:59.853403','MANUAL'),('90632913-1404-48f4-b694-0a02efa11d4c','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',15000000,30000000,'2026-05-31 02:29:26.074242','AUTO'),('90632913-1404-48f4-b694-0a02efa11d4c','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',20000000,20000000,'2026-05-31 02:29:52.541781','AUTO'),('90632913-1404-48f4-b694-0a02efa11d4c','32841bb2-34a9-4612-8ed6-efa776de8845','onion',26000000,26000000,'2026-05-31 02:31:29.246594','MANUAL'),('90632913-1404-48f4-b694-0a02efa11d4c','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',31000000,31000000,'2026-05-31 02:35:26.791944','MANUAL'),('c9a34244-4a4c-49f4-8fec-82ca9af2f50b','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',900000,900000,'2026-05-31 02:42:29.184839','MANUAL'),('c9a34244-4a4c-49f4-8fec-82ca9af2f50b','32841bb2-34a9-4612-8ed6-efa776de8845','onion',955000,1500000,'2026-05-31 02:43:25.354213','AUTO'),('c9a34244-4a4c-49f4-8fec-82ca9af2f50b','1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo',1555000,3000000,'2026-05-31 02:43:27.333531','AUTO'),('c9a34244-4a4c-49f4-8fec-82ca9af2f50b','32841bb2-34a9-4612-8ed6-efa776de8845','onion',4000000,4000000,'2026-05-31 02:43:39.423383','MANUAL');
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
INSERT INTO `bidder` VALUES ('0e94d4ef-cb2f-4315-9a4a-538323d6e022',1000000000,500000),('1b9af444-d419-49b6-b94c-0e6c48c90c2b',68644000,0),('32841bb2-34a9-4612-8ed6-efa776de8845',30000000,4000000),('b7f8495b-c990-4892-a4b2-3c52f9b76668',100000000000,0);
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
INSERT INTO `electronic` VALUES ('229bff7f-428b-4978-bebe-6239be4eefb4',0,24),('36575a93-afc2-4976-bebf-7a5ab8e4f9d6',0,0),('3d31f243-1d93-47ae-91e9-58d008458c69',0,13),('57978ce8-e615-4670-a8a0-785780e44389',1,0),('7e78f891-d4e1-4cbd-a791-c2dc7a0b5e74',0,2),('a7b52af6-0322-4c25-9cb0-85b748ea4689',0,15),('b3a3f83a-f0a6-40e0-9b2a-27e79c760d4b',1,1),('eb93690f-942b-46e4-bba0-8a18b49a11c2',0,36);
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
INSERT INTO `entity` VALUES ('000000000000000000000000000000000001','adminTT'),('000000000000000000000000000000000002','adminKP'),('000000000000000000000000000000000003','adminHA'),('000000000000000000000000000000000004','adminTW'),('0d26ddb6-8a25-45eb-86f8-cab200ddc4b2','thầy Chêm'),('0e94d4ef-cb2f-4315-9a4a-538323d6e022','tue tue'),('10b454c4-92e7-4673-aaca-ae9e81e69997','ROMAND\'S'),('1b9af444-d419-49b6-b94c-0e6c48c90c2b','Trung Lê'),('229bff7f-428b-4978-bebe-6239be4eefb4','Melody'),('2fd3bcb7-a4c7-40d6-a896-10e1eb81bf07','Mirror Strawberry'),('32841bb2-34a9-4612-8ed6-efa776de8845','Hà Anh'),('36575a93-afc2-4976-bebf-7a5ab8e4f9d6','Carbonara'),('3d31f243-1d93-47ae-91e9-58d008458c69','Baby I\'m a rockStar'),('3d948586-c722-41c1-a953-793711627d12','Bán FANS'),('57978ce8-e615-4670-a8a0-785780e44389','phone'),('5bff5db0-e229-4a68-8381-586bf48e006b','Xe ngựa'),('62741609-feac-40e5-9eb8-dacb4c276a74','Tablet'),('79c175c3-2050-4798-a1cc-df156d166496','testing'),('7b6094f0-693f-486e-ad49-952da730384d','CORTIS'),('7e78f891-d4e1-4cbd-a791-c2dc7a0b5e74','EN CHAN TÍT'),('80a85000-667a-43dc-8958-28ef8389168a','FTU'),('84308354-5686-4896-81f5-a7fbe7433882','Juhoon'),('8bdb7ff8-c804-4514-8f49-8500f47df8e8','Trung Lê'),('90632913-1404-48f4-b694-0a02efa11d4c','test create auction with image + notification + anti-snipping'),('919d2586-bed7-4b3d-8628-1d8f7d91398c','Hà Anh'),('99ca13c5-b07f-46d2-9a62-fb5475014da8','Flower Knows'),('a7b52af6-0322-4c25-9cb0-85b748ea4689','UnKnown'),('abb41140-35f7-48a7-8fd9-216afdd28dbd','FERRARI'),('b34f3dde-9d37-4fd6-a62c-311b8f1a5494','Juicy lasting tint #21 Deep Sangria'),('b3a3f83a-f0a6-40e0-9b2a-27e79c760d4b','Hello Kitty'),('b7f8495b-c990-4892-a4b2-3c52f9b76668','Phong Nguyen'),('c1a15c23-d100-4473-9afe-65aa388d8f8f','BULDAK'),('c4b2cab1-64d9-43c4-8093-72c074252ffd','Barbie\'s'),('c9a34244-4a4c-49f4-8fec-82ca9af2f50b','CORTIS'),('cf5272f9-dff2-4833-a331-e5946f11f355','test create auction'),('dcddfe84-da38-4eea-a458-5fd8ca3b2f63','Em iu trường em'),('dceb6cc5-d7e6-4e4f-886b-75ac4df1ff1f','Victoria\'s Secret'),('e6a1f0d1-32c1-4e84-b84b-da45e2238069','Phép thuật WINX'),('e6d9cdd0-2f14-4bfa-9691-2b6a40e00def','Perfume'),('eb93690f-942b-46e4-bba0-8a18b49a11c2','drums'),('ec70ad7b-df96-45a2-8c5e-9caad0a93dd4','Phù hợp cho mọi lứa tuổi'),('f04fe671-9b40-499e-b7d6-5ca430b440ff','Heavenly soft supercar'),('f917243a-4d5f-40c4-baf5-8bf57f7bbbe8','Bài tây 52 lá'),('f95bd70e-9bd0-4365-9859-255c2be50855','Sleeping lamp'),('fec278dc-a9d0-4ece-9bbe-14b16688cbeb','Sakura Card Captor'),('ff8911a2-a704-4398-b789-437ec3bc189b','ELECTRIC GUITAR');
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
INSERT INTO `item` VALUES ('0d26ddb6-8a25-45eb-86f8-cab200ddc4b2','919d2586-bed7-4b3d-8628-1d8f7d91398c','Big HIt Entertainment','','VEHICLE'),('229bff7f-428b-4978-bebe-6239be4eefb4','919d2586-bed7-4b3d-8628-1d8f7d91398c','Sanrio','My Melody là một trong những nhân vật nổi tiếng nhất của hãng Sanrio (Nhật Bản), ra mắt lần đầu tiên vào năm 1975. Cô bé được miêu tả là một chú thỏ trắng tốt bụng, ngây thơ, thích làm bánh quy và luôn đội một chiếc mũ trùm đầu màu đỏ hoặc','ELECTRONIC'),('2fd3bcb7-a4c7-40d6-a896-10e1eb81bf07','919d2586-bed7-4b3d-8628-1d8f7d91398c','Hoa biết','Flower Knows là thương hiệu mỹ phẩm nội địa Trung nổi tiếng toàn cầu, được thành lập tại Hàng Châu, nổi bật với phong cách trang điểm \"cổ tích\". Hãng gây ấn tượng mạnh nhờ bao bì thiết kế cầu kỳ, đậm chất nghệ thuật tựa như các món đồ trang sức hoặc tác phẩm điêu khắc cổ điển, có giá trị sưu tầm cao','ART'),('36575a93-afc2-4976-bebf-7a5ab8e4f9d6','919d2586-bed7-4b3d-8628-1d8f7d91398c','Korea','','ELECTRONIC'),('3d31f243-1d93-47ae-91e9-58d008458c69','919d2586-bed7-4b3d-8628-1d8f7d91398c','LiSA','','ELECTRONIC'),('57978ce8-e615-4670-a8a0-785780e44389','919d2586-bed7-4b3d-8628-1d8f7d91398c','Princes','','ELECTRONIC'),('5bff5db0-e229-4a68-8381-586bf48e006b','919d2586-bed7-4b3d-8628-1d8f7d91398c','tiên đỡ đầu','','VEHICLE'),('79c175c3-2050-4798-a1cc-df156d166496','8bdb7ff8-c804-4514-8f49-8500f47df8e8','trunglee','none image first','ART'),('7e78f891-d4e1-4cbd-a791-c2dc7a0b5e74','919d2586-bed7-4b3d-8628-1d8f7d91398c','winx','Open your eyes, open your mind!\nWe are the Winx!','ELECTRONIC'),('84308354-5686-4896-81f5-a7fbe7433882','919d2586-bed7-4b3d-8628-1d8f7d91398c','Big Hit Entertainment','Who is choking? I don\'t care\nWho is choking? I don’t care\nWho is choking? I don\'t care\nWho is choking? I don\'t care\nWho is choking? I don’t care\nWho is choking? I don\'t care (Let\'s go)\n\n[Chorus: Martin, James]\n벌컥, 벌컥, 땡겨, 땡겨, I just choke on 아사이\n한 잔 먹고 흥이 올라 흔들면 다 samba지 (Ah)\n혓 속까지 보라색 Genie 마치 Aladdin\nAll I want is uh, yeah, uh, yeah, all I want is uh, yeah, uh, yeah\n\n[Post-Chorus: Keonho, Juhoon, Seonghyeon]\nUh, uh, uh, uh, uh, hundred 아사이\nUh, uh, uh, uh, uh, bring that 아사이\n벌컥, 벌컥, 땡겨, 땡겨, I just choke on 아사이\n내가 많이 좋아해, uh, uh, 아사이\n\n[Verse 1: Martin]\n아쉬웠던 taste 걷어 근본 없는 topping\n함 바꿔 보게 씬을 마치 명배우의 action\n밤새서 session, 새 hotel check-in\n그다음 날에 check out 어깨 위에 짐을 올린','ART'),('a7b52af6-0322-4c25-9cb0-85b748ea4689','919d2586-bed7-4b3d-8628-1d8f7d91398c','Apple','Ly đến từ Ý, và tivi bên Germany\nBàn bên Thụy SĨĩ, và gương soi mua ở Paris\nVòi khoáng Hàn Quốc, chứa nước muối đến từ Bali','ELECTRONIC'),('b34f3dde-9d37-4fd6-a62c-311b8f1a5494','919d2586-bed7-4b3d-8628-1d8f7d91398c','Romand comestics','Romand Juicy Lasting Tint là dòng son tint bóng nổi tiếng từ thương hiệu Romand (Hàn Quốc). Son nổi bật với chất son mỏng nhẹ, độ bóng trong suốt như ngọc trai giúp môi căng mọng và khả năng giữ màu lâu trôi từ 4-6 giờ. Giá bán dao động từ 150.000đ - 250.000đ/thỏi tùy phiên bản.','ART'),('b3a3f83a-f0a6-40e0-9b2a-27e79c760d4b','919d2586-bed7-4b3d-8628-1d8f7d91398c','China','Chưa từng yêu 1 ai nhiều như thế. Từng cố bỏ đi nhưng đâu dễ','ELECTRONIC'),('dcddfe84-da38-4eea-a458-5fd8ca3b2f63','919d2586-bed7-4b3d-8628-1d8f7d91398c','DuongTranThuy','nó hại mình\nđã lâu tôi không được ăn bữa tử tế\n- chia sẻ bởi 1 ftuer','VEHICLE'),('e6d9cdd0-2f14-4bfa-9691-2b6a40e00def','919d2586-bed7-4b3d-8628-1d8f7d91398c','France','ghé thăm trang web chính thức của chúng tôi: https://www.victoriassecret.com/us/','ART'),('eb93690f-942b-46e4-bba0-8a18b49a11c2','8bdb7ff8-c804-4514-8f49-8500f47df8e8','not me','drum for nightcore music','ELECTRONIC'),('f04fe671-9b40-499e-b7d6-5ca430b440ff','919d2586-bed7-4b3d-8628-1d8f7d91398c','Ferrari','','VEHICLE'),('f917243a-4d5f-40c4-baf5-8bf57f7bbbe8','919d2586-bed7-4b3d-8628-1d8f7d91398c','Japan','anh trai gay\ncrush gay\ncrush thích anh trai','ART');
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
INSERT INTO `item_image_url` VALUES ('84308354-5686-4896-81f5-a7fbe7433882','https://i.pinimg.com/736x/95/d5/9b/95d59b73d58170d4bedebbfef81ef3f1.jpg'),('84308354-5686-4896-81f5-a7fbe7433882','https://i.pinimg.com/736x/53/20/07/5320077dc69beb20a9b23194e5acea31.jpg'),('84308354-5686-4896-81f5-a7fbe7433882','https://i.pinimg.com/736x/5b/b1/49/5bb149418a4121557448649a2706ab96.jpg'),('84308354-5686-4896-81f5-a7fbe7433882','https://i.pinimg.com/736x/59/bd/e3/59bde30dd249066d6474a259e0b6a72f.jpg'),('0d26ddb6-8a25-45eb-86f8-cab200ddc4b2','https://i.pinimg.com/736x/97/3f/cd/973fcd8acc2e91398a40be3b8051cfaa.jpg'),('0d26ddb6-8a25-45eb-86f8-cab200ddc4b2','https://i.pinimg.com/736x/a6/ae/e5/a6aee598c204ff625d1db7310eb27f39.jpg'),('0d26ddb6-8a25-45eb-86f8-cab200ddc4b2','https://i.pinimg.com/736x/9e/12/07/9e1207a69b222059acbab2f2e9366796.jpg'),('0d26ddb6-8a25-45eb-86f8-cab200ddc4b2','https://i.pinimg.com/736x/83/0d/7c/830d7c7a7c357891667313f96baf31d5.jpg'),('57978ce8-e615-4670-a8a0-785780e44389','https://i.pinimg.com/736x/d5/8e/8e/d58e8ed1d98ec020b8124b53786246b1.jpg'),('57978ce8-e615-4670-a8a0-785780e44389','https://i.pinimg.com/736x/12/59/6c/12596cfcea5210448b376281ebf05821.jpg'),('57978ce8-e615-4670-a8a0-785780e44389','https://i.pinimg.com/736x/ee/86/25/ee8625111aa12df3283a944338ff5a37.jpg'),('b34f3dde-9d37-4fd6-a62c-311b8f1a5494','https://i.pinimg.com/736x/3a/5b/9a/3a5b9a8a33c4352b7054ba536863f296.jpg'),('b34f3dde-9d37-4fd6-a62c-311b8f1a5494','https://i.pinimg.com/736x/41/9f/35/419f355a107dc17a8b8c1c3cec077807.jpg'),('b34f3dde-9d37-4fd6-a62c-311b8f1a5494','https://i.pinimg.com/736x/6a/78/86/6a78868d976f3eca79c2182361b72575.jpg'),('2fd3bcb7-a4c7-40d6-a896-10e1eb81bf07','https://i.pinimg.com/736x/e3/c4/eb/e3c4ebdfdcbbf9bc66ad9d81fd0db667.jpg'),('2fd3bcb7-a4c7-40d6-a896-10e1eb81bf07','https://i.pinimg.com/736x/fb/68/57/fb68573d730f0aa742d9d9f25a87fad4.jpg'),('2fd3bcb7-a4c7-40d6-a896-10e1eb81bf07','https://i.pinimg.com/736x/f6/b5/da/f6b5da2ce4f0dfe11775105919d8ff55.jpg'),('f04fe671-9b40-499e-b7d6-5ca430b440ff','https://i.pinimg.com/1200x/6f/13/6d/6f136d6baf16ea67d1522be4b7e2ccbe.jpg'),('f04fe671-9b40-499e-b7d6-5ca430b440ff','https://i.pinimg.com/1200x/31/02/33/3102339c1f6748d6e07b821085588ce3.jpg'),('f04fe671-9b40-499e-b7d6-5ca430b440ff','https://i.pinimg.com/736x/31/0a/b2/310ab2eb3fa4ab143afd139ea3d399e5.jpg'),('f04fe671-9b40-499e-b7d6-5ca430b440ff','https://i.pinimg.com/736x/af/f8/d2/aff8d229bab63cd223cbc54cdba9ddb0.jpg'),('3d31f243-1d93-47ae-91e9-58d008458c69','https://i.pinimg.com/736x/0e/32/38/0e32383ca80302d622ec8838b7cbd427.jpg'),('3d31f243-1d93-47ae-91e9-58d008458c69','https://i.pinimg.com/736x/3e/e1/24/3ee12442980a1d9c6cd2954abe91cb36.jpg'),('3d31f243-1d93-47ae-91e9-58d008458c69','https://i.pinimg.com/736x/e9/63/1e/e9631e13f108f5782ae642f687ce0037.jpg'),('b3a3f83a-f0a6-40e0-9b2a-27e79c760d4b','https://i.pinimg.com/736x/e7/62/4a/e7624a33052e210f833d0079c32826fa.jpg'),('b3a3f83a-f0a6-40e0-9b2a-27e79c760d4b','https://i.pinimg.com/736x/09/62/40/096240e4a7080b2f7906a53bf8fd757c.jpg'),('b3a3f83a-f0a6-40e0-9b2a-27e79c760d4b','https://i.pinimg.com/736x/65/10/37/651037777a74988b2708d6180c9ecb67.jpg'),('36575a93-afc2-4976-bebf-7a5ab8e4f9d6','https://i.pinimg.com/736x/01/c7/25/01c72587540a0f8b1f01af86bf0e670e.jpg'),('36575a93-afc2-4976-bebf-7a5ab8e4f9d6','https://i.pinimg.com/736x/49/ae/f9/49aef993bc0f309b7005966f08c8f86f.jpg'),('36575a93-afc2-4976-bebf-7a5ab8e4f9d6','https://i.pinimg.com/736x/a5/fa/81/a5fa81ee5a5115dd27cc281437b1a169.jpg'),('e6d9cdd0-2f14-4bfa-9691-2b6a40e00def','https://i.pinimg.com/736x/06/79/37/067937057589d754517c0bfbd909141e.jpg'),('e6d9cdd0-2f14-4bfa-9691-2b6a40e00def','https://i.pinimg.com/736x/87/3a/ec/873aeccf968de74b75c5abdede766b26.jpg'),('229bff7f-428b-4978-bebe-6239be4eefb4','https://i.pinimg.com/736x/bf/4e/9c/bf4e9c70495de3080f0e6164c811e3e0.jpg'),('229bff7f-428b-4978-bebe-6239be4eefb4','https://i.pinimg.com/736x/88/b7/af/88b7af2dd86db8deafc3be423ab06021.jpg'),('229bff7f-428b-4978-bebe-6239be4eefb4','https://i.pinimg.com/1200x/b8/be/0c/b8be0cc140fe01132dc094dbec9076de.jpg'),('a7b52af6-0322-4c25-9cb0-85b748ea4689','https://i.pinimg.com/736x/77/9c/61/779c614cd3e8018d85224a20c6f4dade.jpg'),('a7b52af6-0322-4c25-9cb0-85b748ea4689','https://i.pinimg.com/1200x/a0/a8/31/a0a831e73f7490a7044652f24073b923.jpg'),('f917243a-4d5f-40c4-baf5-8bf57f7bbbe8','https://i.pinimg.com/736x/58/4c/9e/584c9e756fe1fb98e894a20a49ebd81c.jpg'),('f917243a-4d5f-40c4-baf5-8bf57f7bbbe8','https://i.pinimg.com/1200x/7c/a8/e1/7ca8e151daa6d2079d3cf5a3e19df0e5.jpg'),('7e78f891-d4e1-4cbd-a791-c2dc7a0b5e74','https://i.pinimg.com/736x/10/01/cb/1001cb70c8703c8ff53c8af8d7061120.jpg'),('7e78f891-d4e1-4cbd-a791-c2dc7a0b5e74','https://i.pinimg.com/736x/6e/eb/67/6eeb673cfa0a782365b2966730f6763d.jpg'),('7e78f891-d4e1-4cbd-a791-c2dc7a0b5e74','https://i.pinimg.com/736x/97/41/cd/9741cd1ba3c45868ba479ef7a79e2c49.jpg'),('5bff5db0-e229-4a68-8381-586bf48e006b','https://i.pinimg.com/1200x/ab/6f/04/ab6f045c139ab21313f859213c71b185.jpg'),('dcddfe84-da38-4eea-a458-5fd8ca3b2f63','https://i.pinimg.com/736x/f0/f4/fb/f0f4fb0dd6db839d95254f5247d795f3.jpg'),('79c175c3-2050-4798-a1cc-df156d166496','https://cdn.donmai.us/original/b1/a8/b1a861a2321d635e7a0d6e452730f9d5.jpg'),('eb93690f-942b-46e4-bba0-8a18b49a11c2','https://i.pinimg.com/1200x/5f/69/12/5f69121238fe6d3b70c7faa4c4efa201.jpg'),('eb93690f-942b-46e4-bba0-8a18b49a11c2','https://i.pinimg.com/1200x/63/e0/a6/63e0a6b3b24ac0f5ad59eac79e877e77.jpg');
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
INSERT INTO `seller` VALUES ('8bdb7ff8-c804-4514-8f49-8500f47df8e8','112233445566',31000000,0),('919d2586-bed7-4b3d-8628-1d8f7d91398c','0819908626',515000,4463000);
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
INSERT INTO `user` VALUES ('000000000000000000000000000000000001','adminTT','123456','0','ADMIN'),('000000000000000000000000000000000002','adminKP','123456','0','ADMIN'),('000000000000000000000000000000000003','adminHA','123456','0','ADMIN'),('000000000000000000000000000000000004','adminTW','123456','0','ADMIN'),('0e94d4ef-cb2f-4315-9a4a-538323d6e022','tue','123456','tue@','BIDDER'),('1b9af444-d419-49b6-b94c-0e6c48c90c2b','trungbeo','trung2007','info@gmail.com','BIDDER'),('32841bb2-34a9-4612-8ed6-efa776de8845','onion','123456','intotheunknownohhh@gmail.com','BIDDER'),('8bdb7ff8-c804-4514-8f49-8500f47df8e8','trunglee','trung2007','trungbeo@gmail.com','SELLER'),('919d2586-bed7-4b3d-8628-1d8f7d91398c','ozone','123456','intotheunknownohh@gmail.com','SELLER'),('b7f8495b-c990-4892-a4b2-3c52f9b76668','Kphong','123456','123@gmail.com','BIDDER');
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
INSERT INTO `vehicle` VALUES ('0d26ddb6-8a25-45eb-86f8-cab200ddc4b2',0,12),('5bff5db0-e229-4a68-8381-586bf48e006b',0,1),('dcddfe84-da38-4eea-a458-5fd8ca3b2f63',0,0),('f04fe671-9b40-499e-b7d6-5ca430b440ff',0,14);
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
INSERT INTO `watchlist` VALUES ('7b6094f0-693f-486e-ad49-952da730384d','1b9af444-d419-49b6-b94c-0e6c48c90c2b'),('90632913-1404-48f4-b694-0a02efa11d4c','1b9af444-d419-49b6-b94c-0e6c48c90c2b'),('c9a34244-4a4c-49f4-8fec-82ca9af2f50b','32841bb2-34a9-4612-8ed6-efa776de8845');
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

-- Dump completed on 2026-05-31 15:46:25
