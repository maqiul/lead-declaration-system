-- ============================================================
-- Lead Declaration System - Production Database Init Script
-- Generated: 2026-03-25 from Docker MySQL (mysql8)
-- Excludes: Flowable engine tables (ACT_*, FLW_*)
--           They are auto-created by Spring Boot on startup.
-- ============================================================

CREATE DATABASE IF NOT EXISTS `lead_declaration` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `lead_declaration`;

-- MySQL dump 10.13  Distrib 8.0.43, for Linux (x86_64)
--
-- Host: localhost    Database: lead_declaration
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `bank_account_config`
--

DROP TABLE IF EXISTS `bank_account_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bank_account_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `account_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '璐︽埛鍚嶇О',
  `bank_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '閾惰鍚嶇О',
  `bank_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '閾惰浠ｇ爜',
  `account_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '閾惰璐﹀彿',
  `swift_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SWIFT浠ｇ爜',
  `iban` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IBAN鍙风爜',
  `account_holder` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '璐︽埛鎸佹湁浜?,
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USD' COMMENT '璐︽埛甯佺',
  `branch_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鏀鍚嶇О',
  `branch_address` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鏀鍦板潃',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '鏄惁榛樿璐︽埛 0-鍚?1-鏄?,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-绂佺敤 1-鍚敤',
  `sort` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `remarks` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '澶囨敞',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囧織 0-姝ｅ父 1-鍒犻櫎',
  `service_fee_rate` decimal(10,6) DEFAULT '0.000000' COMMENT '鎵嬬画璐圭巼',
  PRIMARY KEY (`id`),
  KEY `idx_bank_name` (`bank_name`),
  KEY `idx_account_holder` (`account_holder`),
  KEY `idx_currency` (`currency`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='閾惰璐︽埛閰嶇疆琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bank_account_config`
--

LOCK TABLES `bank_account_config` WRITE;
/*!40000 ALTER TABLE `bank_account_config` DISABLE KEYS */;
INSERT INTO `bank_account_config` VALUES (1,'缇庡厓鍩烘湰璐︽埛','涓浗閾惰','BOC','1234567890123456','BKCHCNBJ',NULL,'瀹佹尝鏅虹考绉戞妧鏈夐檺鍏徃','USD',NULL,NULL,1,1,1,'鍏徃涓昏缇庡厓鏀舵璐︽埛','2026-03-17 07:34:39','2026-03-17 16:09:16',NULL,1,1,0.000000),(2,'娆у厓璐︽埛','宸ュ晢閾惰','ICBC','9876543210987654','ICBKCNBJ',NULL,'瀹佹尝鏅虹考绉戞妧鏈夐檺鍏徃','EUR',NULL,NULL,0,1,2,'娆ф床涓氬姟鏀舵璐︽埛','2026-03-17 07:34:39','2026-03-17 16:09:15',NULL,1,1,0.000000),(3,'浜烘皯甯佽处鎴?,'寤鸿閾惰','CCB','1122334455667788','PCBCCNBJ',NULL,'瀹佹尝鏅虹考绉戞妧鏈夐檺鍏徃','CNY',NULL,NULL,0,1,3,'鍥藉唴涓氬姟鏀舵璐︽埛','2026-03-17 07:34:39','2026-03-17 16:09:13',NULL,1,1,0.000000),(4,'111','11','11','111','11','11','111','USD','1','',0,1,0,'','2026-03-24 17:24:56','2026-03-25 10:16:21',1,1,0,0.020000);
/*!40000 ALTER TABLE `bank_account_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contract_generation`
--

DROP TABLE IF EXISTS `contract_generation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contract_generation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `contract_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍚堝悓缂栧彿',
  `declaration_form_id` bigint NOT NULL COMMENT '鍏宠仈鐨勭敵鎶ュ崟ID',
  `template_id` bigint NOT NULL COMMENT '浣跨敤鐨勬ā鏉縄D',
  `generated_file_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐢熸垚鐨勬枃浠跺悕',
  `generated_file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐢熸垚鐨勬枃浠惰矾寰?,
  `file_size` bigint DEFAULT NULL COMMENT '鏂囦欢澶у皬(瀛楄妭)',
  `generated_by` bigint NOT NULL COMMENT '鐢熸垚浜?,
  `generated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鐢熸垚鏃堕棿',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-宸插垹闄?1-姝ｅ父',
  `remarks` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '澶囨敞',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_declaration_form_id` (`declaration_form_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_generated_by` (`generated_by`),
  KEY `idx_generated_time` (`generated_time`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍚堝悓鐢熸垚璁板綍琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contract_generation`
--

LOCK TABLES `contract_generation` WRITE;
/*!40000 ALTER TABLE `contract_generation` DISABLE KEYS */;
INSERT INTO `contract_generation` VALUES (1,'EC20260323366297',29,1,'EC20260323366297_1774243366310.docx','DEC202603237846/EC20260323366297_1774243366310.docx',719982,1,'2026-03-23 13:22:47',0,NULL),(2,'EC20260323123228',29,1,'EC20260323123228_1774244123228.docx','DEC202603237846/EC20260323123228_1774244123228.docx',719982,1,'2026-03-23 13:35:23',0,NULL),(3,'EC20260323746347',29,1,'EC20260323746347_1774244746364.docx','DEC202603237846/EC20260323746347_1774244746364.docx',719777,1,'2026-03-23 13:45:47',0,NULL),(4,'EC20260323949377',29,1,'EC20260323949377_1774244949378.docx','DEC202603237846/EC20260323949377_1774244949378.docx',719789,1,'2026-03-23 13:49:10',0,NULL),(5,'EC20260323180277',29,1,'EC20260323180277_1774245180277.docx','DEC202603237846/EC20260323180277_1774245180277.docx',719789,1,'2026-03-23 13:53:01',0,NULL),(6,'EC20260323629205',29,1,'EC20260323629205_1774245629206.docx','DEC202603237846/EC20260323629205_1774245629206.docx',719789,1,'2026-03-23 14:00:29',1,NULL),(7,'EC20260323199186',28,1,'EC20260323199186_1774250199187.docx','DEC202603236836/EC20260323199186_1774250199187.docx',719789,1,'2026-03-23 15:16:40',1,NULL),(8,'EC20260324970502',30,1,'EC20260324970502_1774327970517.docx','DEC20260324387/EC20260324970502_1774327970517.docx',719697,1,'2026-03-24 12:52:51',1,NULL),(9,'EC20260324485090',31,1,'EC20260324485090_1774337485091.docx','DEC202603246011/EC20260324485090_1774337485091.docx',719699,1,'2026-03-24 15:31:26',1,NULL),(10,'EC20260324726085',32,1,'EC20260324726085_1774344726086.docx','DEC202603248904/EC20260324726085_1774344726086.docx',719701,1,'2026-03-24 17:32:07',1,NULL),(11,'EC20260324597081',34,1,'EC20260324597081_1774349597081.docx','DEC202603241260/EC20260324597081_1774349597081.docx',719699,1,'2026-03-24 18:53:18',1,NULL),(12,'EC20260325917708',43,1,'EC20260325917708_1774416917727.docx','DEC202603256924/EC20260325917708_1774416917727.docx',719701,1,'2026-03-25 13:35:19',1,NULL);
/*!40000 ALTER TABLE `contract_generation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contract_template`
--

DROP TABLE IF EXISTS `contract_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contract_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_name` varchar(200) NOT NULL COMMENT '妯℃澘鍚嶇О',
  `template_code` varchar(100) DEFAULT NULL COMMENT '妯℃澘缂栫爜',
  `template_type` varchar(50) DEFAULT 'EXPORT' COMMENT '妯℃澘绫诲瀷',
  `file_name` varchar(300) DEFAULT NULL COMMENT '鏂囦欢鍚?,
  `file_path` varchar(500) DEFAULT NULL COMMENT '鏂囦欢璺緞',
  `file_size` bigint DEFAULT '0' COMMENT '鏂囦欢澶у皬',
  `description` text COMMENT '鎻忚堪',
  `STATUS` int DEFAULT '1' COMMENT '0绂佺敤1鍚敤',
  `sort` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `del_flag` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鍚堝悓妯℃澘琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contract_template`
--

LOCK TABLES `contract_template` WRITE;
/*!40000 ALTER TABLE `contract_template` DISABLE KEYS */;
INSERT INTO `contract_template` VALUES (1,'11','11','EXPORT','fc40c7c02f654afebe57ff565a9c45d6.docx','\\uploads\\templates\\fc40c7c02f654afebe57ff565a9c45d6.docx',729110,'',1,1,'2026-03-19 09:48:09','2026-03-19 09:48:09',1,1,0);
/*!40000 ALTER TABLE `contract_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `country_info`
--

DROP TABLE IF EXISTS `country_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `country_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `country_code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍥藉浠ｇ爜(ISO 3166-1 alpha-3)',
  `chinese_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '涓枃鍚嶇О',
  `english_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鑻辨枃鍚嶇О',
  `abbreviation` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '绠€绉?缂╁啓 (闈炲敮涓€锛岄伩鍏嶅啿绐?',
  `continent` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎵€灞炴床 (浜氭床/娆ф床/鍖楃編娲茬瓑)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-绂佺敤 1-鍚敤',
  `sort` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭鍊?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜篒D',
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜篒D',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囧織 0-姝ｅ父 1-鍒犻櫎',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_country_code` (`country_code`),
  KEY `idx_abbreviation` (`abbreviation`),
  KEY `idx_chinese_name` (`chinese_name`),
  KEY `idx_english_name` (`english_name`),
  KEY `idx_continent` (`continent`),
  KEY `idx_status_del` (`status`,`del_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍥藉淇℃伅琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `country_info`
--

LOCK TABLES `country_info` WRITE;
/*!40000 ALTER TABLE `country_info` DISABLE KEYS */;
INSERT INTO `country_info` VALUES (1,'CHN','涓浗','China','CHN','浜氭床',1,1,'2026-03-17 07:29:07','2026-03-17 16:08:51',NULL,1,1),(2,'USA','缇庡浗','United States','USA','鍖楃編娲?,1,2,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(3,'GBR','鑻卞浗','United Kingdom','GBR','娆ф床',1,3,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(4,'DEU','寰峰浗','Germany','DEU','娆ф床',1,4,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(5,'FRA','娉曞浗','France','FRA','娆ф床',1,5,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(6,'JPN','鏃ユ湰','Japan','JPN','浜氭床',1,6,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(7,'KOR','闊╁浗','South Korea','KOR','浜氭床',1,7,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(8,'AUS','婢冲ぇ鍒╀簹','Australia','AUS','澶ф磱娲?,1,8,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(9,'CAN','鍔犳嬁澶?,'Canada','CAN','鍖楃編娲?,1,9,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(10,'SGP','鏂板姞鍧?,'Singapore','SGP','浜氭床',1,10,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(11,'MYS','椹潵瑗夸簹','Malaysia','MYS','浜氭床',1,11,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(12,'THA','娉板浗','Thailand','THA','浜氭床',1,12,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(13,'VNM','瓒婂崡','Vietnam','VNM','浜氭床',1,13,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(14,'IND','鍗板害','India','IND','浜氭床',1,14,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(15,'BRA','宸磋タ','Brazil','BRA','鍗楃編娲?,1,15,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(16,'RUS','淇勭綏鏂?,'Russia','RUS','娆ф床',1,16,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(17,'ITA','鎰忓ぇ鍒?,'Italy','ITA','娆ф床',1,17,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(18,'ESP','瑗跨彮鐗?,'Spain','ESP','娆ф床',1,18,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(19,'NLD','鑽峰叞','Netherlands','NLD','娆ф床',1,19,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0),(20,'BEL','姣斿埄鏃?,'Belgium','BEL','娆ф床',1,20,'2026-03-17 07:29:07','2026-03-17 07:29:07',NULL,NULL,0);
/*!40000 ALTER TABLE `country_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `currency_info`
--

DROP TABLE IF EXISTS `currency_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `currency_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `currency_code` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT '璐у竵浠ｇ爜(ISO 4217)',
  `currency_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鑻辨枃鍚嶇О',
  `chinese_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '涓枃鍚嶇О',
  `unit_cn` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '涓枃鍗曚綅',
  `symbol` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '璐у竵绗﹀彿',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-绂佺敤 1-鍚敤',
  `sort` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '閫昏緫鍒犻櫎 0-鏈垹闄?1-宸插垹闄?,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_currency_code` (`currency_code`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='璐у竵淇℃伅琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `currency_info`
--

LOCK TABLES `currency_info` WRITE;
/*!40000 ALTER TABLE `currency_info` DISABLE KEYS */;
INSERT INTO `currency_info` VALUES (1,'CNY','Chinese Yuan','浜烘皯甯?,'鍏?,'楼',1,1,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(2,'USD','US Dollar','缇庡厓','缇庡垎','$',1,2,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(3,'EUR','Euro','娆у厓','鍒?,'鈧?,1,3,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(4,'GBP','British Pound','鑻遍晳','渚垮＋','拢',1,4,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(5,'JPY','Japanese Yen','鏃ュ厓','鍐?,'楼',1,5,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(6,'HKD','Hong Kong Dollar','娓竵','浠?,'HK$',1,6,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(7,'KRW','South Korean Won','闊╁厓','鍏?,'鈧?,1,7,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(8,'SGD','Singapore Dollar','鏂板姞鍧″厓','鍒?,'S$',1,8,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(9,'AUD','Australian Dollar','婢冲厓','鍒?,'A$',1,9,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(10,'CAD','Canadian Dollar','鍔犲厓','鍒?,'CA$',1,10,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(11,'TWD','New Taiwan Dollar','鏂板彴甯?,'瑙?,'NT$',1,11,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(12,'THB','Thai Baht','娉伴摙','钀ㄥ綋','喔?,1,12,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(13,'MYR','Malaysian Ringgit','椹潵瑗夸簹鏋楀悏鐗?,'浠?,'RM',1,13,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(14,'VND','Vietnamese Dong','瓒婂崡鐩?,'姣?,'鈧?,1,14,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(15,'RUB','Russian Ruble','淇勭綏鏂崲甯?,'鎴堟瘮','鈧?,1,15,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0),(16,'INR','Indian Rupee','鍗板害鍗㈡瘮','娲惧＋','鈧?,1,16,'2026-03-23 05:37:25','2026-03-23 05:37:25',NULL,NULL,0);
/*!40000 ALTER TABLE `currency_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `declaration_attachment`
--

DROP TABLE IF EXISTS `declaration_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `declaration_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `form_id` bigint DEFAULT NULL COMMENT '鐢虫姤鍗旾D',
  `file_name` varchar(255) NOT NULL COMMENT '鏂囦欢鍚?,
  `file_url` varchar(500) NOT NULL COMMENT '鏂囦欢涓嬭浇璺緞',
  `file_type` varchar(50) DEFAULT NULL COMMENT '鏂囦欢绫诲瀷 (Invoice, PackingList, etc.)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`)
) ENGINE=InnoDB AUTO_INCREMENT=224 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐢虫姤鍗曢檮浠惰〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `declaration_attachment`
--

LOCK TABLES `declaration_attachment` WRITE;
/*!40000 ALTER TABLE `declaration_attachment` DISABLE KEYS */;
INSERT INTO `declaration_attachment` VALUES (1,5,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603166567.xlsx','/api/v1/files/download?path=Declaration_DEC202603166567_f351579f974e4e219bad0654421c2052.xlsx','FullDocuments','2026-03-16 07:47:30'),(2,5,'瀹氶噾姘村崟_DEC202603166567.xlsx','/api/v1/files/download?path=Remittance_瀹氶噾_DEC202603166567_1e6603d5052549b9b93b312bf701a1b2.xlsx','Remittance_Deposit','2026-03-16 15:48:08'),(3,6,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603166272.xlsx','/api/v1/files/download?path=Declaration_DEC202603166272_510d7e43c50c43af9fc26d350c16a68f.xlsx','FullDocuments','2026-03-16 09:45:44'),(4,NULL,'qrcode (9).png','/api/v1/files/download?path=4a77825b-1791-4f6f-95e5-50aba577a4c1.png','ProductPhoto','2026-03-17 10:04:01'),(5,NULL,'qrcode (9).png','/api/v1/files/download?path=7b8911ae-81e6-454c-8a6c-fb064fae2d2c.png','ProductPhoto','2026-03-17 10:22:43'),(6,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/b845874c-b3ef-4901-93ac-f7d66b1bf168.png','ProductPhoto','2026-03-17 10:50:13'),(7,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/14f2439e-eb31-4f37-a793-f20ca93c5709.png','ProductPhoto','2026-03-17 11:06:56'),(8,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/439c068b-2206-40c0-a1ca-030ada920426.png','ProductPhoto','2026-03-17 11:08:04'),(9,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/c62510d8-22c8-4e43-9de7-6241e51d49a6.png','ProductPhoto','2026-03-17 11:18:15'),(10,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/9505fbdc-28ec-4eda-bccb-be8b0c6fca9f.png','ProductPhoto','2026-03-17 11:25:09'),(11,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/4ee39935-66a8-493e-9289-fa7c592c3f45.png','ProductPhoto','2026-03-17 11:33:45'),(12,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/af5fa9ff-fae8-4f01-8eb2-3368656e8fe9.png','ProductPhoto','2026-03-17 11:33:54'),(13,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/ecc5deb6-16d0-4eb1-b513-db419c419ae0.png','ProductPhoto','2026-03-17 11:34:35'),(14,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/787bebfc-7fbb-413a-acf5-aa4a669f68e3.png','ProductPhoto','2026-03-17 11:40:19'),(15,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/7973016e-99df-4a4b-813f-4d8f0dddba4f.png','ProductPhoto','2026-03-17 11:44:25'),(16,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/a08db7fd-3270-4f03-9c72-3e976b64a759.png','ProductPhoto','2026-03-17 11:50:05'),(17,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/a4560e28-bdd1-4cc8-83a9-14858a408b23.png','ProductPhoto','2026-03-17 11:52:14'),(18,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/e69f3c7a-7052-4d7f-9abe-f9cc19c400ce.png','ProductPhoto','2026-03-17 12:59:32'),(19,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/46f12443-0b76-49aa-83cd-316a11dbdb69.png','ProductPhoto','2026-03-17 13:21:19'),(20,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/ea80901c-93ef-4828-9f77-21021dac6e92.png','ProductPhoto','2026-03-17 13:21:41'),(21,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/cb266919-b7ee-4a70-b44a-f0fa9b2c5d5a.png','ProductPhoto','2026-03-17 13:23:37'),(22,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/7c682116-bf75-433c-8193-4cc789539f19.png','ProductPhoto','2026-03-17 13:23:42'),(23,NULL,'qrcode (8).png','/api/v1/files/download?path=202603/b47eb0a4-cc94-4739-addc-d0b3959285c0.png','ProductPhoto','2026-03-17 13:25:54'),(24,9,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603173522.xlsx','/api/v1/files/download?path=Declaration_DEC202603173522_9fbc36319d0840f08632f046a9671256.xlsx','FullDocuments','2026-03-17 05:26:25'),(25,10,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603179280.xlsx','/api/v1/files/download?path=Declaration_DEC202603179280_cedf2624d7b544b2a450734f3d46b457.xlsx','FullDocuments','2026-03-17 05:37:17'),(26,NULL,'cursor-2025.png','/api/v1/files/download?path=202603/2d9cb300-b0c9-49d0-a22a-1049c55b3aa3.png','remittance','2026-03-17 13:52:59'),(28,9,'瀹氶噾姘村崟_DEC202603173522.xlsx','/api/v1/files/download?path=Remittance_瀹氶噾_DEC202603173522_5af5d9dead7c4203aa8cc81da06a16f3.xlsx','Remittance_Deposit','2026-03-17 13:53:33'),(29,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/cbe12cb5-3fc2-40ad-9078-f1d46cb61e26.png','remittance','2026-03-17 14:30:30'),(30,10,'瀹氶噾姘村崟_DEC202603179280.xlsx','/api/v1/files/download?path=Remittance_瀹氶噾_DEC202603179280_152eb09d05aa4a0db33457bd3e6c6f96.xlsx','Remittance_Deposit','2026-03-17 14:30:32'),(31,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/ba5667a5-d183-42eb-8be0-8feeed87fd79.png','remittance','2026-03-17 14:31:30'),(33,10,'灏炬姘村崟_DEC202603179280.xlsx','/api/v1/files/download?path=Remittance_灏炬_DEC202603179280_050864dde52141c0b79591e81d7715ea.xlsx','Remittance_Balance','2026-03-17 14:54:43'),(34,NULL,'cursor-2025.png','/api/v1/files/download?path=202603/0db14f5c-80b4-4f5a-be98-d0ad373a4bb3.png','ProductPhoto','2026-03-17 15:05:58'),(35,11,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603172214.xlsx','/api/v1/files/download?path=Declaration_DEC202603172214_17522358cf114c9ab68d8c37b4006adf.xlsx','FullDocuments','2026-03-17 07:06:38'),(36,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/eebae4ce-f87e-41c2-b1ce-ecd705e59b3d.png','ProductPhoto','2026-03-18 10:21:23'),(37,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/1d995327-a5d6-4ad0-936d-ed3bf610d32b.png','ProductPhoto','2026-03-18 11:04:20'),(38,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/e9c04dd3-a53f-4dba-ad2d-2174af243bdb.png','ProductPhoto','2026-03-18 11:17:53'),(39,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/a4747ba4-c8ea-4a34-a124-13693c82c980.png','ProductPhoto','2026-03-18 11:19:40'),(40,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/4bccce59-ef84-45d2-8781-1e065ffd233c.png','ProductPhoto','2026-03-18 11:25:42'),(41,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/e8bb2e05-d208-479f-a19e-56fe77c8d55d.png','ProductPhoto','2026-03-18 13:41:05'),(42,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/1e466587-984c-4b50-a227-e33c7b94f59d.png','ProductPhoto','2026-03-18 14:00:07'),(43,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/017c939a-00db-456f-9778-a5d7937cfcc2.png','ProductPhoto','2026-03-18 14:12:40'),(49,22,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603184030.xlsx','/api/v1/files/download?path=Declaration_DEC202603184030_a3595181da6a4f8f8b2240be2bfcc120.xlsx','FullDocuments','2026-03-18 06:34:20'),(50,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/cb7c4270-8ab4-4c99-94f1-44802d23500e.png','remittance','2026-03-18 14:35:18'),(52,22,'瀹氶噾姘村崟_DEC202603184030.xlsx','/api/v1/files/download?path=Remittance_瀹氶噾_DEC202603184030_0de53f49305d43c3b6cc82bc9c9cf930.xlsx','Remittance_Deposit','2026-03-18 14:53:08'),(53,22,'灏炬姘村崟_DEC202603184030.xlsx','/api/v1/files/download?path=Remittance_灏炬_DEC202603184030_c31ba03a34994aee9ca0cf9658922318.xlsx','Remittance_Balance','2026-03-18 14:54:24'),(62,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4.png','Common','2026-03-18 15:22:49'),(63,22,'qrcode (9).png','/api/v1/files/download?path=202603/f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4.png','PickupList','2026-03-18 15:22:49'),(64,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/b4a82934-b97b-47cc-a7fd-2895b10c77d3.png','ProductPhoto','2026-03-18 15:59:02'),(66,23,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603183348.xlsx','/api/v1/files/download?path=Declaration_DEC202603183348_09e0b3162497443893259bea1246cd8a.xlsx','FullDocuments','2026-03-18 07:59:08'),(67,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/de3f3ee7-4e43-4487-aba0-e14eff969ebb.png','remittance','2026-03-18 15:59:41'),(68,23,'瀹氶噾姘村崟_DEC202603183348.xlsx','/api/v1/files/download?path=Remittance_瀹氶噾_DEC202603183348_be03ba913f2d46f3a12bc9302a7893c5.xlsx','Remittance_Deposit','2026-03-18 15:59:44'),(69,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/29f2a29a-6ea9-45eb-8968-41cebc81c5fa.png','remittance','2026-03-18 16:00:06'),(70,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/638d56c7-96fb-4bd9-aada-2cced5d7f518.png','remittance','2026-03-18 16:00:33'),(71,23,'灏炬姘村崟_DEC202603183348.xlsx','/api/v1/files/download?path=Remittance_灏炬_DEC202603183348_79013cebadeb4e70a246e2764b817937.xlsx','Remittance_Balance','2026-03-18 16:00:35'),(72,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/caee3b2a-854a-4575-8704-e03f35769edb.png','PickupList','2026-03-18 16:03:53'),(73,23,'qrcode (9).png','/api/v1/files/download?path=202603/caee3b2a-854a-4575-8704-e03f35769edb.png','PickupList','2026-03-18 16:03:53'),(74,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4.png','/api/v1/files/download?path=202603/0d8f79d7-1bb1-4cb1-b2ca-916e8dda09f1.png','TaxRefund','2026-03-18 16:54:31'),(75,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (1).png','/api/v1/files/download?path=202603/ccd957b9-d053-4c55-b29d-a99ded26c7c2.png','TaxRefund','2026-03-18 16:54:38'),(76,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/6219ff20-3284-442d-acef-3761f509dfaf.png','TaxRefund','2026-03-18 17:19:05'),(77,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/5b754478-8c9a-4849-b0ec-d457b69592f0.png','TaxRefund','2026-03-18 17:37:18'),(78,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4.png','/api/v1/files/download?path=202603/1cd13299-7002-4bff-8aa0-826d8a1760a9.png','TaxRefund','2026-03-18 17:46:24'),(79,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/60998f8c-ce9a-4af9-abf0-0fe4e3bdb8ba.png','TaxRefund','2026-03-18 17:51:37'),(80,NULL,'invoice.pdf','/api/v1/files/download?path=202603/f00b2c51-4264-4f60-a10a-422472ee2df8.pdf','TaxRefund','2026-03-19 10:33:51'),(81,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/269b301b-4018-4232-a641-454b931dfed8.png','TaxRefund','2026-03-19 10:40:39'),(82,NULL,'invoice_invoice_merged.pdf','/api/v1/files/download?path=202603/f92fd497-099a-4a76-a8fa-320b8ce9b470.pdf','TaxRefund','2026-03-19 11:15:11'),(83,NULL,'invoice.pdf','/api/v1/files/download?path=202603/0990d4f2-6f20-4339-b6aa-bf667f1e95df.pdf','TaxRefund','2026-03-19 11:44:31'),(84,NULL,'100023273149-鐢靛瓙鍙戠エ-26117000000099447157-20260112-103908.pdf','/api/v1/files/download?path=202603/32e66d75-f551-49f1-bb06-999c9292cc5e.pdf','TaxRefund','2026-03-19 11:44:37'),(85,NULL,'100023273149-鐢靛瓙鍙戠エ-26117000000099447157-20260112-103908.pdf','/api/v1/files/download?path=202603/34fd7434-9dba-4b40-b664-779d38fc78e5.pdf','TaxRefund','2026-03-19 11:56:34'),(86,NULL,'100023273149-鐢靛瓙鍙戠エ-26117000000099447157-20260112-103908.pdf','/api/v1/files/download?path=202603/7f933747-54ce-4a46-b18b-59825ddc441d.pdf','TaxRefund','2026-03-19 12:38:57'),(87,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/15f9d700-98b6-4c0f-bd84-d5f5e298cceb.png','TaxRefund','2026-03-19 12:39:53'),(88,NULL,'100023273149-鐢靛瓙鍙戠エ-26117000000099447157-20260112-103908.pdf','/api/v1/files/download?path=202603/79a720bb-cb89-432b-b3b1-eb3a0343cebd.pdf','TaxRefund','2026-03-19 13:00:43'),(89,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/87326807-1662-41ee-908f-dfe6551d8330.png','TaxRefund','2026-03-19 13:11:06'),(90,NULL,'100023273149-鐢靛瓙鍙戠エ-26117000000099447157-20260112-103908.pdf','/api/v1/files/download?path=202603/dfe4330d-9da5-4463-aae0-1b8043e1de9e.pdf','TaxRefund','2026-03-19 13:17:54'),(91,NULL,'dfe4330d-9da5-4463-aae0-1b8043e1de9e (1).pdf','/api/v1/files/download?path=202603/c3617ae1-c216-4316-910e-d2db93abd769.pdf','TaxRefund','2026-03-19 13:28:59'),(92,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/6d4e7dbc-1366-4e5b-aa36-eb264f0fe760.png','ProductPhoto','2026-03-20 13:25:11'),(95,24,'鍏ㄥ鎶ュ叧鍗曡瘉_DEC202603208003.xlsx','/api/v1/files/download?path=AllTemple_DEC202603208003_2a99f0e43f284a69ad084778ca6b983e.xlsx','AllDocuments','2026-03-20 14:24:03'),(96,NULL,'qrcode (9).png','/api/v1/files/download?path=202603/8817e860-8132-43dc-97b5-335d46aaa722.png','remittance','2026-03-20 14:24:38'),(97,24,'瀹氶噾姘村崟_DEC202603208003.xlsx','/api/v1/files/download?path=Remittance_瀹氶噾_DEC202603208003_a5f05fe794e4491a9956c9be94194a01.xlsx','Remittance_Deposit','2026-03-20 14:24:41'),(98,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (1).png','/api/v1/files/download?path=202603/968aa1c7-6e76-49e3-88da-5241dc67b680.png','remittance','2026-03-20 14:25:10'),(99,24,'灏炬姘村崟_DEC202603208003.xlsx','/api/v1/files/download?path=Remittance_灏炬_DEC202603208003_c5dd8fed91b64e75be17a69dd68e6e87.xlsx','Remittance_Balance','2026-03-20 14:25:12'),(100,NULL,'dfe4330d-9da5-4463-aae0-1b8043e1de9e.pdf','/api/v1/files/download?path=202603/3a90344e-3231-4f20-9efd-7957eacf45ad.pdf','PickupList','2026-03-20 14:27:22'),(102,NULL,'dfe4330d-9da5-4463-aae0-1b8043e1de9e (1).pdf','/api/v1/files/download?path=202603/8a2673d1-4890-4f9b-9eaa-8fdcb4d80860.pdf','PickupList','2026-03-20 14:28:08'),(103,24,'dfe4330d-9da5-4463-aae0-1b8043e1de9e (1).pdf','/api/v1/files/download?path=202603/8a2673d1-4890-4f9b-9eaa-8fdcb4d80860.pdf','PickupList','2026-03-20 14:28:08'),(104,NULL,'c3617ae1-c216-4316-910e-d2db93abd769.pdf','/api/v1/files/download?path=202603/bf53ef9d-4cc5-4153-9745-75749cfd8850.pdf','TaxRefund','2026-03-20 14:39:25'),(109,24,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603208003.xlsx','/api/v1/files/download?path=Declaration_DEC202603208003_7964ea0fead34a55ac7aefc3528dfeec.xlsx','FullDocuments','2026-03-20 16:24:49'),(110,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/8d7c1329-14dc-482c-aae8-8b12bf29406e.png','ProductPhoto','2026-03-20 16:40:32'),(111,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4.png','/api/v1/files/download?path=202603/edcdcc9b-2461-48ff-b8a7-d6b8b33f85fb.png','ProductPhoto','2026-03-20 16:40:54'),(112,25,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603203215.xlsx','/api/v1/files/download?path=Declaration_DEC202603203215_42cf2be3b0f94b3aa280d75ecf1d7d30.xlsx','FullDocuments','2026-03-20 16:45:57'),(113,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/b004d867-1aa2-407f-b067-2c1a0386810e.png','ProductPhoto','2026-03-20 16:56:50'),(114,26,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603208464.xlsx','/api/v1/files/download?path=Declaration_DEC202603208464_013454ca75934e30859dcc1fc2135e00.xlsx','FullDocuments','2026-03-20 16:57:41'),(115,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/ef50a7b5-0649-4068-90a0-3e843274c522.png','ProductPhoto','2026-03-20 17:06:28'),(116,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4.png','/api/v1/files/download?path=202603/d74c54b6-f43b-42a9-ac5a-7ee35e4b004a.png','ProductPhoto','2026-03-20 17:06:40'),(117,27,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603203278.xlsx','/api/v1/files/download?path=Declaration_DEC202603203278_8e36dff658444b4f80e46b1544fc3399.xlsx','FullDocuments','2026-03-20 17:07:06'),(118,NULL,'bf53ef9d-4cc5-4153-9745-75749cfd8850.pdf','/api/v1/files/download?path=202603/d2a75fc2-aec0-4f4b-b19e-d944e2559391.pdf','PickupList','2026-03-20 17:08:16'),(119,27,'bf53ef9d-4cc5-4153-9745-75749cfd8850.pdf','/api/v1/files/download?path=202603/d2a75fc2-aec0-4f4b-b19e-d944e2559391.pdf','PickupList','2026-03-20 17:08:16'),(120,28,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603236836.xlsx','/api/v1/files/download?path=DEC202603236836/Declaration_DEC202603236836_c491bf996444415baefe4e8eba6a5b8d.xlsx','FullDocuments','2026-03-23 10:02:17'),(121,29,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603237846.xlsx','/api/v1/files/download?path=DEC202603237846/Declaration_DEC202603237846_f333384352bf4c9db1b36e1d804048fd.xlsx','FullDocuments','2026-03-23 11:15:57'),(122,29,'鍏ㄥ鎶ュ叧鍗曡瘉_DEC202603237846.xlsx','/api/v1/files/download?path=DEC202603237846/AllTemple_DEC202603237846_c58cb09d93044ef38083efe297694e8f.xlsx','AllDocuments','2026-03-23 11:50:45'),(123,NULL,'EC20260323812262_1774248812263.docx','/api/v1/files/download?path=202603/d7a19871-68e6-406e-869a-50668e0c841d.docx','PickupList','2026-03-23 15:03:44'),(124,29,'EC20260323812262_1774248812263.docx','/api/v1/files/download?path=202603/d7a19871-68e6-406e-869a-50668e0c841d.docx','PickupList','2026-03-23 15:03:44'),(125,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/2992eaf8-16c6-450b-af79-b75eab3855eb.png','ProductPhoto','2026-03-24 12:51:39'),(126,30,'鍏ㄥ鍑哄彛鍗曡瘉_DEC20260324387.xlsx','/api/v1/files/download?path=DEC20260324387/Declaration_DEC20260324387_f4cb47b0eb1644fc9364a403351d663e.xlsx','FullDocuments','2026-03-24 12:52:50'),(127,30,'鍏ㄥ鎶ュ叧鍗曡瘉_DEC20260324387.xlsx','/api/v1/files/download?path=DEC20260324387/AllTemple_DEC20260324387_31d5d69b75f34295991778a1ee605571.xlsx','AllDocuments','2026-03-24 12:52:50'),(128,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (1).png','/api/v1/files/download?path=202603/0aaf4771-f949-4e2c-821e-1b650759bbf2.png','ProductPhoto','2026-03-24 15:31:04'),(129,31,'鍏ㄥ鍑哄彛鍗曡瘉_DEC202603246011.xlsx','/api/v1/files/download?path=DEC202603246011/Declaration_DEC202603246011_04c306a5e4e344e0803e3583dca16e8b.xlsx','FullDocuments','2026-03-24 15:31:25'),(130,31,'鍏ㄥ鎶ュ叧鍗曡瘉_DEC202603246011.xlsx','/api/v1/files/download?path=DEC202603246011/AllTemple_DEC202603246011_ad32d95810de4627a543c5022a838389.xlsx','AllDocuments','2026-03-24 15:31:25'),(131,NULL,'EC20260323629205_1774245629206 (1).docx','/api/v1/files/download?path=202603/852938c0-6965-4fbb-8d49-b800e3887c4f.docx','DeliveryOrder','2026-03-24 16:25:14'),(132,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/b1fdfdc8-6951-44ad-b5b1-352defcb9163.png','ProductPhoto','2026-03-24 16:58:12'),(133,32,'棰勫綍鍏ヨ〃鍗昣DEC202603248904.xlsx','/api/v1/files/download?path=DEC202603248904/棰勫綍鍏ヨ〃鍗昣DEC202603248904_2b04abebc1d9456dbfd9c12869f91542.xlsx','FullDocuments','2026-03-24 16:58:21'),(134,32,'鎶ュ叧琛ㄥ崟_DEC202603248904.xlsx','/api/v1/files/download?path=DEC202603248904/AllTemple_DEC202603248904_5521fc41ec2d4432905e38f3262bb1ae.xlsx','AllDocuments','2026-03-24 16:59:50'),(135,NULL,'AllTemple_DEC202603237140_023c5f2225984a18bd92fc6f9496ecc4.xlsx','/api/v1/files/download?path=202603/305ccb76-b2d4-4249-a144-6757a3cd637d.xlsx','finance_supplement','2026-03-24 17:08:01'),(136,NULL,'Declaration_DEC202603211430_8e22d038cf954ab88b4aa905c91f1abe.xlsx','/api/v1/files/download?path=202603/5e9015cb-46a9-4020-bff7-2c78aaf362ca.xlsx','finance_supplement','2026-03-24 17:08:07'),(137,NULL,'AllTemple_DEC202603237140_023c5f2225984a18bd92fc6f9496ecc4.xlsx','/api/v1/files/download?path=202603/04bb5d91-3b53-40b3-b725-fd1dd0d96753.xlsx','finance_supplement','2026-03-24 17:08:11'),(138,NULL,'EC20260323366297_1774243366310.docx','/api/v1/files/download?path=202603/1f7880f9-e311-4455-a8ae-b093ea164c51.docx','DeliveryOrder','2026-03-24 17:17:03'),(139,NULL,'EC20260323366297_1774243366310.docx','/api/v1/files/download?path=202603/f6b2d951-a030-45e3-8d73-bde20dbcc88a.docx','DeliveryOrder','2026-03-24 17:32:38'),(140,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/d4c136ea-b748-4c99-a73a-a7e370954446.png','remittance','2026-03-24 17:39:01'),(141,32,'瀹氶噾姘村崟_DEC202603248904.xlsx','/api/v1/files/download?path=DEC202603248904/Remittance_瀹氶噾_DEC202603248904_29e17b65e7c24544bdd664bda27e72c7.xlsx','Remittance_Deposit','2026-03-24 17:39:05'),(142,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/8776ef64-a76d-429a-b78c-b0d8c9d6307f.png','ProductPhoto','2026-03-24 18:04:35'),(143,33,'棰勫綍鍏ヨ〃鍗昣DEC202603244667.xlsx','/api/v1/files/download?path=DEC202603244667/棰勫綍鍏ヨ〃鍗昣DEC202603244667_a54ff139e8c04260b04fa6ab6c28fb6c.xlsx','FullDocuments','2026-03-24 18:04:47'),(144,33,'鎶ュ叧琛ㄥ崟_DEC202603244667.xlsx','/api/v1/files/download?path=DEC202603244667/AllTemple_DEC202603244667_ea1b358ffd01452382b13da0c56de4fd.xlsx','AllDocuments','2026-03-24 18:04:58'),(145,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/903ad6c4-260a-42f2-9cee-70440ce886ab.png','remittance','2026-03-24 18:05:22'),(146,33,'瀹氶噾姘村崟_DEC202603244667.xlsx','/api/v1/files/download?path=DEC202603244667/Remittance_瀹氶噾_DEC202603244667_b5f6017eb1fb47cca47df4c9c78116f9.xlsx','Remittance_Deposit','2026-03-24 18:05:23'),(147,NULL,'EC20260323366297_1774243366310.docx','/api/v1/files/download?path=202603/a6a6c866-1cbd-44a4-9437-79820ea6ea78.docx','DeliveryOrder','2026-03-24 18:05:56'),(148,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/d09b9a01-88c4-495a-811f-34fe619c2a22.png','ProductPhoto','2026-03-24 18:16:21'),(149,34,'棰勫綍鍏ヨ〃鍗昣DEC202603241260.xlsx','/api/v1/files/download?path=DEC202603241260/棰勫綍鍏ヨ〃鍗昣DEC202603241260_9a28fa7bbbcb4d7c82bf5c5c38e6f233.xlsx','FullDocuments','2026-03-24 18:16:32'),(150,34,'鎶ュ叧琛ㄥ崟_DEC202603241260.xlsx','/api/v1/files/download?path=DEC202603241260/AllTemple_DEC202603241260_97fa1b6d040549e09e38a0c3c2759fd7.xlsx','AllDocuments','2026-03-24 18:16:41'),(151,34,'瀹氶噾姘村崟_DEC202603241260.xlsx','/api/v1/files/download?path=DEC202603241260/Remittance_瀹氶噾_DEC202603241260_3a9f438efafa47fe8be7c151902fe15d.xlsx','Remittance_Deposit','2026-03-24 18:17:27'),(152,NULL,'EC20260323366297_1774243366310.docx','/api/v1/files/download?path=202603/ec225c4b-e9ea-453a-b870-d1b6d389083d.docx','DeliveryOrder','2026-03-24 18:18:24'),(153,34,'灏炬姘村崟_DEC202603241260.xlsx','/api/v1/files/download?path=DEC202603241260/Remittance_灏炬_DEC202603241260_edc3b492b74b4931ba8d2a8453ad5895.xlsx','Remittance_Balance','2026-03-24 18:39:25'),(154,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/32b57ff8-ec13-4d8d-9b40-29e283cd5cef.png','ProductPhoto','2026-03-25 09:23:03'),(155,35,'棰勫綍鍏ヨ〃鍗昣DEC202603254330.xlsx','/api/v1/files/download?path=DEC202603254330/棰勫綍鍏ヨ〃鍗昣DEC202603254330_0dd7460e49684b1e8f750cdf772a6ebd.xlsx','FullDocuments','2026-03-25 09:23:17'),(156,35,'鎶ュ叧琛ㄥ崟_DEC202603254330.xlsx','/api/v1/files/download?path=DEC202603254330/AllTemple_DEC202603254330_4aa042292c794697a4de206b4e4f7285.xlsx','AllDocuments','2026-03-25 09:23:49'),(157,35,'瀹氶噾姘村崟_DEC202603254330.xlsx','/api/v1/files/download?path=DEC202603254330/Remittance_瀹氶噾_DEC202603254330_b7bd295ae85e4c4abc081657d044bbc5.xlsx','Remittance_Deposit','2026-03-25 09:24:05'),(158,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/919ae69c-c7ad-4224-be12-c9139f5b196c.png','ProductPhoto','2026-03-25 09:36:26'),(159,36,'棰勫綍鍏ヨ〃鍗昣DEC202603255454.xlsx','/api/v1/files/download?path=DEC202603255454/棰勫綍鍏ヨ〃鍗昣DEC202603255454_55be3f392d6b4780bb84ffa55605f149.xlsx','FullDocuments','2026-03-25 09:36:42'),(160,36,'鎶ュ叧琛ㄥ崟_DEC202603255454.xlsx','/api/v1/files/download?path=DEC202603255454/AllTemple_DEC202603255454_4c4af7aa3ec14deca1eef23c88425a4a.xlsx','AllDocuments','2026-03-25 09:37:27'),(161,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/a1dd0b25-0725-4453-936c-b9f87cabd216.png','ProductPhoto','2026-03-25 09:40:54'),(162,37,'棰勫綍鍏ヨ〃鍗昣DEC20260325426.xlsx','/api/v1/files/download?path=DEC20260325426/棰勫綍鍏ヨ〃鍗昣DEC20260325426_f2509fa3db244b4b9f61284077017eeb.xlsx','FullDocuments','2026-03-25 09:41:01'),(163,37,'鎶ュ叧琛ㄥ崟_DEC20260325426.xlsx','/api/v1/files/download?path=DEC20260325426/AllTemple_DEC20260325426_76e035e1f60c4d7b850eb91340fc3ef7.xlsx','AllDocuments','2026-03-25 09:41:04'),(164,38,'棰勫綍鍏ヨ〃鍗昣DEC202603259027.xlsx','/api/v1/files/download?path=DEC202603259027/棰勫綍鍏ヨ〃鍗昣DEC202603259027_03e968d950ec4c2a98c0636ce1c00779.xlsx','FullDocuments','2026-03-25 09:44:20'),(165,38,'鎶ュ叧琛ㄥ崟_DEC202603259027.xlsx','/api/v1/files/download?path=DEC202603259027/AllTemple_DEC202603259027_bd10132d138243d7ad350b768cf6c06c.xlsx','AllDocuments','2026-03-25 09:44:24'),(166,39,'棰勫綍鍏ヨ〃鍗昣DEC202603258168.xlsx','/api/v1/files/download?path=DEC202603258168/棰勫綍鍏ヨ〃鍗昣DEC202603258168_4ea717d63fd24a04bdd588cea83ec5dc.xlsx','FullDocuments','2026-03-25 09:53:44'),(167,39,'鎶ュ叧琛ㄥ崟_DEC202603258168.xlsx','/api/v1/files/download?path=DEC202603258168/AllTemple_DEC202603258168_06b54b91f9d64199b039cd0d0fc360bd.xlsx','AllDocuments','2026-03-25 09:53:47'),(168,40,'棰勫綍鍏ヨ〃鍗昣DEC202603253562.xlsx','/api/v1/files/download?path=DEC202603253562/棰勫綍鍏ヨ〃鍗昣DEC202603253562_2ac19569cb6a4652af4b4393994fac18.xlsx','FullDocuments','2026-03-25 09:59:06'),(169,40,'鎶ュ叧琛ㄥ崟_DEC202603253562.xlsx','/api/v1/files/download?path=DEC202603253562/AllTemple_DEC202603253562_bd2e2341f9244f4f8d4a81c5fcf29611.xlsx','AllDocuments','2026-03-25 09:59:09'),(170,41,'棰勫綍鍏ヨ〃鍗昣DEC202603256635.xlsx','/api/v1/files/download?path=DEC202603256635/棰勫綍鍏ヨ〃鍗昣DEC202603256635_052aae1e194b4653924cd40082333978.xlsx','FullDocuments','2026-03-25 10:07:09'),(171,41,'鎶ュ叧琛ㄥ崟_DEC202603256635.xlsx','/api/v1/files/download?path=DEC202603256635/AllTemple_DEC202603256635_e5fa9e52e7ec474a83c8f12d747de0d6.xlsx','AllDocuments','2026-03-25 10:07:13'),(172,41,'瀹氶噾姘村崟_DEC202603256635.xlsx','/api/v1/files/download?path=DEC202603256635/Remittance_瀹氶噾_DEC202603256635_2abf32756473482989834161085bbecf.xlsx','Remittance_Deposit','2026-03-25 10:07:21'),(173,NULL,'闇€姹傛竻鍗?docx','/api/v1/files/download?path=202603/bec90641-9b8c-4282-bb35-485c026fd7d0.docx','DeliveryOrder','2026-03-25 10:26:53'),(175,NULL,'宸ヤ綔璁″垝琛?xlsx','/api/v1/files/download?path=202603/1d8cb4d6-25a5-4e46-ba35-cb8a3c1ae721.xlsx','finance_supplement','2026-03-25 10:43:49'),(176,42,'棰勫綍鍏ヨ〃鍗昣DEC202603257580.xlsx','/api/v1/files/download?path=DEC202603257580/棰勫綍鍏ヨ〃鍗昣DEC202603257580_65d89a60aa3d4959be1ffdd9918bbe81.xlsx','FullDocuments','2026-03-25 10:44:50'),(177,42,'鎶ュ叧琛ㄥ崟_DEC202603257580.xlsx','/api/v1/files/download?path=DEC202603257580/AllTemple_DEC202603257580_e89c157f42be43b2a39e4a597be25d9e.xlsx','AllDocuments','2026-03-25 10:44:58'),(178,43,'棰勫綍鍏ヨ〃鍗昣DEC202603256924.xlsx','/api/v1/files/download?path=DEC202603256924/棰勫綍鍏ヨ〃鍗昣DEC202603256924_52a77921908c46439e109f9d32acd677.xlsx','FullDocuments','2026-03-25 10:54:19'),(179,43,'鎶ュ叧琛ㄥ崟_DEC202603256924.xlsx','/api/v1/files/download?path=DEC202603256924/AllTemple_DEC202603256924_3f43df2d62054341bd19a7a7fc835757.xlsx','AllDocuments','2026-03-25 10:54:25'),(180,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/0efe1241-bd17-4686-841c-bfbb24596fb1.xlsx','finance_supplement','2026-03-25 11:24:45'),(181,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/72511f0b-8ce9-4d80-b221-361c60084001.xlsx','finance_supplement','2026-03-25 11:24:48'),(182,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/ca25dcf7-f5f8-4ae8-877e-53e979919f70.xlsx','finance_supplement','2026-03-25 11:24:52'),(183,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/8f6b3a78-b8f4-427f-9ca4-007d19aeb1f0.xlsx','Common','2026-03-25 13:29:20'),(184,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/9208b123-cd7a-49da-9e56-c985afb0246e.xlsx','Common','2026-03-25 13:29:26'),(185,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/6c2f584d-2481-448e-9567-0b011df81f40.xlsx','Common','2026-03-25 13:29:29'),(186,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/21d5c0db-a92c-4c14-8585-e0b2016dfe9b.xlsx','Common','2026-03-25 13:37:46'),(187,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/9be9dd98-9c07-4e8d-81a9-2333f310f42a.xlsx','Common','2026-03-25 13:45:36'),(188,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/fbad3de1-babf-42f6-8a84-bd5981be1d16.xlsx','Common','2026-03-25 13:45:47'),(189,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/70f8c275-2d32-44ca-899c-1687bf484921.xlsx','Common','2026-03-25 13:49:56'),(190,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/7ec3a33b-6986-4d05-b980-00bc2f4b07ac.xlsx','Common','2026-03-25 13:51:31'),(191,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/86169d2d-0803-4c7d-8c28-318f0dff7fa8.xlsx','Common','2026-03-25 13:51:39'),(192,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/d9137aaa-203b-4aa2-a71f-9a5b5900e4fd.xlsx','Common','2026-03-25 13:53:59'),(193,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/aebf5bba-512c-4112-b89f-1a4f8e798878.xlsx','Common','2026-03-25 13:54:06'),(194,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/a424996f-7550-46ea-8196-5abf1198f34a.xlsx','Common','2026-03-25 13:54:16'),(195,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/27c9ecc4-4998-41dc-ae3c-d0aafbe6728a.xlsx','Common','2026-03-25 13:54:22'),(196,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/1788b706-ae46-4136-a7b1-35e735b3aa33.xlsx','Common','2026-03-25 13:55:27'),(197,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/c3e0d18e-f278-4c8a-b029-4fa59c1c8b06.xlsx','Common','2026-03-25 13:55:34'),(198,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/ccabfba1-af84-4990-b8d0-254cc3ba6300.xlsx','Common','2026-03-25 13:55:37'),(199,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/f33daa3b-d667-4330-916c-594ab040b763.xlsx','Common','2026-03-25 13:58:05'),(200,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/dbd0dde5-efca-4b1b-b3a0-ec1ce5d80848.xlsx','Common','2026-03-25 13:58:08'),(201,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/2b16b292-e7a9-4209-b1b3-a3df696d4e34.xlsx','Common','2026-03-25 13:58:10'),(202,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/00d4db16-53a7-4881-8fba-01b67c583f4a.xlsx','Common','2026-03-25 14:02:47'),(203,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/c23a9f49-8e63-4fbb-aea9-66390a372d51.xlsx','Common','2026-03-25 14:04:58'),(204,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/2fc7a0c9-57ff-4cc3-9bfb-f1dab7b418e1.xlsx','Common','2026-03-25 14:05:03'),(205,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/032353fd-1afd-418d-9adc-fb02d6f02cc0.xlsx','Common','2026-03-25 14:05:06'),(206,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/199eab67-a5e5-42cb-8a5f-0bd5a229e18c.xlsx','Common','2026-03-25 14:07:57'),(207,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/8d830666-1742-4c10-9193-544eb4b510d2.xlsx','Common','2026-03-25 14:08:00'),(208,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/7452a698-0c3e-4d43-9ddf-4269a5d67448.xlsx','Common','2026-03-25 14:08:03'),(209,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/9b4291df-a4ab-45a0-9ef8-91c5fcfbfc58.xlsx','Common','2026-03-25 14:10:05'),(210,NULL,'sys_user.xlsx','/api/v1/files/download?path=202603/f6969909-a3ea-438c-a18f-8a26a3dad28c.xlsx','Common','2026-03-25 14:10:08'),(211,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/1852cb2e-134b-49f7-babd-3626379c48a7.xlsx','Common','2026-03-25 14:10:11'),(212,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/f7fa5985-2cc4-480f-ba1a-63ec56b597f1.xlsx','Common','2026-03-25 14:22:41'),(213,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/08bcf3b6-ec2d-4881-a82b-8b857d16b382.xlsx','Common','2026-03-25 14:22:45'),(214,NULL,'TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/571c0a6b-1aed-4df7-a846-67c1377440b8.xlsx','Common','2026-03-25 14:22:47'),(215,NULL,'f6ad0ce4-4530-4bf7-aa4c-23e92538dcb4 (2).png','/api/v1/files/download?path=202603/1fcb34e8-de97-4dd0-a28e-c0f26a80ffd5.png','ProductPhoto','2026-03-25 15:06:44'),(216,44,'棰勫綍鍏ヨ〃鍗昣DEC202603254864.xlsx','/api/v1/files/download?path=DEC202603254864/棰勫綍鍏ヨ〃鍗昣DEC202603254864_b7b405d8e3224845bf4897adb25740e2.xlsx','FullDocuments','2026-03-25 15:06:56'),(217,44,'鎶ュ叧琛ㄥ崟_DEC202603254864.xlsx','/api/v1/files/download?path=DEC202603254864/AllTemple_DEC202603254864_989b8c5773544ecba25e838cfeea4d77.xlsx','AllDocuments','2026-03-25 15:07:09'),(218,44,'瀹氶噾姘村崟_DEC202603254864.xlsx','/api/v1/files/download?path=DEC202603254864/Remittance_瀹氶噾_DEC202603254864_1537ee9b144b4297b69ae9e01371a90e.xlsx','Remittance_Deposit','2026-03-25 15:07:41'),(219,44,'灏炬姘村崟_DEC202603254864.xlsx','/api/v1/files/download?path=DEC202603254864/Remittance_灏炬_DEC202603254864_3b104c35070b4f03a30a04457a8cf3a8.xlsx','Remittance_Balance','2026-03-25 15:08:08'),(220,NULL,'EC20260325917708_1774416917727.docx','/api/v1/files/download?path=202603/f79dcdc7-e874-412a-851b-4d79d4fa5e12.docx','DeliveryOrder','2026-03-25 15:08:28'),(221,NULL,'EC20260325917708_1774416917727.docx','/api/v1/files/download?path=202603/52e017f3-b352-4fea-9e14-dd151e06d19b.docx','Common','2026-03-25 15:08:55'),(222,NULL,'EC20260325917708_1774416917727.docx','/api/v1/files/download?path=202603/43fc3d6c-3f1f-4ef2-afe7-441a23c203a1.docx','Common','2026-03-25 15:08:58'),(223,NULL,'571c0a6b-1aed-4df7-a846-67c1377440b8.xlsx','/api/v1/files/download?path=202603/f5137ede-6982-4fb0-8fb6-fb5bdf069147.xlsx','Common','2026-03-25 15:09:01');
/*!40000 ALTER TABLE `declaration_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `declaration_carton`
--

DROP TABLE IF EXISTS `declaration_carton`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `declaration_carton` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `form_id` bigint NOT NULL COMMENT '鐢虫姤鍗旾D',
  `carton_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '绠卞彿',
  `quantity` int NOT NULL COMMENT '绠卞瓙鏁伴噺',
  `volume` decimal(10,3) NOT NULL COMMENT '鍗曠浣撶Н(CBM)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_carton_no` (`carton_no`),
  CONSTRAINT `fk_declaration_carton_form` FOREIGN KEY (`form_id`) REFERENCES `declaration_form` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢虫姤鍗曠瀛愪俊鎭〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `declaration_carton`
--

LOCK TABLES `declaration_carton` WRITE;
/*!40000 ALTER TABLE `declaration_carton` DISABLE KEYS */;
INSERT INTO `declaration_carton` VALUES (34,43,'CTN001',1,0.000,0,'2026-03-25 10:54:17','2026-03-25 10:54:17'),(35,44,'CTN001',11,10.000,0,'2026-03-25 15:06:55','2026-03-25 15:06:55');
/*!40000 ALTER TABLE `declaration_carton` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `declaration_carton_product`
--

DROP TABLE IF EXISTS `declaration_carton_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `declaration_carton_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `carton_id` bigint NOT NULL COMMENT '绠卞瓙ID',
  `product_id` bigint NOT NULL COMMENT '浜у搧ID',
  `quantity` int NOT NULL COMMENT '璇ョ涓骇鍝佹暟閲?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_carton_id` (`carton_id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_carton_product_carton` FOREIGN KEY (`carton_id`) REFERENCES `declaration_carton` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_carton_product_product` FOREIGN KEY (`product_id`) REFERENCES `declaration_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绠卞瓙浜у搧鍏宠仈琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `declaration_carton_product`
--

LOCK TABLES `declaration_carton_product` WRITE;
/*!40000 ALTER TABLE `declaration_carton_product` DISABLE KEYS */;
INSERT INTO `declaration_carton_product` VALUES (39,34,39,1,'2026-03-25 10:54:17'),(40,35,40,1111,'2026-03-25 15:06:55');
/*!40000 ALTER TABLE `declaration_carton_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `declaration_delivery_order`
--

DROP TABLE IF EXISTS `declaration_delivery_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `declaration_delivery_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `form_id` bigint NOT NULL COMMENT '鍏宠仈鐢虫姤鍗旾D',
  `delivery_date` date DEFAULT NULL COMMENT '鎻愯揣鏃ユ湡',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '澶囨敞璇存槑',
  `file_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎻愯揣鍗曟枃浠禪RL',
  `file_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎻愯揣鍗曟枃浠跺悕',
  `status` tinyint DEFAULT '0' COMMENT '鐘舵€侊細0-寰呭鏍?1-宸插鏍?2-宸查┏鍥?,
  `audit_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '瀹℃牳澶囨敞',
  `audit_by` bigint DEFAULT NULL COMMENT '瀹℃牳浜篒D',
  `audit_time` datetime DEFAULT NULL COMMENT '瀹℃牳鏃堕棿',
  `created_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜篒D',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢虫姤鍗曟彁璐у崟琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `declaration_delivery_order`
--

LOCK TABLES `declaration_delivery_order` WRITE;
/*!40000 ALTER TABLE `declaration_delivery_order` DISABLE KEYS */;
INSERT INTO `declaration_delivery_order` VALUES (1,32,'2026-03-24','111','/api/v1/files/download?path=202603/f6b2d951-a030-45e3-8d73-bde20dbcc88a.docx','EC20260323366297_1774243366310.docx',0,NULL,NULL,NULL,1,'2026-03-24 17:32:40','2026-03-24 17:32:40'),(2,33,'2026-03-24','11','/api/v1/files/download?path=202603/a6a6c866-1cbd-44a4-9437-79820ea6ea78.docx','EC20260323366297_1774243366310.docx',0,NULL,NULL,NULL,1,'2026-03-24 18:05:57','2026-03-24 18:05:57'),(3,34,'2026-03-24','111','/api/v1/files/download?path=202603/ec225c4b-e9ea-453a-b870-d1b6d389083d.docx','EC20260323366297_1774243366310.docx',0,NULL,NULL,NULL,1,'2026-03-24 18:18:24','2026-03-24 18:18:24'),(4,41,'2026-03-25','','/api/v1/files/download?path=202603/bec90641-9b8c-4282-bb35-485c026fd7d0.docx','闇€姹傛竻鍗?docx',0,NULL,NULL,NULL,1,'2026-03-25 10:26:56','2026-03-25 10:26:56'),(5,43,'2026-03-25','','','',0,NULL,NULL,NULL,1,'2026-03-25 10:54:50','2026-03-25 10:54:50'),(6,44,'2026-03-25','','/api/v1/files/download?path=202603/f79dcdc7-e874-412a-851b-4d79d4fa5e12.docx','EC20260325917708_1774416917727.docx',0,NULL,NULL,NULL,1,'2026-03-25 15:08:32','2026-03-25 15:08:32');
/*!40000 ALTER TABLE `declaration_delivery_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `declaration_draft`
--

DROP TABLE IF EXISTS `declaration_draft`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `declaration_draft` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `user_id` bigint NOT NULL COMMENT '鎵€灞炵敤鎴稩D',
  `org_id` bigint DEFAULT NULL COMMENT '鎵€灞炵粍缁嘔D',
  `form_no` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鐢虫姤鍗曞彿',
  `shipper_company` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍙戣揣浜?,
  `consignee_company` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鏀惰揣浜?,
  `total_amount` decimal(15,2) DEFAULT '0.00' COMMENT '鎬婚噾棰?,
  `form_data` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鏁翠釜琛ㄥ崟鐨凧SON鏁版嵁',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_user_org` (`user_id`,`org_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢虫姤鍗曡崏绋跨琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `declaration_draft`
--

LOCK TABLES `declaration_draft` WRITE;
/*!40000 ALTER TABLE `declaration_draft` DISABLE KEYS */;
/*!40000 ALTER TABLE `declaration_draft` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `declaration_element_value`
--

DROP TABLE IF EXISTS `declaration_element_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `declaration_element_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `product_id` bigint NOT NULL COMMENT '浜у搧ID',
  `element_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '瑕佺礌鍚嶇О',
  `element_value` text COLLATE utf8mb4_unicode_ci COMMENT '瑕佺礌鍊?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_declaration_element_product` FOREIGN KEY (`product_id`) REFERENCES `declaration_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=213 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢虫姤瑕佺礌濉啓璁板綍琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `declaration_element_value`
--

LOCK TABLES `declaration_element_value` WRITE;
/*!40000 ALTER TABLE `declaration_element_value` DISABLE KEYS */;
INSERT INTO `declaration_element_value` VALUES (200,39,'鐢ㄩ€?,'鎻愪緵鍞竴鏁版嵁','2026-03-25 10:54:17','2026-03-25 10:54:17'),(201,39,'鏄惁褰曞埗','鍚?,'2026-03-25 10:54:17','2026-03-25 10:54:17'),(202,39,'鍝佺墝','鏃?,'2026-03-25 10:54:17','2026-03-25 10:54:17'),(203,39,'鍨嬪彿','鏃?,'2026-03-25 10:54:17','2026-03-25 10:54:17'),(204,39,'鍝佺墝绫诲瀷','鏃?,'2026-03-25 10:54:17','2026-03-25 10:54:17'),(205,39,'鍑哄彛浜儬鎯呭喌','涓嶇‘瀹?,'2026-03-25 10:54:17','2026-03-25 10:54:17'),(206,40,'鐢ㄩ€?,'鎵弿鍞竴鐮?,'2026-03-25 15:06:55','2026-03-25 15:06:55'),(207,40,'鍘熺悊','鐢靛瓙灏勯鎵弿','2026-03-25 15:06:55','2026-03-25 15:06:55'),(208,40,'鍔熻兘','鑾峰彇鏃犳簮灏勯鏍囩鏁版嵁','2026-03-25 15:06:55','2026-03-25 15:06:55'),(209,40,'鍝佺墝','鏃?,'2026-03-25 15:06:55','2026-03-25 15:06:55'),(210,40,'鍨嬪彿','鏃?,'2026-03-25 15:06:55','2026-03-25 15:06:55'),(211,40,'鍝佺墝绫诲瀷','鏃?,'2026-03-25 15:06:55','2026-03-25 15:06:55'),(212,40,'鍑哄彛浜儬鎯呭喌','涓嶇‘瀹?,'2026-03-25 15:06:55','2026-03-25 15:06:55');
/*!40000 ALTER TABLE `declaration_element_value` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `declaration_form`
--

DROP TABLE IF EXISTS `declaration_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `declaration_form` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `form_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐢虫姤鍗曞彿',
  `shipper_company` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍙戣揣浜哄叕鍙稿悕',
  `shipper_address` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍙戣揣浜哄湴鍧€',
  `consignee_company` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鏀惰揣浜哄叕鍙稿悕',
  `consignee_address` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鏀惰揣浜哄湴鍧€',
  `invoice_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍙戠エ鍙?,
  `declaration_date` date NOT NULL COMMENT '鐢虫姤鏃ユ湡',
  `transport_mode` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '杩愯緭鏂瑰紡',
  `departure_city` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍑哄彂鍩庡競',
  `destination_country` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐩殑鍦板尯鍩?,
  `destination_country_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鐩殑鍥戒唬鐮?(鍏宠仈 country_info.country_code)',
  `destination_country_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鐩殑鍥藉悕绉?(鍐椾綑瀛楁锛岀敤浜庡揩閫熷睍绀猴紝鍙€?',
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '甯佺',
  `total_quantity` int NOT NULL DEFAULT '0' COMMENT '鎬绘暟閲?,
  `total_amount` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '鎬婚噾棰?,
  `total_cartons` int NOT NULL DEFAULT '0' COMMENT '鎬荤鏁?,
  `total_gross_weight` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '鎬绘瘺閲?KGS)',
  `total_net_weight` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '鎬诲噣閲?KGS)',
  `total_volume` decimal(10,3) NOT NULL DEFAULT '0.000' COMMENT '鎬讳綋绉?CBM)',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '鐘舵€?0-鑽夌 1-宸叉彁浜?2-宸插鏍?3-宸插畬鎴?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  `org_id` bigint DEFAULT NULL COMMENT '鎵€灞炵粍缁嘔D',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囧織 0-姝ｅ父 1-鍒犻櫎',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_no` (`form_no`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`),
  KEY `idx_dest_country_code` (`destination_country_code`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍑哄彛鐢虫姤鍗曚富琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `declaration_form`
--

LOCK TABLES `declaration_form` WRITE;
/*!40000 ALTER TABLE `declaration_form` DISABLE KEYS */;
INSERT INTO `declaration_form` VALUES (43,'DEC202603256924','NINGBO ZIYI TECHNOLOGY CO.,LTD','XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA','111','111','ZIYI-26-0324','2026-03-25','AIR','SHANGHAI, CHINA','United States',NULL,NULL,'USD',1,0.00,0,0.00,0.00,0.000,8,'2026-03-25 10:54:17','2026-03-25 10:54:17',1,1,NULL,0),(44,'DEC202603254864','NINGBO ZIYI TECHNOLOGY CO.,LTD','XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA','111','111','ZIYI-26-0324','2026-03-25','TRUCK','SHANGHAI, CHINA','United Kingdom',NULL,NULL,'USD',1111,11110.00,0,10.00,10.00,10.000,8,'2026-03-25 15:06:55','2026-03-25 15:06:55',1,1,NULL,0);
/*!40000 ALTER TABLE `declaration_form` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `declaration_history`
--

DROP TABLE IF EXISTS `declaration_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `declaration_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `declaration_id` bigint NOT NULL COMMENT '鐢虫姤鏁版嵁ID',
  `version` int NOT NULL COMMENT '鐗堟湰鍙?,
  `hs_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HS缂栫爜',
  `english_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鑻辨枃鍚嶇О',
  `chinese_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '涓枃鍚嶇О',
  `declaration_elements` json DEFAULT NULL COMMENT '鐢虫姤瑕佺礌JSON鏁版嵁',
  `created_by` bigint NOT NULL COMMENT '鍒涘缓浜?,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_declaration_id` (`declaration_id`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_history_version` (`declaration_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢虫姤鍘嗗彶璁板綍琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `declaration_history`
--

LOCK TABLES `declaration_history` WRITE;
/*!40000 ALTER TABLE `declaration_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `declaration_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `declaration_product`
--

DROP TABLE IF EXISTS `declaration_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `declaration_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `form_id` bigint NOT NULL COMMENT '鐢虫姤鍗旾D',
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '浜у搧鍚嶇О',
  `hs_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HS缂栫爜',
  `quantity` int NOT NULL COMMENT '鏁伴噺',
  `unit` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍗曚綅',
  `unit_price` decimal(15,4) NOT NULL COMMENT '鍗曚环',
  `amount` decimal(15,2) NOT NULL COMMENT '閲戦',
  `gross_weight` decimal(10,2) NOT NULL COMMENT '姣涢噸(KGS)',
  `net_weight` decimal(10,2) NOT NULL COMMENT '鍑€閲?KGS)',
  `cartons` int NOT NULL DEFAULT '1' COMMENT '绠辨暟',
  `volume` decimal(10,3) NOT NULL DEFAULT '0.000' COMMENT '浣撶Н(CBM)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `image_id` bigint DEFAULT NULL COMMENT '浜у搧鍥剧墖ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_hs_code` (`hs_code`),
  CONSTRAINT `fk_declaration_product_form` FOREIGN KEY (`form_id`) REFERENCES `declaration_form` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢虫姤鍗曚骇鍝佹槑缁嗚〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `declaration_product`
--

LOCK TABLES `declaration_product` WRITE;
/*!40000 ALTER TABLE `declaration_product` DISABLE KEYS */;
INSERT INTO `declaration_product` VALUES (39,43,'RFID STICKER','8523591000',1,'PCS',0.0000,0.00,0.00,0.00,1,0.000,0,NULL,'2026-03-25 10:54:17','2026-03-25 10:54:17'),(40,44,'Handheld reader','9031809090',1111,'PCS',10.0000,11110.00,10.00,10.00,11,10.000,0,215,'2026-03-25 15:06:55','2026-03-25 15:06:55');
/*!40000 ALTER TABLE `declaration_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `declaration_remittance`
--

DROP TABLE IF EXISTS `declaration_remittance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `declaration_remittance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `form_id` bigint NOT NULL COMMENT '鐢虫姤鍗旾D',
  `remittance_type` tinyint NOT NULL DEFAULT '1' COMMENT '姘村崟绫诲瀷: 1-瀹氶噾, 2-灏炬',
  `remittance_name` varchar(100) NOT NULL COMMENT '鏀舵眹鍚嶇О',
  `remittance_date` date NOT NULL COMMENT '鏀舵眹鏃ユ湡',
  `remittance_amount` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '鏀舵眹閲戦($)',
  `exchange_rate` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '褰撴棩姹囩巼',
  `bank_fee` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '鎵嬬画璐?$)',
  `credited_amount` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '鍏ヨ处閲戦($)',
  `remarks` text COMMENT '澶囨敞',
  `photo_url` varchar(500) DEFAULT NULL COMMENT '姘村崟鐓х墖 URL',
  `status` tinyint DEFAULT '0' COMMENT '鐘舵€侊細0-寰呭鏍?1-宸插鏍?2-宸查┏鍥?,
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '瀹℃牳澶囨敞',
  `audit_by` bigint DEFAULT NULL COMMENT '瀹℃牳浜篒D',
  `audit_time` datetime DEFAULT NULL COMMENT '瀹℃牳鏃堕棿',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='姘村崟淇℃伅琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `declaration_remittance`
--

LOCK TABLES `declaration_remittance` WRITE;
/*!40000 ALTER TABLE `declaration_remittance` DISABLE KEYS */;
INSERT INTO `declaration_remittance` VALUES (1,5,1,'瀹氶噾','2026-03-16',0.0000,1.0000,0.0000,0.0000,NULL,NULL,0,NULL,NULL,NULL,'2026-03-16 15:48:08','2026-03-16 15:48:08'),(2,9,1,'瀹氶噾姘村崟','2026-03-17',111.0000,7.2000,30.0000,111.0000,NULL,'/api/v1/files/download?id=26',0,NULL,NULL,NULL,'2026-03-17 13:53:10','2026-03-17 13:53:10'),(3,9,1,'瀹氶噾姘村崟','2026-03-17',111.0000,7.2000,30.0000,211.0000,NULL,'',0,NULL,NULL,NULL,'2026-03-17 13:53:33','2026-03-17 13:53:33'),(4,10,1,'瀹氶噾姘村崟','2026-03-17',41111.0000,2.0000,30.0000,0.0000,NULL,'/api/v1/files/download?id=29',0,NULL,NULL,NULL,'2026-03-17 14:30:32','2026-03-17 14:30:32'),(5,10,2,'灏炬姘村崟','2026-03-17',111.0000,7.2000,30.0000,11.0000,'','/api/v1/files/download?id=31',0,NULL,NULL,NULL,'2026-03-17 14:31:31','2026-03-17 14:31:31'),(6,22,1,'瀹氶噾姘村崟','2026-03-18',1101.0000,7.2000,30.0000,11.0000,'','/api/v1/files/download?id=50',0,NULL,NULL,NULL,'2026-03-18 14:35:20','2026-03-18 14:35:20'),(7,22,1,'瀹氶噾姘村崟','2026-03-18',1101.0000,7.2000,30.0000,11.0000,NULL,'/api/v1/files/download?id=50',0,NULL,NULL,NULL,'2026-03-18 14:35:24','2026-03-18 14:35:24'),(8,22,1,'瀹氶噾姘村崟','2026-03-18',1101.0000,7.2000,30.0000,11.0000,NULL,'/api/v1/files/download?id=50',0,NULL,NULL,NULL,'2026-03-18 14:35:53','2026-03-18 14:35:53'),(9,22,1,'瀹氶噾姘村崟','2026-03-18',1101.0000,7.2000,30.0000,11.0000,NULL,'/api/v1/files/download?id=50',0,NULL,NULL,NULL,'2026-03-18 14:40:05','2026-03-18 14:40:05'),(10,22,1,'瀹氶噾姘村崟','2026-03-18',1101.0000,7.2000,30.0000,11.0000,NULL,'/api/v1/files/download?id=50',0,NULL,NULL,NULL,'2026-03-18 14:47:19','2026-03-18 14:47:19'),(11,22,2,'灏炬姘村崟','2026-03-18',0.0000,7.2000,30.0000,0.0000,NULL,'',0,NULL,NULL,NULL,'2026-03-18 14:54:24','2026-03-18 14:54:24'),(12,23,1,'瀹氶噾姘村崟','2026-03-18',1110.0000,7.2000,30.0000,1110.0000,NULL,'/api/v1/files/download?id=67',0,NULL,NULL,NULL,'2026-03-18 15:59:44','2026-03-18 15:59:44'),(13,23,2,'灏炬姘村崟','2026-03-18',1110.0000,7.2000,30.0000,10.0000,NULL,'/api/v1/files/download?id=70',0,NULL,NULL,NULL,'2026-03-18 16:00:35','2026-03-18 16:00:35'),(14,24,1,'瀹氶噾姘村崟','2026-03-20',1110.0000,7.2000,30.0000,1110.0000,NULL,'/api/v1/files/download?id=96',0,NULL,NULL,NULL,'2026-03-20 14:24:41','2026-03-20 14:24:41'),(15,24,2,'灏炬姘村崟','2026-03-20',1110.0000,7.2000,30.0000,1110.0000,NULL,'/api/v1/files/download?id=98',0,NULL,NULL,NULL,'2026-03-20 14:25:12','2026-03-20 14:25:12'),(16,32,1,'瀹氶噾姘村崟','2026-03-24',110.0000,7.2000,0.0000,0.0000,NULL,'/api/v1/files/download?id=140',0,NULL,NULL,NULL,'2026-03-24 17:39:05','2026-03-24 17:39:05'),(17,33,1,'瀹氶噾姘村崟','2026-03-24',10.0000,7.2000,0.0000,0.0000,NULL,'/api/v1/files/download?id=145',0,NULL,NULL,NULL,'2026-03-24 18:05:23','2026-03-24 18:05:23'),(18,34,1,'瀹氶噾姘村崟','2026-03-24',10.0000,7.2000,0.0000,10.0000,NULL,'',0,NULL,NULL,NULL,'2026-03-24 18:17:27','2026-03-24 18:17:27'),(19,34,2,'灏炬姘村崟','2026-03-24',11.0000,7.2000,0.0000,0.0000,NULL,'',0,NULL,NULL,NULL,'2026-03-24 18:39:25','2026-03-24 18:39:25'),(20,35,1,'瀹氶噾姘村崟','2026-03-25',1000.0000,7.2000,0.0000,1000.0000,NULL,'',0,NULL,NULL,NULL,'2026-03-25 09:24:05','2026-03-25 09:24:05'),(21,41,1,'瀹氶噾姘村崟','2026-03-25',0.0000,7.2000,30.0000,0.0000,NULL,'',0,NULL,NULL,NULL,'2026-03-25 10:07:21','2026-03-25 10:07:21'),(22,44,1,'瀹氶噾姘村崟','2026-03-25',110.0000,7.0000,0.0000,110.0000,NULL,'',0,NULL,NULL,NULL,'2026-03-25 15:07:41','2026-03-25 15:07:41'),(23,44,2,'灏炬姘村崟','2026-03-25',110.0000,6.9000,0.0000,110.0000,NULL,'',0,NULL,NULL,NULL,'2026-03-25 15:08:08','2026-03-25 15:08:08');
/*!40000 ALTER TABLE `declaration_remittance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `financial_supplement`
--

DROP TABLE IF EXISTS `financial_supplement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `financial_supplement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `form_id` bigint NOT NULL COMMENT '鍏宠仈鐢虫姤鍗旾D',
  `form_no` varchar(64) NOT NULL COMMENT '鍏宠仈鐢虫姤鍗曞彿',
  `freight_amount` decimal(15,2) DEFAULT NULL COMMENT '璐т唬閲戦',
  `freight_invoice_no` varchar(64) DEFAULT NULL COMMENT '璐т唬鍙戠エ鍙?,
  `freight_file_name` varchar(255) DEFAULT NULL COMMENT '璐т唬鍙戠エ鏂囦欢鍚?,
  `freight_file_url` varchar(500) DEFAULT NULL COMMENT '璐т唬鍙戠エURL',
  `customs_amount` decimal(15,2) DEFAULT NULL COMMENT '鎶ュ叧浠ｇ悊閲戦',
  `customs_invoice_no` varchar(64) DEFAULT NULL COMMENT '鎶ュ叧鍙戠エ鍙?,
  `customs_file_name` varchar(255) DEFAULT NULL COMMENT '鎶ュ叧浠ｇ悊鍙戠エ鏂囦欢鍚?,
  `customs_file_url` varchar(500) DEFAULT NULL COMMENT '鎶ュ叧浠ｇ悊鍙戠エURL',
  `customs_receipt_file_name` varchar(255) DEFAULT NULL COMMENT '娴峰叧鍥炴墽鏂囦欢鍚?,
  `customs_receipt_file_url` varchar(500) DEFAULT NULL COMMENT '娴峰叧鍥炴墽URL',
  `tax_refund_amount` decimal(15,2) DEFAULT NULL COMMENT '閫€绋庨噾棰?,
  `tax_refund_rate` decimal(10,4) DEFAULT '0.0000' COMMENT '閫€绋庣偣(%)',
  `foreign_exchange_bank_id` bigint DEFAULT NULL COMMENT '澶栨眹閾惰ID',
  `foreign_exchange_bank` varchar(64) DEFAULT NULL COMMENT '澶栨眹閾惰',
  `bank_fee_rate` decimal(10,4) DEFAULT '0.0000' COMMENT '閾惰鎵嬬画璐圭巼(%)',
  `details_amount` decimal(15,2) DEFAULT NULL COMMENT '寮€绁ㄩ噾棰?,
  `details_invoice_no` varchar(64) DEFAULT NULL COMMENT '寮€绁ㄥ彿',
  `details_file_name` varchar(255) DEFAULT NULL COMMENT '寮€绁ㄦ槑缁嗘枃浠跺悕',
  `details_file_url` varchar(500) DEFAULT NULL COMMENT '寮€绁ㄦ槑缁哢RL',
  `currency` varchar(10) DEFAULT 'CNY' COMMENT '甯佺',
  `status` int DEFAULT '0' COMMENT '鐘舵€?0-寰呬笂浼? 1-宸叉彁浜?,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓鑰?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊鑰?,
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_foreign_exchange_bank_id` (`foreign_exchange_bank_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='璐㈠姟寮€绁ㄨˉ鍏呰〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `financial_supplement`
--

LOCK TABLES `financial_supplement` WRITE;
/*!40000 ALTER TABLE `financial_supplement` DISABLE KEYS */;
INSERT INTO `financial_supplement` VALUES (14,43,'DEC202603256924',11.00,'11','TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/f7fa5985-2cc4-480f-ba1a-63ec56b597f1.xlsx',111.00,'11','TP0070KR_Light_yellow.xlsx','/api/v1/files/download?path=202603/08bcf3b6-ec2d-4881-a82b-8b857d16b382.xlsx',NULL,NULL,NULL,11.0000,4,'',0.0200,0.00,'','寮€绁ㄦ槑缁嗚绠楀崟_DEC202603256924.xlsx','/api/v1/files/download?path=202603/a6127adc-3273-4b94-b01f-3b51734b3fa0.xlsx','USD',0,'2026-03-25 14:22:52','2026-03-25 15:04:40',1,1),(15,44,'',11.00,'11','EC20260325917708_1774416917727.docx','/api/v1/files/download?path=202603/52e017f3-b352-4fea-9e14-dd151e06d19b.docx',1.00,'11','EC20260325917708_1774416917727.docx','/api/v1/files/download?path=202603/43fc3d6c-3f1f-4ef2-afe7-441a23c203a1.docx',NULL,NULL,NULL,13.0000,4,NULL,0.0200,NULL,NULL,'寮€绁ㄦ槑缁嗚绠楀崟_DEC202603254864.xlsx','/api/v1/files/download?path=202603/1bc50cf6-a24b-42b7-affc-0c6ec2122759.xlsx','CNY',0,'2026-03-25 15:08:39','2026-03-25 15:12:08',1,1);
/*!40000 ALTER TABLE `financial_supplement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `measurement_units`
--

DROP TABLE IF EXISTS `measurement_units`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `measurement_units` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `unit_code` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍗曚綅浠ｇ爜',
  `unit_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍗曚綅鍚嶇О',
  `unit_name_en` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鑻辨枃鍗曚綅鍚嶇О',
  `unit_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍗曚綅绫诲瀷',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鍗曚綅鎻忚堪',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-绂佺敤 1-鍚敤',
  `sort` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unit_code` (`unit_code`),
  KEY `idx_unit_type` (`unit_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='璁￠噺鍗曚綅閰嶇疆琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `measurement_units`
--

LOCK TABLES `measurement_units` WRITE;
/*!40000 ALTER TABLE `measurement_units` DISABLE KEYS */;
INSERT INTO `measurement_units` VALUES (1,'01','涓?,'PCS','鏁伴噺鍗曚綅','鍩烘湰璁℃暟鍗曚綅',1,1,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(2,'02','鍙?,'SETS','鏁伴噺鍗曚綅','璁惧鍙版暟',1,2,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(3,'03','濂?,'SUITS','鏁伴噺鍗曚綅','鎴愬璁惧',1,3,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(4,'04','浠?,'ITEMS','鏁伴噺鍗曚綅','涓€鑸墿鍝佷欢鏁?,1,4,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(5,'05','鍙?,'ONLY','鏁伴噺鍗曚綅','鍔ㄧ墿銆佸櫒鍏风瓑',1,5,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(6,'06','寮?,'SHEETS','鏁伴噺鍗曚綅','绾稿紶銆佺エ璇佺瓑',1,6,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(7,'07','鏀?,'BRANCHES','鏁伴噺鍗曚綅','绗斻€佺鐘剁墿绛?,1,7,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(8,'08','鏍?,'ROOTS','鏁伴噺鍗曚綅','妫掔姸鐗?,1,8,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(9,'09','鏉?,'STRIPS','鏁伴噺鍗曚綅','闀挎潯褰㈢墿鍝?,1,9,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(10,'10','鍧?,'BLOCKS','鏁伴噺鍗曚綅','鍧楃姸鐗╁搧',1,10,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(11,'11','鍗冨厠','KILOS','閲嶉噺鍗曚綅','鍏枻',1,11,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(12,'12','鍏?,'GRAMS','閲嶉噺鍗曚綅','鍏?,1,12,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(13,'13','鍚?,'TONS','閲嶉噺鍗曚綅','鍏惃',1,13,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(14,'14','绔嬫柟绫?,'CBM','浣撶Н鍗曚綅','绔嬫柟绫?,1,14,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(15,'15','骞虫柟绫?,'SQM','闈㈢Н鍗曚綅','骞虫柟绫?,1,15,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(16,'16','绫?,'METERS','闀垮害鍗曚綅','绫?,1,16,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(17,'17','鍘樼背','CM','闀垮害鍗曚綅','鍘樼背',1,17,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(18,'18','姣背','MM','闀垮害鍗曚綅','姣背',1,18,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(19,'19','鍗?,'LITERS','瀹归噺鍗曚綅','鍗?,1,19,'2026-03-14 07:13:42','2026-03-14 07:13:42'),(20,'20','姣崌','ML','瀹归噺鍗曚綅','姣崌',1,20,'2026-03-14 07:13:42','2026-03-14 07:13:42');
/*!40000 ALTER TABLE `measurement_units` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `process_definition`
--

DROP TABLE IF EXISTS `process_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `process_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `name` varchar(100) DEFAULT NULL COMMENT '娴佺▼瀹氫箟鍚嶇О',
  `process_key` varchar(100) NOT NULL COMMENT '娴佺▼瀹氫箟KEY',
  `process_name` varchar(100) DEFAULT NULL COMMENT '娴佺▼鍚嶇О',
  `bpmn_xml` longtext COMMENT '娴佺▼XML鍐呭',
  `version` int DEFAULT '1' COMMENT '娴佺▼瀹氫箟鐗堟湰',
  `deployment_id` varchar(64) DEFAULT NULL COMMENT '閮ㄧ讲ID',
  `description` varchar(500) DEFAULT NULL COMMENT '娴佺▼瀹氫箟鎻忚堪',
  `category` varchar(50) DEFAULT NULL COMMENT '鍒嗙被',
  `status` tinyint DEFAULT '1' COMMENT '鐘舵€?0:鍋滅敤 1:鍚敤',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='涓氬姟娴佺▼瀹氫箟鎵╁睍璁板綍琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `process_definition`
--

LOCK TABLES `process_definition` WRITE;
/*!40000 ALTER TABLE `process_definition` DISABLE KEYS */;
INSERT INTO `process_definition` VALUES (4,NULL,'declarationProcess','鐢虫姤瀹℃壒鍏ㄧ敓鍛藉懆鏈熸祦绋?涓茶鐗?','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"http://www.flowable.org/processdef\">\n  <process id=\"declarationProcess\" name=\"鐢虫姤瀹℃壒鍏ㄧ敓鍛藉懆鏈熸祦绋?涓茶鐗?\" isExecutable=\"true\">\n    <startEvent id=\"startEvent1\" name=\"鍚姩鐢虫姤\" flowable:initiator=\"starterId\" />\n    <userTask id=\"deptAudit\" name=\"閮ㄩ棬鍒濆\" flowable:candidateGroups=\"DEPT_MANAGER\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${declarationTaskListener}\" />\n        <flowable:taskListener event=\"complete\" delegateExpression=\"${declarationTaskListener}\" />\n      </extensionElements>\n      <incoming>Flow_1fpr6j2</incoming>\n    </userTask>\n    <exclusiveGateway id=\"gw1\" name=\"鍒濆缁撴灉\" />\n    <userTask id=\"rejectHandler\" name=\"椹冲洖淇敼\" flowable:assignee=\"${starterId}\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${declarationTaskListener}\" />\n      </extensionElements>\n    </userTask>\n    <serviceTask id=\"genContractTask\" name=\"鑷姩鐢熸垚鍏ㄥ鍗曡瘉\" flowable:delegateExpression=\"${declarationServiceTask}\" />\n    <userTask id=\"depositPayment\" name=\"涓婁紶瀹氶噾鍑瘉\" flowable:assignee=\"${starterId}\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${declarationTaskListener}\" />\n      </extensionElements>\n    </userTask>\n    <userTask id=\"depositAudit\" name=\"璐㈠姟纭瀹氶噾\" flowable:candidateGroups=\"FINANCE_MANAGER\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${declarationTaskListener}\" />\n        <flowable:taskListener event=\"complete\" delegateExpression=\"${declarationTaskListener}\" />\n      </extensionElements>\n    </userTask>\n    <exclusiveGateway id=\"gw2\" name=\"瀹氶噾瀹℃牳缁撴灉\" />\n    <userTask id=\"balancePayment\" name=\"涓婁紶灏炬鍑瘉\" flowable:assignee=\"${starterId}\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${declarationTaskListener}\" />\n      </extensionElements>\n    </userTask>\n    <userTask id=\"balanceAudit\" name=\"璐㈠姟纭灏炬\" flowable:candidateGroups=\"FINANCE_MANAGER\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${declarationTaskListener}\" />\n        <flowable:taskListener event=\"complete\" delegateExpression=\"${declarationTaskListener}\" />\n      </extensionElements>\n    </userTask>\n    <exclusiveGateway id=\"gw3\" name=\"灏炬瀹℃牳缁撴灉\" />\n    <userTask id=\"pickupListUpload\" name=\"涓婁紶鎻愯揣鍗昞" flowable:assignee=\"${starterId}\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${declarationTaskListener}\" />\n      </extensionElements>\n    </userTask>\n    <userTask id=\"pickupListAudit\" name=\"璐㈠姟纭鎻愯揣鍗昞" flowable:candidateGroups=\"FINANCE_MANAGER\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${declarationTaskListener}\" />\n        <flowable:taskListener event=\"complete\" delegateExpression=\"${declarationTaskListener}\" />\n      </extensionElements>\n    </userTask>\n    <exclusiveGateway id=\"gw4\" name=\"鎻愯揣鍗曞鏍哥粨鏋淺" />\n    <userTask id=\"financeUploadTask\" name=\"璐㈠姟琛ュ厖鍙戠エ涓婁紶\" flowable:candidateGroups=\"FINANCE_MANAGER\">\n      <documentation>鍖呭惈锛氳揣浠ｅ彂绁ㄣ€佹捣鍏充唬鐞嗗彂绁ㄣ€佹捣鍏冲洖鎵с€侀€€绋庣偣璁剧疆</documentation>\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${declarationTaskListener}\" />\n      </extensionElements>\n    </userTask>\n    <endEvent id=\"endEvent1\" name=\"娴佺▼瀹屾垚\">\n      <extensionElements>\n        <flowable:executionListener event=\"end\" delegateExpression=\"${declarationTaskListener}\" />\n      </extensionElements>\n    </endEvent>\n    <sequenceFlow id=\"f1\" sourceRef=\"startEvent1\" targetRef=\"genContractTaskSmall\" />\n    <sequenceFlow id=\"f2\" sourceRef=\"deptAudit\" targetRef=\"gw1\" />\n    <sequenceFlow id=\"f3_pass\" name=\"閫氳繃\" sourceRef=\"gw1\" targetRef=\"genContractTask\">\n      <conditionExpression xsi:type=\"tFormalExpression\">${approved == true}</conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"f3_reject\" name=\"椹冲洖\" sourceRef=\"gw1\" targetRef=\"rejectHandler\">\n      <conditionExpression xsi:type=\"tFormalExpression\">${approved == false}</conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"f_reject_back\" sourceRef=\"rejectHandler\" targetRef=\"deptAudit\" />\n    <sequenceFlow id=\"f4\" sourceRef=\"genContractTask\" targetRef=\"depositPayment\" />\n    <sequenceFlow id=\"f_deposit_to_audit\" sourceRef=\"depositPayment\" targetRef=\"depositAudit\" />\n    <sequenceFlow id=\"f_deposit_audit_result\" sourceRef=\"depositAudit\" targetRef=\"gw2\" />\n    <sequenceFlow id=\"f_deposit_pass\" name=\"閫氳繃\" sourceRef=\"gw2\" targetRef=\"balancePayment\">\n      <conditionExpression xsi:type=\"tFormalExpression\">${approved == true}</conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"f_deposit_reject\" name=\"椹冲洖\" sourceRef=\"gw2\" targetRef=\"depositPayment\">\n      <conditionExpression xsi:type=\"tFormalExpression\">${approved == false}</conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"f_balance_to_audit\" sourceRef=\"balancePayment\" targetRef=\"balanceAudit\" />\n    <sequenceFlow id=\"f_balance_audit_result\" sourceRef=\"balanceAudit\" targetRef=\"gw3\" />\n    <sequenceFlow id=\"f_balance_pass\" name=\"閫氳繃\" sourceRef=\"gw3\" targetRef=\"pickupListUpload\">\n      <conditionExpression xsi:type=\"tFormalExpression\">${approved == true}</conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"f_balance_reject\" name=\"椹冲洖\" sourceRef=\"gw3\" targetRef=\"balancePayment\">\n      <conditionExpression xsi:type=\"tFormalExpression\">${approved == false}</conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"f_pickup_to_audit\" sourceRef=\"pickupListUpload\" targetRef=\"pickupListAudit\" />\n    <sequenceFlow id=\"f_pickup_audit_result\" sourceRef=\"pickupListAudit\" targetRef=\"gw4\" />\n    <sequenceFlow id=\"f_pickup_pass\" name=\"閫氳繃\" sourceRef=\"gw4\" targetRef=\"financeUploadTask\">\n      <conditionExpression xsi:type=\"tFormalExpression\">${approved == true}</conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"f_pickup_reject\" name=\"椹冲洖\" sourceRef=\"gw4\" targetRef=\"pickupListUpload\">\n      <conditionExpression xsi:type=\"tFormalExpression\">${approved == false}</conditionExpression>\n    </sequenceFlow>\n    <sequenceFlow id=\"f_finance_to_end\" sourceRef=\"financeUploadTask\" targetRef=\"endEvent1\" />\n    <serviceTask id=\"genContractTaskSmall\" name=\"鑷姩鐢熸垚棰勫綍鍏" flowable:delegateExpression=\"${declarationServiceTask}\">\n      <incoming>f1</incoming>\n      <outgoing>Flow_1fpr6j2</outgoing>\n    </serviceTask>\n    <sequenceFlow id=\"Flow_1fpr6j2\" sourceRef=\"genContractTaskSmall\" targetRef=\"deptAudit\" />\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_declarationProcess\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_declarationProcess\" bpmnElement=\"declarationProcess\">\n      <bpmndi:BPMNShape id=\"BPMNShape_deptAudit\" bpmnElement=\"deptAudit\">\n        <omgdc:Bounds x=\"120\" y=\"180\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_gw1\" bpmnElement=\"gw1\" isMarkerVisible=\"true\">\n        <omgdc:Bounds x=\"260\" y=\"200\" width=\"40\" height=\"40\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_rejectHandler\" bpmnElement=\"rejectHandler\">\n        <omgdc:Bounds x=\"120\" y=\"60\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_genContractTask\" bpmnElement=\"genContractTask\">\n        <omgdc:Bounds x=\"340\" y=\"180\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_depositPayment\" bpmnElement=\"depositPayment\">\n        <omgdc:Bounds x=\"480\" y=\"180\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_depositAudit\" bpmnElement=\"depositAudit\">\n        <omgdc:Bounds x=\"620\" y=\"180\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_gw2\" bpmnElement=\"gw2\" isMarkerVisible=\"true\">\n        <omgdc:Bounds x=\"760\" y=\"200\" width=\"40\" height=\"40\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_balancePayment\" bpmnElement=\"balancePayment\">\n        <omgdc:Bounds x=\"840\" y=\"180\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_balanceAudit\" bpmnElement=\"balanceAudit\">\n        <omgdc:Bounds x=\"980\" y=\"180\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_gw3\" bpmnElement=\"gw3\" isMarkerVisible=\"true\">\n        <omgdc:Bounds x=\"1120\" y=\"200\" width=\"40\" height=\"40\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_pickupListUpload\" bpmnElement=\"pickupListUpload\">\n        <omgdc:Bounds x=\"1200\" y=\"180\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_pickupListAudit\" bpmnElement=\"pickupListAudit\">\n        <omgdc:Bounds x=\"1340\" y=\"180\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_gw4\" bpmnElement=\"gw4\" isMarkerVisible=\"true\">\n        <omgdc:Bounds x=\"1480\" y=\"200\" width=\"40\" height=\"40\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_financeUploadTask\" bpmnElement=\"financeUploadTask\">\n        <omgdc:Bounds x=\"1560\" y=\"180\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_endEvent1\" bpmnElement=\"endEvent1\">\n        <omgdc:Bounds x=\"1700\" y=\"206\" width=\"28\" height=\"28\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_startEvent1\" bpmnElement=\"startEvent1\">\n        <omgdc:Bounds x=\"-165\" y=\"200\" width=\"30\" height=\"30\" />\n        <bpmndi:BPMNLabel>\n          <omgdc:Bounds x=\"-172\" y=\"230\" width=\"45\" height=\"14\" />\n        </bpmndi:BPMNLabel>\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_1i0v91y\" bpmnElement=\"genContractTaskSmall\">\n        <omgdc:Bounds x=\"-50\" y=\"175\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f1\" bpmnElement=\"f1\">\n        <omgdi:waypoint x=\"-135\" y=\"215\" />\n        <omgdi:waypoint x=\"-50\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f2\" bpmnElement=\"f2\">\n        <omgdi:waypoint x=\"220\" y=\"215\" />\n        <omgdi:waypoint x=\"260\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f3_pass\" bpmnElement=\"f3_pass\">\n        <omgdi:waypoint x=\"300\" y=\"215\" />\n        <omgdi:waypoint x=\"340\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f3_reject\" bpmnElement=\"f3_reject\">\n        <omgdi:waypoint x=\"280\" y=\"200\" />\n        <omgdi:waypoint x=\"280\" y=\"100\" />\n        <omgdi:waypoint x=\"220\" y=\"100\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_reject_back\" bpmnElement=\"f_reject_back\">\n        <omgdi:waypoint x=\"120\" y=\"100\" />\n        <omgdi:waypoint x=\"100\" y=\"100\" />\n        <omgdi:waypoint x=\"100\" y=\"215\" />\n        <omgdi:waypoint x=\"120\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f4\" bpmnElement=\"f4\">\n        <omgdi:waypoint x=\"440\" y=\"215\" />\n        <omgdi:waypoint x=\"480\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_deposit_to_audit\" bpmnElement=\"f_deposit_to_audit\">\n        <omgdi:waypoint x=\"580\" y=\"215\" />\n        <omgdi:waypoint x=\"620\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_deposit_audit_result\" bpmnElement=\"f_deposit_audit_result\">\n        <omgdi:waypoint x=\"720\" y=\"215\" />\n        <omgdi:waypoint x=\"760\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_deposit_pass\" bpmnElement=\"f_deposit_pass\">\n        <omgdi:waypoint x=\"800\" y=\"215\" />\n        <omgdi:waypoint x=\"840\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_deposit_reject\" bpmnElement=\"f_deposit_reject\">\n        <omgdi:waypoint x=\"780\" y=\"200\" />\n        <omgdi:waypoint x=\"780\" y=\"150\" />\n        <omgdi:waypoint x=\"530\" y=\"150\" />\n        <omgdi:waypoint x=\"530\" y=\"180\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_balance_to_audit\" bpmnElement=\"f_balance_to_audit\">\n        <omgdi:waypoint x=\"940\" y=\"215\" />\n        <omgdi:waypoint x=\"980\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_balance_audit_result\" bpmnElement=\"f_balance_audit_result\">\n        <omgdi:waypoint x=\"1080\" y=\"215\" />\n        <omgdi:waypoint x=\"1120\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_balance_pass\" bpmnElement=\"f_balance_pass\">\n        <omgdi:waypoint x=\"1160\" y=\"215\" />\n        <omgdi:waypoint x=\"1200\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_balance_reject\" bpmnElement=\"f_balance_reject\">\n        <omgdi:waypoint x=\"1140\" y=\"200\" />\n        <omgdi:waypoint x=\"1140\" y=\"150\" />\n        <omgdi:waypoint x=\"890\" y=\"150\" />\n        <omgdi:waypoint x=\"890\" y=\"180\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_pickup_to_audit\" bpmnElement=\"f_pickup_to_audit\">\n        <omgdi:waypoint x=\"1300\" y=\"215\" />\n        <omgdi:waypoint x=\"1340\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_pickup_audit_result\" bpmnElement=\"f_pickup_audit_result\">\n        <omgdi:waypoint x=\"1440\" y=\"215\" />\n        <omgdi:waypoint x=\"1480\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_pickup_pass\" bpmnElement=\"f_pickup_pass\">\n        <omgdi:waypoint x=\"1520\" y=\"215\" />\n        <omgdi:waypoint x=\"1560\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_pickup_reject\" bpmnElement=\"f_pickup_reject\">\n        <omgdi:waypoint x=\"1500\" y=\"230\" />\n        <omgdi:waypoint x=\"1500\" y=\"270\" />\n        <omgdi:waypoint x=\"1250\" y=\"270\" />\n        <omgdi:waypoint x=\"1250\" y=\"250\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_f_finance_to_end\" bpmnElement=\"f_finance_to_end\">\n        <omgdi:waypoint x=\"1660\" y=\"215\" />\n        <omgdi:waypoint x=\"1700\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"Flow_1fpr6j2_di\" bpmnElement=\"Flow_1fpr6j2\">\n        <omgdi:waypoint x=\"50\" y=\"215\" />\n        <omgdi:waypoint x=\"120\" y=\"215\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>\n',8,'declarationProcess:8:112504','','IT',1,'2026-03-18 13:40:03','2026-03-18 13:40:03',1,NULL),(8,NULL,'tax_refund_process','绋庡姟閫€璐瑰鎵规祦绋?,'<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"http://www.flowable.org/processdef\">\n  <process id=\"tax_refund_process\" name=\"绋庡姟閫€璐瑰鎵规祦绋媆" isExecutable=\"true\">\n    <startEvent id=\"startEvent\" name=\"寮€濮媆" />\n    <userTask id=\"departmentSubmit\" name=\"閮ㄩ棬鎻愪氦淇℃伅\" flowable:assignee=\"${initiator}\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${taxRefundExecutionListener}\" />\n      </extensionElements>\n    </userTask>\n    <userTask id=\"financeReview\" name=\"璐㈠姟鍒濆\" flowable:candidateGroups=\"finance\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${taxRefundExecutionListener}\" />\n      </extensionElements>\n    </userTask>\n    <serviceTask id=\"generateDocument\" name=\"鐢熸垚璐㈠姟鏂囦欢\" flowable:delegateExpression=\"${taxRefundServiceTask}\" />\n    <userTask id=\"returnDocument\" name=\"鏂囦欢杩斿洖鍙戣捣浜篭" flowable:assignee=\"${initiator}\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${taxRefundExecutionListener}\" />\n      </extensionElements>\n    </userTask>\n    <userTask id=\"invoiceSubmit\" name=\"鎻愪氦鍙戠エ閫€绋庝俊鎭痋" flowable:assignee=\"${initiator}\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${taxRefundExecutionListener}\" />\n      </extensionElements>\n    </userTask>\n    <userTask id=\"financeFinalReview\" name=\"璐㈠姟澶嶅\" flowable:candidateGroups=\"finance\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${taxRefundExecutionListener}\" />\n      </extensionElements>\n    </userTask>\n    <serviceTask id=\"archiveDocument\" name=\"鏂囦欢褰掓。\" flowable:delegateExpression=\"${taxRefundServiceTask}\" />\n    <endEvent id=\"endEvent\" name=\"缁撴潫\" />\n    <sequenceFlow id=\"flow1\" sourceRef=\"startEvent\" targetRef=\"departmentSubmit\" />\n    <sequenceFlow id=\"flow2\" sourceRef=\"departmentSubmit\" targetRef=\"financeReview\" />\n    <sequenceFlow id=\"flow3\" sourceRef=\"financeReview\" targetRef=\"generateDocument\" />\n    <sequenceFlow id=\"flow4\" sourceRef=\"generateDocument\" targetRef=\"returnDocument\" />\n    <sequenceFlow id=\"flow5\" sourceRef=\"returnDocument\" targetRef=\"invoiceSubmit\" />\n    <sequenceFlow id=\"flow6\" sourceRef=\"invoiceSubmit\" targetRef=\"financeFinalReview\" />\n    <sequenceFlow id=\"flow7\" sourceRef=\"financeFinalReview\" targetRef=\"archiveDocument\" />\n    <sequenceFlow id=\"flow8\" sourceRef=\"archiveDocument\" targetRef=\"endEvent\" />\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_tax_refund_process\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_tax_refund_process\" bpmnElement=\"tax_refund_process\">\n      <bpmndi:BPMNShape id=\"BPMNShape_startEvent\" bpmnElement=\"startEvent\">\n        <omgdc:Bounds x=\"100\" y=\"160\" width=\"30\" height=\"30\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_departmentSubmit\" bpmnElement=\"departmentSubmit\">\n        <omgdc:Bounds x=\"180\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_financeReview\" bpmnElement=\"financeReview\">\n        <omgdc:Bounds x=\"330\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_generateDocument\" bpmnElement=\"generateDocument\">\n        <omgdc:Bounds x=\"480\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_returnDocument\" bpmnElement=\"returnDocument\">\n        <omgdc:Bounds x=\"630\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_invoiceSubmit\" bpmnElement=\"invoiceSubmit\">\n        <omgdc:Bounds x=\"780\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_financeFinalReview\" bpmnElement=\"financeFinalReview\">\n        <omgdc:Bounds x=\"930\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_archiveDocument\" bpmnElement=\"archiveDocument\">\n        <omgdc:Bounds x=\"1080\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_endEvent\" bpmnElement=\"endEvent\">\n        <omgdc:Bounds x=\"1230\" y=\"161\" width=\"28\" height=\"28\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow1\" bpmnElement=\"flow1\">\n        <omgdi:waypoint x=\"130\" y=\"175\" />\n        <omgdi:waypoint x=\"180\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow2\" bpmnElement=\"flow2\">\n        <omgdi:waypoint x=\"280\" y=\"175\" />\n        <omgdi:waypoint x=\"330\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow3\" bpmnElement=\"flow3\">\n        <omgdi:waypoint x=\"430\" y=\"175\" />\n        <omgdi:waypoint x=\"480\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow4\" bpmnElement=\"flow4\">\n        <omgdi:waypoint x=\"580\" y=\"175\" />\n        <omgdi:waypoint x=\"630\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow5\" bpmnElement=\"flow5\">\n        <omgdi:waypoint x=\"730\" y=\"175\" />\n        <omgdi:waypoint x=\"780\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow6\" bpmnElement=\"flow6\">\n        <omgdi:waypoint x=\"880\" y=\"175\" />\n        <omgdi:waypoint x=\"930\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow7\" bpmnElement=\"flow7\">\n        <omgdi:waypoint x=\"1030\" y=\"175\" />\n        <omgdi:waypoint x=\"1080\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow8\" bpmnElement=\"flow8\">\n        <omgdi:waypoint x=\"1180\" y=\"175\" />\n        <omgdi:waypoint x=\"1230\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>\n',4,'tax_refund_process:4:45004','','IT',1,'2026-03-18 18:02:17','2026-03-18 18:02:17',1,NULL),(9,NULL,'tax_refund_process_simple','绋庡姟閫€璐瑰鎵规祦绋?绠€鍖栫増)','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"http://www.flowable.org/processdef\">\n  <process id=\"tax_refund_process_simple\" name=\"绋庡姟閫€璐瑰鎵规祦绋?绠€鍖栫増)\" isExecutable=\"true\">\n    <startEvent id=\"startEvent\" name=\"寮€濮媆" />\n    <userTask id=\"departmentSubmit\" name=\"閮ㄩ棬鎻愪氦淇℃伅\" flowable:assignee=\"${initiator}\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${taxRefundExecutionListener}\" />\n      </extensionElements>\n    </userTask>\n    <userTask id=\"financeReview\" name=\"璐㈠姟鍒濆\" flowable:candidateGroups=\"finance\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${taxRefundExecutionListener}\" />\n      </extensionElements>\n    </userTask>\n    <serviceTask id=\"generateDocument\" name=\"鐢熸垚璐㈠姟鏂囦欢\" flowable:delegateExpression=\"${taxRefundServiceTask}\" />\n    <userTask id=\"financeFinalReview\" name=\"璐㈠姟澶嶅\" flowable:candidateGroups=\"finance\">\n      <extensionElements>\n        <flowable:taskListener event=\"create\" delegateExpression=\"${taxRefundExecutionListener}\" />\n      </extensionElements>\n    </userTask>\n    <serviceTask id=\"archiveDocument\" name=\"鏂囦欢褰掓。\" flowable:delegateExpression=\"${taxRefundServiceTask}\" />\n    <endEvent id=\"endEvent\" name=\"缁撴潫\" />\n    <sequenceFlow id=\"flow1\" sourceRef=\"startEvent\" targetRef=\"departmentSubmit\" />\n    <sequenceFlow id=\"flow2\" sourceRef=\"departmentSubmit\" targetRef=\"financeReview\" />\n    <sequenceFlow id=\"flow3\" sourceRef=\"financeReview\" targetRef=\"generateDocument\" />\n    <sequenceFlow id=\"flow4\" sourceRef=\"generateDocument\" targetRef=\"financeFinalReview\" />\n    <sequenceFlow id=\"flow5\" sourceRef=\"financeFinalReview\" targetRef=\"archiveDocument\" />\n    <sequenceFlow id=\"flow6\" sourceRef=\"archiveDocument\" targetRef=\"endEvent\" />\n  </process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_tax_refund_process_simple\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_tax_refund_process_simple\" bpmnElement=\"tax_refund_process_simple\">\n      <bpmndi:BPMNShape id=\"BPMNShape_startEvent\" bpmnElement=\"startEvent\">\n        <omgdc:Bounds x=\"100\" y=\"160\" width=\"30\" height=\"30\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_departmentSubmit\" bpmnElement=\"departmentSubmit\">\n        <omgdc:Bounds x=\"180\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_financeReview\" bpmnElement=\"financeReview\">\n        <omgdc:Bounds x=\"330\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_generateDocument\" bpmnElement=\"generateDocument\">\n        <omgdc:Bounds x=\"480\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_financeFinalReview\" bpmnElement=\"financeFinalReview\">\n        <omgdc:Bounds x=\"630\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_archiveDocument\" bpmnElement=\"archiveDocument\">\n        <omgdc:Bounds x=\"780\" y=\"135\" width=\"100\" height=\"80\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNShape id=\"BPMNShape_endEvent\" bpmnElement=\"endEvent\">\n        <omgdc:Bounds x=\"930\" y=\"161\" width=\"28\" height=\"28\" />\n      </bpmndi:BPMNShape>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow1\" bpmnElement=\"flow1\">\n        <omgdi:waypoint x=\"130\" y=\"175\" />\n        <omgdi:waypoint x=\"180\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow2\" bpmnElement=\"flow2\">\n        <omgdi:waypoint x=\"280\" y=\"175\" />\n        <omgdi:waypoint x=\"330\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow3\" bpmnElement=\"flow3\">\n        <omgdi:waypoint x=\"430\" y=\"175\" />\n        <omgdi:waypoint x=\"480\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow4\" bpmnElement=\"flow4\">\n        <omgdi:waypoint x=\"580\" y=\"175\" />\n        <omgdi:waypoint x=\"630\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow5\" bpmnElement=\"flow5\">\n        <omgdi:waypoint x=\"730\" y=\"175\" />\n        <omgdi:waypoint x=\"780\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n      <bpmndi:BPMNEdge id=\"BPMNEdge_flow6\" bpmnElement=\"flow6\">\n        <omgdi:waypoint x=\"880\" y=\"175\" />\n        <omgdi:waypoint x=\"930\" y=\"175\" />\n      </bpmndi:BPMNEdge>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</definitions>\n',1,'tax_refund_process_simple:1:60034','','IT',1,'2026-03-19 13:15:36','2026-03-19 13:15:36',1,NULL);
/*!40000 ALTER TABLE `process_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_declaration`
--

DROP TABLE IF EXISTS `product_declaration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_declaration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `serial_number` int NOT NULL COMMENT '搴忓彿',
  `hs_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HS缂栫爜',
  `english_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鑻辨枃鍚嶇О',
  `chinese_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '涓枃鍚嶇О',
  `declaration_elements` json DEFAULT NULL COMMENT '鐢虫姤瑕佺礌JSON鏁版嵁',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '鐘舵€?0-鑽夌 1-宸叉彁浜?2-宸插鏍?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囧織 0-姝ｅ父 1-鍒犻櫎',
  PRIMARY KEY (`id`),
  KEY `idx_hs_code` (`hs_code`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_product_decl_hs_status` (`hs_code`,`status`),
  KEY `idx_product_decl_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鍟嗗搧鐢虫姤鏁版嵁琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_declaration`
--

LOCK TABLES `product_declaration` WRITE;
/*!40000 ALTER TABLE `product_declaration` DISABLE KEYS */;
/*!40000 ALTER TABLE `product_declaration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_type_config`
--

DROP TABLE IF EXISTS `product_type_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_type_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `hs_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HS缂栫爜',
  `english_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鑻辨枃鍚嶇О',
  `chinese_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '涓枃鍚嶇О',
  `elements_config` json DEFAULT NULL COMMENT '鐢虫姤瑕佺礌閰嶇疆JSON',
  `unit_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '璁￠噺鍗曚綅绫诲瀷',
  `unit_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '璁￠噺鍗曚綅浠ｇ爜',
  `unit_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '璁￠噺鍗曚綅鍚嶇О',
  `sort` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-绂佺敤 1-鍚敤',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囧織 0-姝ｅ父 1-鍒犻櫎',
  PRIMARY KEY (`id`),
  KEY `idx_english_name` (`english_name`),
  KEY `idx_status` (`status`),
  KEY `uk_hs_code` (`hs_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HS鍟嗗搧绫诲瀷閰嶇疆琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_type_config`
--

LOCK TABLES `product_type_config` WRITE;
/*!40000 ALTER TABLE `product_type_config` DISABLE KEYS */;
INSERT INTO `product_type_config` VALUES (1,'8523591000','RFID STICKER','RFID鏍囩','[{\"key\": \"0\", \"type\": \"text\", \"label\": \"鐢ㄩ€擻", \"value\": \"鎻愪緵鍞竴鏁版嵁\", \"options\": null, \"editable\": false, \"required\": true, \"placeholder\": \"鎻愪緵鍞竴鏁版嵁\", \"defaultValue\": \"鎻愪緵鍞竴鏁版嵁\"}, {\"key\": \"1\", \"type\": \"select\", \"label\": \"鏄惁褰曞埗\", \"value\": \"鍚", \"options\": [\"鏄痋", \"鍚"], \"editable\": false, \"required\": true, \"placeholder\": \"璇烽€夋嫨\", \"defaultValue\": \"鍚"}, {\"key\": \"2\", \"type\": \"text\", \"label\": \"鍝佺墝\", \"value\": \"\", \"options\": null, \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗孿", \"defaultValue\": \"鏃燶"}, {\"key\": \"3\", \"type\": \"text\", \"label\": \"鍨嬪彿\", \"value\": \"\", \"options\": null, \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ瀷鍙穃", \"defaultValue\": \"\"}, {\"key\": \"4\", \"type\": \"text\", \"label\": \"鍝佺墝绫诲瀷\", \"value\": \"\", \"options\": null, \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗岀被鍨媆", \"defaultValue\": \"\"}, {\"key\": \"5\", \"type\": \"select\", \"label\": \"鍑哄彛浜儬鎯呭喌\", \"value\": \"涓嶇‘瀹歕", \"options\": [\"纭畾\", \"涓嶇‘瀹歕", \"涓嶉€傜敤\"], \"editable\": false, \"required\": true, \"placeholder\": \"璇烽€夋嫨\", \"defaultValue\": \"涓嶇‘瀹歕"}]',NULL,NULL,NULL,0,1,'2026-03-14 03:49:13','2026-03-16 05:33:13',NULL,1,0),(2,'9031809090','Handheld reader','RFID鎵弿鍣?,'[{\"key\": \"0\", \"type\": \"text\", \"label\": \"鐢ㄩ€擻", \"value\": \"鎵弿鍞竴鐮乗", \"editable\": false, \"required\": true, \"placeholder\": \"鎵弿鍞竴鐮乗"}, {\"key\": \"1\", \"type\": \"text\", \"label\": \"鍘熺悊\", \"value\": \"鐢靛瓙灏勯鎵弿\", \"editable\": false, \"required\": true, \"placeholder\": \"鐢靛瓙灏勯鎵弿\"}, {\"key\": \"2\", \"type\": \"text\", \"label\": \"鍔熻兘\", \"value\": \"鑾峰彇鏃犳簮灏勯鏍囩鏁版嵁\", \"editable\": false, \"required\": true, \"placeholder\": \"鑾峰彇鏃犳簮灏勯鏍囩鏁版嵁\"}, {\"key\": \"3\", \"type\": \"text\", \"label\": \"鍝佺墝\", \"value\": \"\", \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗孿"}, {\"key\": \"4\", \"type\": \"text\", \"label\": \"鍨嬪彿\", \"value\": \"\", \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ瀷鍙穃"}, {\"key\": \"5\", \"type\": \"text\", \"label\": \"鍝佺墝绫诲瀷\", \"value\": \"\", \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗岀被鍨媆"}, {\"key\": \"6\", \"type\": \"select\", \"label\": \"鍑哄彛浜儬鎯呭喌\", \"value\": \"涓嶇‘瀹歕", \"options\": [\"纭畾\", \"涓嶇‘瀹歕", \"涓嶉€傜敤\"], \"editable\": false, \"required\": true, \"placeholder\": \"璇烽€夋嫨\"}]',NULL,NULL,NULL,2,1,'2026-03-14 03:49:13','2026-03-16 05:37:38',NULL,NULL,0),(3,'3926209000','PLASTIC SEAL','鍚婄矑','[{\"key\": \"0\", \"type\": \"text\", \"label\": \"鐢ㄩ€擻", \"value\": \"鐢ㄤ簬琛ｆ湇涓婄殑鍚婄矑\", \"editable\": false, \"required\": true, \"placeholder\": \"鐢ㄤ簬琛ｆ湇涓婄殑鍚婄矑\"}, {\"key\": \"1\", \"type\": \"text\", \"label\": \"鏉愯川\", \"value\": \"濉戞枡\", \"editable\": false, \"required\": true, \"placeholder\": \"濉戞枡\"}, {\"key\": \"2\", \"type\": \"text\", \"label\": \"鍝佺墝\", \"value\": \"\", \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗孿"}, {\"key\": \"3\", \"type\": \"text\", \"label\": \"鍨嬪彿\", \"value\": \"\", \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ瀷鍙穃"}, {\"key\": \"4\", \"type\": \"text\", \"label\": \"鍝佺墝绫诲瀷\", \"value\": \"\", \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗岀被鍨媆"}, {\"key\": \"5\", \"type\": \"select\", \"label\": \"鍑哄彛浜儬鎯呭喌\", \"value\": \"涓嶇‘瀹歕", \"options\": [\"纭畾\", \"涓嶇‘瀹歕", \"涓嶉€傜敤\"], \"editable\": false, \"required\": true, \"placeholder\": \"璇烽€夋嫨\"}]',NULL,NULL,NULL,3,1,'2026-03-14 03:49:13','2026-03-16 05:37:38',NULL,NULL,0),(4,'4819400000','PAPER BAG','绾歌','[{\"key\": \"0\", \"type\": \"text\", \"label\": \"鐢ㄩ€擻", \"value\": \"鐢ㄤ簬鍖呰琛ｆ湇\", \"editable\": false, \"required\": true, \"placeholder\": \"鐢ㄤ簬鍖呰琛ｆ湇\"}, {\"key\": \"1\", \"type\": \"text\", \"label\": \"鏉愯川\", \"value\": \"鐗涚毊绾竆", \"editable\": false, \"required\": true, \"placeholder\": \"鐗涚毊绾竆"}, {\"key\": \"2\", \"type\": \"text\", \"label\": \"鍝佺墝\", \"value\": \"\", \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗孿"}, {\"key\": \"3\", \"type\": \"text\", \"label\": \"瑙勬牸\", \"value\": \"\", \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ヨ鏍糪"}, {\"key\": \"4\", \"type\": \"text\", \"label\": \"鍝佺墝绫诲瀷\", \"value\": \"\", \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗岀被鍨媆"}, {\"key\": \"5\", \"type\": \"select\", \"label\": \"鍑哄彛浜儬鎯呭喌\", \"value\": \"涓嶇‘瀹歕", \"options\": [\"纭畾\", \"涓嶇‘瀹歕", \"涓嶉€傜敤\"], \"editable\": false, \"required\": true, \"placeholder\": \"璇烽€夋嫨\"}]',NULL,NULL,NULL,4,1,'2026-03-14 03:49:13','2026-03-16 05:37:38',NULL,NULL,0),(5,'5807100000','WOVEN LABEL','缁囨爣','[{\"key\": \"0\", \"type\": \"text\", \"label\": \"缁囬€犳柟娉昞", \"value\": \"鏈虹粐\", \"options\": null, \"editable\": false, \"required\": true, \"placeholder\": \"鏈虹粐\", \"defaultValue\": \"鏈虹粐\"}, {\"key\": \"1\", \"type\": \"text\", \"label\": \"鎴愬垎鍚噺\", \"value\": \"100%娑ょ憾\", \"options\": null, \"editable\": false, \"required\": true, \"placeholder\": \"100%娑ょ憾\", \"defaultValue\": \"100%娑ょ憾\"}, {\"key\": \"2\", \"type\": \"text\", \"label\": \"\", \"value\": \"闈炲埡缁", \"options\": null, \"editable\": false, \"required\": false, \"placeholder\": \"闈炲埡缁", \"defaultValue\": \"闈炲埡缁"}, {\"key\": \"3\", \"type\": \"text\", \"label\": \"鍝佺墝\", \"value\": \"\", \"options\": null, \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗孿", \"defaultValue\": \"\"}, {\"key\": \"4\", \"type\": \"text\", \"label\": \"鍝佺墝绫诲瀷\", \"value\": \"\", \"options\": null, \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗岀被鍨媆", \"defaultValue\": \"\"}, {\"key\": \"5\", \"type\": \"select\", \"label\": \"鍑哄彛浜儬鎯呭喌\", \"value\": \"涓嶇‘瀹歕", \"options\": [\"纭畾\", \"涓嶇‘瀹歕", \"涓嶉€傜敤\"], \"editable\": false, \"required\": true, \"placeholder\": \"璇烽€夋嫨\", \"defaultValue\": \"涓嶇‘瀹歕"}]',NULL,NULL,NULL,5,1,'2026-03-14 03:49:13','2026-03-16 05:42:07',NULL,1,0),(6,'4821100000','HANG TAG','鍚婄墝','[{\"key\": \"0\", \"type\": \"text\", \"label\": \"鏉愯川\", \"value\": \"绾歌川\", \"options\": null, \"editable\": false, \"required\": true, \"placeholder\": \"绾歌川\", \"defaultValue\": \"绾歌川\"}, {\"key\": \"1\", \"type\": \"text\", \"label\": \"鍔犲伐绋嬪害\", \"value\": \"鍗板埗\", \"options\": null, \"editable\": false, \"required\": true, \"placeholder\": \"鍗板埗\", \"defaultValue\": \"鍗板埗\"}, {\"key\": \"2\", \"type\": \"text\", \"label\": \"鍝佺墝\", \"value\": \"\", \"options\": null, \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗孿", \"defaultValue\": \"\"}, {\"key\": \"3\", \"type\": \"text\", \"label\": \"鍝佺墝绫诲瀷\", \"value\": \"\", \"options\": null, \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗岀被鍨媆", \"defaultValue\": \"\"}, {\"key\": \"4\", \"type\": \"select\", \"label\": \"鍑哄彛浜儬鎯呭喌\", \"value\": \"涓嶇‘瀹歕", \"options\": [\"纭畾\", \"涓嶇‘瀹歕", \"涓嶉€傜敤\"], \"editable\": false, \"required\": true, \"placeholder\": \"璇烽€夋嫨\", \"defaultValue\": \"涓嶇‘瀹歕"}]',NULL,NULL,NULL,6,1,'2026-03-14 03:49:13','2026-03-16 05:43:12',NULL,1,0),(7,'3923210000','POLYBAG','濉戞枡琚?,'[{\"key\": \"0\", \"type\": \"text\", \"label\": \"鐢ㄩ€擻", \"value\": \"鐢ㄤ簬琛ｆ湇鍖呰\", \"options\": null, \"editable\": false, \"required\": true, \"placeholder\": \"鐢ㄤ簬琛ｆ湇鍖呰\", \"defaultValue\": \"鐢ㄤ簬琛ｆ湇鍖呰\"}, {\"key\": \"1\", \"type\": \"text\", \"label\": \"鏉愯川\", \"value\": \"濉戞枡\", \"options\": null, \"editable\": false, \"required\": true, \"placeholder\": \"濉戞枡\", \"defaultValue\": \"濉戞枡\"}, {\"key\": \"2\", \"type\": \"text\", \"label\": \"鍝佺墝\", \"value\": \"\", \"options\": null, \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗孿", \"defaultValue\": \"\"}, {\"key\": \"3\", \"type\": \"text\", \"label\": \"鍨嬪彿\", \"value\": \"\", \"options\": null, \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ瀷鍙穃", \"defaultValue\": \"\"}, {\"key\": \"4\", \"type\": \"text\", \"label\": \"鍝佺墝绫诲瀷\", \"value\": \"\", \"options\": null, \"editable\": true, \"required\": false, \"placeholder\": \"璇疯緭鍏ュ搧鐗岀被鍨媆", \"defaultValue\": \"\"}, {\"key\": \"5\", \"type\": \"select\", \"label\": \"鍑哄彛浜儬鎯呭喌\", \"value\": \"涓嶇‘瀹歕", \"options\": [\"纭畾\", \"涓嶇‘瀹歕", \"涓嶉€傜敤\"], \"editable\": false, \"required\": true, \"placeholder\": \"璇烽€夋嫨\", \"defaultValue\": \"涓嶇‘瀹歕"}]',NULL,NULL,NULL,7,1,'2026-03-14 03:49:13','2026-03-16 05:43:59',NULL,1,0);
/*!40000 ALTER TABLE `product_type_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '閰嶇疆閿?,
  `config_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '閰嶇疆鍚嶇О',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '閰嶇疆鍊?,
  `input_type` tinyint DEFAULT '1' COMMENT '閰嶇疆杈撳叆绫诲瀷 1-鏂囨湰妗?2-涓嬫媺妗?3-寮€鍏?4-鏁板瓧杈撳叆妗?,
  `select_options` json DEFAULT NULL COMMENT '涓嬫媺妗嗛€夐」JSON(褰搃nput_type=2鏃朵娇鐢?',
  `is_system_param` tinyint DEFAULT '0' COMMENT '鏄惁绯荤粺鍐呯疆鍙傛暟 0-鍚?1-鏄?,
  `config_type` tinyint NOT NULL DEFAULT '1' COMMENT '閰嶇疆绫诲瀷 1-绯荤粺閰嶇疆 2-UI閰嶇疆 3-涓氬姟閰嶇疆',
  `config_group` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '閰嶇疆鍒嗙粍',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '澶囨敞璇存槑',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-绂佺敤 1-鍚敤',
  `sort` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囧織 0-姝ｅ父 1-鍒犻櫎',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_group` (`config_group`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绯荤粺閰嶇疆琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

LOCK TABLES `sys_config` WRITE;
/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` VALUES (1,'system.name','绯荤粺鍚嶇О','娴峰叧鐢虫姤绯荤粺',1,NULL,1,1,'basic','绯荤粺鏄剧ず鍚嶇О',1,1,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(2,'system.version','绯荤粺鐗堟湰','1.0.0',1,NULL,1,1,'basic','绯荤粺鐗堟湰鍙?,1,2,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(3,'system.description','绯荤粺鎻忚堪','娴峰叧鐢虫姤绯荤粺',1,NULL,1,1,'basic','绯荤粺鍔熻兘鎻忚堪',1,3,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(4,'system.company','鍏徃鍚嶇О','瀹佹尝姊撶啝绉戞妧鏈夐檺鍏徃',1,NULL,1,1,'basic','绯荤粺鎵€灞炲叕鍙?,1,4,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(5,'system.copyright','鐗堟潈淇℃伅','漏 2026 瀹佹尝姊撶啝绉戞妧鏈夐檺鍏徃 鐗堟潈鎵€鏈?,1,NULL,1,1,'basic','绯荤粺鐗堟潈淇℃伅',1,5,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(6,'ui.logo','Logo鍥剧墖','/logo.png',1,NULL,1,2,'ui','绯荤粺Logo鍥剧墖URL',1,1,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(7,'ui.favicon','缃戠珯鍥炬爣','/favicon.ico',1,NULL,1,2,'ui','娴忚鍣ㄦ爣绛鹃〉鍥炬爣',1,2,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(8,'ui.theme','涓婚棰滆壊','#1890ff',2,'[{\"label\": \"榛樿钃漒", \"value\": \"#1890ff\"}, {\"label\": \"绉戞妧钃漒", \"value\": \"#001529\"}, {\"label\": \"娲诲姏姗橽", \"value\": \"#fa8c16\"}, {\"label\": \"娓呮柊缁縗", \"value\": \"#52c41a\"}]',1,2,'ui','绯荤粺涓婚鑹?,1,3,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(9,'ui.footer.text','搴曢儴鏂囧瓧','娴峰叧鐢虫姤绯荤粺 漏2026 瀹佹尝姊撶啝绉戞妧鏈夐檺鍏徃',1,NULL,1,2,'ui','椤甸潰搴曢儴鏄剧ず鏂囧瓧',1,4,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(10,'ui.footer.show','鏄剧ず搴曢儴','true',3,NULL,1,2,'ui','鏄惁鏄剧ず椤甸潰搴曢儴',1,5,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(11,'ui.sidebar.collapsed','渚ц竟鏍忔姌鍙?,'false',3,NULL,1,2,'ui','渚ц竟鏍忛粯璁ゆ槸鍚︽姌鍙?,1,6,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(12,'ui.language','绯荤粺璇█','zh-CN',2,'[{\"label\": \"绠€浣撲腑鏂嘰", \"value\": \"zh-CN\"}, {\"label\": \"English\", \"value\": \"en-US\"}]',0,2,'ui','绯荤粺鏄剧ず璇█',1,7,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(13,'business.tax-refund.enabled','绋庡姟閫€璐瑰姛鑳?,'true',3,NULL,0,3,'business','鏄惁鍚敤绋庡姟閫€璐瑰姛鑳?,1,1,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(14,'business.tax-refund.approval-level','瀹℃壒灞傜骇','3',2,'[{\"label\": \"1绾у鎵筡", \"value\": \"1\"}, {\"label\": \"2绾у鎵筡", \"value\": \"2\"}, {\"label\": \"3绾у鎵筡", \"value\": \"3\"}, {\"label\": \"4绾у鎵筡", \"value\": \"4\"}, {\"label\": \"5绾у鎵筡", \"value\": \"5\"}]',0,3,'business','绋庡姟閫€璐瑰鎵瑰眰绾ф暟',1,2,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(15,'business.file.upload.max-size','鏂囦欢涓婁紶澶у皬闄愬埗','10MB',1,NULL,0,3,'business','鏂囦欢涓婁紶鏈€澶уぇ灏?,1,3,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(16,'business.notification.email-enabled','閭欢閫氱煡','true',3,NULL,0,3,'business','鏄惁鍚敤閭欢閫氱煡',1,4,'2026-03-14 06:23:36','2026-03-14 06:23:36',NULL,NULL,0),(17,'business.default-department','榛樿閮ㄩ棬','tech',2,'[{\"label\": \"鎶€鏈儴\", \"value\": \"tech\"}, {\"label\": \"閿€鍞儴\", \"value\": \"sales\"}, {\"label\": \"璐㈠姟閮╘", \"value\": \"finance\"}, {\"label\": \"浜轰簨閮╘", \"value\": \"hr\"}]',0,3,'business','涓氬姟榛樿褰掑睘閮ㄩ棬',1,5,'2026-03-14 06:23:36','2026-03-14 14:34:15',NULL,NULL,1),(18,'Transport-details  ','杩愯緭鏂瑰紡','AIR FREIGHT',2,'[{\"label\": \"AIR FREIGHT\", \"value\": \"AIR FREIGHT\"}, {\"label\": \"TRUCK\", \"value\": \"TRUCK\"}]',0,3,'business','',1,0,'2026-03-14 14:33:59','2026-03-14 14:33:59',NULL,NULL,0);
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '鑿滃崟鍚嶇О',
  `menu_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鑿滃崟缂栫爜',
  `parent_id` bigint DEFAULT NULL COMMENT '鐖剁骇ID',
  `menu_type` tinyint NOT NULL DEFAULT '1' COMMENT '鑿滃崟绫诲瀷 1-鐩綍 2-鑿滃崟 3-鎸夐挳',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '璺敱鍦板潃',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '缁勪欢璺緞',
  `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鏉冮檺鏍囪瘑',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鍥炬爣',
  `sort` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `is_external` tinyint NOT NULL DEFAULT '0' COMMENT '鏄惁澶栭摼 0-鍚?1-鏄?,
  `is_cache` tinyint NOT NULL DEFAULT '0' COMMENT '鏄惁缂撳瓨 0-鍚?1-鏄?,
  `is_show` tinyint NOT NULL DEFAULT '1' COMMENT '鏄惁鏄剧ず 0-闅愯棌 1-鏄剧ず',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-绂佺敤 1-鍚敤',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑 0-鏈垹闄?1-宸插垹闄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_permission` (`permission`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6036 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='鑿滃崟鏉冮檺琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,'棣栭〉',NULL,0,1,'/','Layout',NULL,'HomeOutlined',1,0,0,0,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2,'棣栭〉',NULL,0,2,'/dashboard','@/views/dashboard/simple.vue',NULL,'HomeOutlined',0,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3,'涓汉涓績',NULL,0,1,'/profile','Layout',NULL,'UserOutlined',99,0,0,0,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4,'涓汉淇℃伅',NULL,3,2,'index','@/views/profile/index.vue',NULL,'UserOutlined',1,0,0,0,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(100,'绯荤粺绠＄悊',NULL,0,1,'/system','Layout',NULL,'SettingOutlined',2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(101,'鐢ㄦ埛绠＄悊',NULL,100,2,'user','@/views/system/user/index.vue','system:user:list','UserOutlined',1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(102,'瑙掕壊绠＄悊',NULL,100,2,'role','@/views/system/role/index.vue','system:role:list','TeamOutlined',2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(103,'缁勭粐绠＄悊',NULL,100,2,'org','@/views/system/org/index.vue','system:org:list','ApartmentOutlined',3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(104,'鑿滃崟绠＄悊',NULL,100,2,'menu','@/views/system/menu/index.vue','system:menu:list','MenuOutlined',4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(105,'閾惰璐︽埛',NULL,100,2,'bank-account','@/views/system/bank-account/index.vue','system:bank-account:list','BankOutlined',5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(106,'鍥藉淇℃伅',NULL,100,2,'country','@/views/system/country/index.vue','system:country:list','GlobalOutlined',6,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(107,'HS鍟嗗搧缁存姢',NULL,100,2,'product','@/views/system/product/index.vue','system:product:list','DatabaseOutlined',7,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(108,'API娴嬭瘯',NULL,100,2,'api-test','@/views/system/api-test/index.vue','system:api-test:list','ApiOutlined',8,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(109,'绯荤粺閰嶇疆',NULL,100,2,'config','@/views/system/config/index.vue','system:config:list','SettingOutlined',9,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(110,'杩愯緭鏂瑰紡',NULL,100,2,'transport-mode','@/views/system/transport-mode/index.vue','system:transport:list','CarOutlined',10,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(111,'璐у竵绠＄悊',NULL,100,2,'currency','@/views/system/currency/index.vue','system:currency:list','MoneyCollectOutlined',11,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(200,'鍑哄彛鐢虫姤',NULL,0,1,'/declaration','Layout',NULL,'FileProtectOutlined',3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(201,'鐢虫姤绠＄悊',NULL,200,2,'manage','@/views/declaration/manage/index.vue','business:declaration:list','ContainerOutlined',1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(202,'璐㈠姟鍗曡瘉',NULL,200,2,'finance','@/views/declaration/finance/index.vue','business:declaration:financeSupplement','PayCircleOutlined',2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(203,'鐢虫姤琛ㄥ崟',NULL,200,2,'form','@/views/declaration/form/index.vue','business:declaration:form','FileTextOutlined',3,0,0,0,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(204,'鏀粯鍑瘉',NULL,200,2,'payment','@/views/declaration/payment/index.vue','business:declaration:payment','AccountBookOutlined',4,0,0,0,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(205,'鐢虫姤缁熻',NULL,200,2,'statistics','@/views/declaration/statistics/index.vue','business:declaration:statistics','BarChartOutlined',5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(300,'绋庡姟閫€璐?,NULL,0,1,'/tax-refund','Layout',NULL,'DollarOutlined',4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(301,'閫€绋庣敵璇?,NULL,300,2,'apply','@/views/tax-refund/apply/index.vue','business:tax-refund:apply','PlusOutlined',1,0,0,0,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',0,0),(302,'鐢宠鍒楄〃',NULL,300,2,'list','@/views/tax-refund/list/index.vue','business:tax-refund:list','UnorderedListOutlined',2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(303,'鐢宠璇︽儏',NULL,300,2,'detail/:id','@/views/tax-refund/detail/index.vue','business:tax-refund:detail','FileSearchOutlined',3,0,0,0,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(400,'宸ヤ綔娴?,NULL,0,1,'/workflow','Layout',NULL,'BranchesOutlined',5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(401,'娴佺▼瀹氫箟',NULL,400,2,'definition','@/views/workflow/definition/index.vue','workflow:definition:list','FileDoneOutlined',1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(402,'娴佺▼璁捐',NULL,400,2,'modeler','@/views/workflow/modeler/index.vue','workflow:modeler:view','EditOutlined',2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(403,'娴佺▼鐩戞帶',NULL,400,2,'monitor','@/views/workflow/monitor/index.vue','workflow:monitor:view','FundViewOutlined',3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(404,'娴佺▼瀹炰緥',NULL,400,2,'instance','@/views/workflow/instance/index.vue','workflow:instance:list','ProfileOutlined',4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(405,'鎴戠殑浠诲姟',NULL,400,2,'task','@/views/workflow/task/index.vue','workflow:task:list','CheckCircleOutlined',5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(500,'鍚堝悓绠＄悊',NULL,0,1,'/contract','Layout',NULL,'FileTextOutlined',6,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(501,'妯℃澘绠＄悊',NULL,500,2,'template','@/views/contract/template/index.vue','business:contract:template','FileAddOutlined',1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(502,'鍚堝悓鍒楄〃',NULL,500,2,'generation','@/views/contract/generation/index.vue','business:contract:generation','HistoryOutlined',2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1011,'鐢ㄦ埛鏌ヨ',NULL,101,3,'','','user:query',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1012,'鐢ㄦ埛鏂板',NULL,101,3,'','','user:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1013,'鐢ㄦ埛缂栬緫',NULL,101,3,'','','user:update',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1014,'鐢ㄦ埛鍒犻櫎',NULL,101,3,'','','user:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1015,'鐢ㄦ埛鍒楄〃',NULL,101,3,'','','user:list',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1016,'瀵嗙爜閲嶇疆',NULL,101,3,'','','user:resetPwd',NULL,6,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1021,'瑙掕壊鏌ヨ',NULL,102,3,'','','role:query',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1022,'瑙掕壊鏂板',NULL,102,3,'','','role:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1023,'瑙掕壊缂栬緫',NULL,102,3,'','','role:update',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1024,'瑙掕壊鍒犻櫎',NULL,102,3,'','','role:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1025,'瑙掕壊鍒楄〃',NULL,102,3,'','','role:list',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1026,'鐢ㄦ埛瑙掕壊绠＄悊',NULL,102,3,'','','role:user',NULL,6,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1027,'瑙掕壊鍒嗛厤',NULL,102,3,'','','role:assign',NULL,7,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1028,'鑿滃崟鏉冮檺绠＄悊',NULL,102,3,'','','role:menu',NULL,8,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1031,'缁勭粐鏌ヨ',NULL,103,3,'','','org:query',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1032,'缁勭粐鏂板',NULL,103,3,'','','org:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1033,'缁勭粐缂栬緫',NULL,103,3,'','','org:update',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1034,'缁勭粐鍒犻櫎',NULL,103,3,'','','org:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1035,'缁勭粐鍒楄〃',NULL,103,3,'','','org:list',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1036,'缁勭粐鐢ㄦ埛绠＄悊',NULL,103,3,'','','org:user',NULL,6,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1041,'鑿滃崟鏌ヨ',NULL,104,3,'','','menu:query',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1042,'鑿滃崟鏂板',NULL,104,3,'','','menu:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1043,'鑿滃崟缂栬緫',NULL,104,3,'','','menu:update',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1044,'鑿滃崟鍒犻櫎',NULL,104,3,'','','menu:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1045,'鑿滃崟鍒楄〃',NULL,104,3,'','','menu:list',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1051,'鍒楄〃鏌ョ湅',NULL,105,3,'','','system:bank-account:list',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1052,'璐︽埛鏂板',NULL,105,3,'','','system:bank-account:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1053,'璐︽埛缂栬緫',NULL,105,3,'','','system:bank-account:update',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1054,'璐︽埛鍒犻櫎',NULL,105,3,'','','system:bank-account:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1055,'璐︽埛鏌ョ湅',NULL,105,3,'','','system:bank-account:view',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1061,'淇℃伅鍒楄〃',NULL,106,3,'','','system:country:list',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1062,'淇℃伅鏂板',NULL,106,3,'','','system:country:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1063,'淇℃伅缂栬緫',NULL,106,3,'','','system:country:edit',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1064,'淇℃伅鍒犻櫎',NULL,106,3,'','','system:country:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1065,'淇℃伅鏌ョ湅',NULL,106,3,'','','system:country:view',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1071,'鍟嗗搧鏌ヨ',NULL,107,3,'','','system:product:query',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1072,'鍟嗗搧鏂板',NULL,107,3,'','','system:product:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1073,'鍟嗗搧缂栬緫',NULL,107,3,'','','system:product:update',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1074,'鍟嗗搧鍒犻櫎',NULL,107,3,'','','system:product:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1075,'鍟嗗搧鍒楄〃',NULL,107,3,'','','system:product:list',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1091,'閰嶇疆鏌ヨ',NULL,109,3,'','','system:config:query',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1092,'閰嶇疆鏂板',NULL,109,3,'','','system:config:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1093,'閰嶇疆缂栬緫',NULL,109,3,'','','system:config:update',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1094,'閰嶇疆鍒犻櫎',NULL,109,3,'','','system:config:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1095,'閰嶇疆鍒楄〃',NULL,109,3,'','','system:config:list',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1101,'鍒楄〃鏌ョ湅',NULL,110,3,'','','system:transport:list',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1102,'璇︽儏鏌ョ湅',NULL,110,3,'','','system:transport:view',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1103,'杩愯緭鏂瑰紡鏂板',NULL,110,3,'','','system:transport:add',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1104,'杩愯緭鏂瑰紡缂栬緫',NULL,110,3,'','','system:transport:edit',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1105,'杩愯緭鏂瑰紡鍒犻櫎',NULL,110,3,'','','system:transport:delete',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1111,'璐у竵鏌ヨ',NULL,111,3,'','','system:currency:query',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1112,'璐у竵鏂板',NULL,111,3,'','','system:currency:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1113,'璐у竵缂栬緫',NULL,111,3,'','','system:currency:edit',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(1114,'璐у竵鍒犻櫎',NULL,111,3,'','','system:currency:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2010,'鐢虫姤鏌ヨ',NULL,201,3,'','','business:declaration:list',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2011,'鏂板鐢虫姤',NULL,201,3,'','','business:declaration:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2012,'缂栬緫鐢虫姤',NULL,201,3,'','','business:declaration:update',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2013,'鍒犻櫎鐢虫姤',NULL,201,3,'','','business:declaration:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2014,'鏌ョ湅鐢虫姤',NULL,201,3,'','','business:declaration:view',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2015,'璇︽儏鏌ヨ',NULL,201,3,'','','business:declaration:query',NULL,6,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2016,'鎻愪氦鐢虫姤',NULL,201,3,'','','business:declaration:submit',NULL,7,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2017,'鐢虫姤瀹℃牳',NULL,201,3,'','','business:declaration:audit',NULL,8,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2018,'瀵煎嚭鐢虫姤',NULL,201,3,'','','business:declaration:export',NULL,9,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2019,'涓嬭浇鍗曡瘉',NULL,201,3,'','','business:declaration:download',NULL,10,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2020,'鐢熸垚鍚堝悓',NULL,201,3,'','','business:declaration:contract',NULL,11,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2021,'浠樻绠＄悊',NULL,201,3,'','','business:declaration:payment',NULL,12,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2022,'鐢虫姤鏉冮檺绠＄悊',NULL,201,3,'','','business:declaration:edit',NULL,13,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2023,'鎻愯揣鍗曟彁浜?,NULL,201,3,'','','business:declaration:pickup-submit',NULL,14,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2024,'鎻愯揣鍗曞鏍?,NULL,201,3,'','','business:declaration:pickup-audit',NULL,15,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(2025,'鎻愯揣鍗曞垹闄?,NULL,201,3,'','','business:declaration:pickup-delete',NULL,16,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3011,'鐢宠鏌ヨ',NULL,301,3,'','','business:tax-refund:list',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3012,'鐢宠鏂板',NULL,301,3,'','','business:tax-refund:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3013,'鐢宠缂栬緫',NULL,301,3,'','','business:tax-refund:edit',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3014,'鐢宠鍒犻櫎',NULL,301,3,'','','business:tax-refund:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3015,'鐢宠鎻愪氦',NULL,301,3,'','','business:tax-refund:submit',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3016,'璇︽儏鏌ヨ',NULL,301,3,'','','business:tax-refund:detail',NULL,6,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3021,'鍒楄〃鏌ヨ',NULL,302,3,'','','business:tax-refund:list:query',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3022,'鎬昏瀹℃牳',NULL,302,3,'','','business:tax-refund:approve',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3023,'绯荤粺瀹℃牳',NULL,302,3,'','','system:tax-refund:approve',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3024,'璐㈠姟鍒濆',NULL,302,3,'','','system:tax-refund:first-review',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3025,'璐㈠姟澶嶅',NULL,302,3,'','','system:tax-refund:final-review',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(3026,'涓氬姟璐㈠姟瀹℃牳',NULL,302,3,'','','business:tax-refund:finance',NULL,6,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4011,'娴佺▼閮ㄧ讲',NULL,401,3,'','','workflow:definition:deploy',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4012,'娴佺▼鏇存柊',NULL,401,3,'','','workflow:definition:update',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4013,'娴佺▼鏌ョ湅',NULL,401,3,'','','workflow:definition:view',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4041,'娴佺▼瀹炰緥缁堟',NULL,404,3,'','','workflow:instance:terminate',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4042,'瀹炰緥鍚姩',NULL,404,3,'','','workflow:instance:start',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4043,'瀹炰緥鏆傚仠',NULL,404,3,'','','workflow:instance:suspend',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4044,'瀹炰緥婵€娲?,NULL,404,3,'','','workflow:instance:activate',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4051,'浠诲姟椹冲洖',NULL,405,3,'','','workflow:task:reject',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4052,'浠诲姟杞淳',NULL,405,3,'','','workflow:task:transfer',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4053,'浠诲姟鍒楄〃',NULL,405,3,'','','workflow:task:list',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4054,'浠诲姟绛炬敹',NULL,405,3,'','','workflow:task:claim',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(4055,'浠诲姟鍔炵悊',NULL,405,3,'','','workflow:task:complete',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(5011,'妯℃澘鏌ヨ',NULL,501,3,'','','business:contract:template:query',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(5012,'妯℃澘鏂板',NULL,501,3,'','','business:contract:template:add',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(5013,'妯℃澘缂栬緫',NULL,501,3,'','','business:contract:template:update',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(5014,'妯℃澘鍒犻櫎',NULL,501,3,'','','business:contract:template:delete',NULL,4,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(5015,'妯℃澘涓婁紶',NULL,501,3,'','','business:contract:template:upload',NULL,5,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(5016,'鎸夋ā鏉跨敓鎴愬悎鍚?,NULL,501,3,'','','business:contract:generate',NULL,6,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(5021,'鍚堝悓鏌ヨ',NULL,502,3,'','','business:contract:generation:query',NULL,1,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(5022,'鍚堝悓涓嬭浇',NULL,502,3,'','','business:contract:download',NULL,2,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL),(5023,'鍚堝悓鍒犻櫎',NULL,502,3,'','','business:contract:generation:delete',NULL,3,0,0,1,1,0,'2026-03-24 10:12:15','2026-03-24 10:12:15',NULL,NULL);
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_operation_log`
--

DROP TABLE IF EXISTS `sys_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `user_id` bigint DEFAULT NULL COMMENT '鐢ㄦ埛ID',
  `username` varchar(50) DEFAULT NULL COMMENT '鐢ㄦ埛鍚?,
  `operation_type` varchar(50) NOT NULL COMMENT '鎿嶄綔绫诲瀷锛圕REATE/UPDATE/DELETE/QUERY/EXPORT/IMPORT绛夛級',
  `business_type` varchar(50) DEFAULT NULL COMMENT '涓氬姟绫诲瀷锛堢敵鎶ョ鐞?鍚堝悓绠＄悊/绯荤粺閰嶇疆绛夛級',
  `method` varchar(10) DEFAULT NULL COMMENT '璇锋眰鏂规硶',
  `request_url` varchar(255) DEFAULT NULL COMMENT '璇锋眰URL',
  `request_params` text COMMENT '璇锋眰鍙傛暟',
  `response_result` text COMMENT '鍝嶅簲缁撴灉',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '鎿嶄綔IP鍦板潃',
  `location` varchar(255) DEFAULT NULL COMMENT '鎿嶄綔鍦扮偣',
  `browser` varchar(50) DEFAULT NULL COMMENT '娴忚鍣ㄧ被鍨?,
  `os` varchar(50) DEFAULT NULL COMMENT '鎿嶄綔绯荤粺',
  `status` tinyint(1) DEFAULT '0' COMMENT '鎿嶄綔鐘舵€侊紙0鎴愬姛 1澶辫触锛?,
  `error_msg` text COMMENT '閿欒娑堟伅',
  `cost_time` bigint DEFAULT NULL COMMENT '鎿嶄綔鑰楁椂锛堟绉掞級',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_business_type` (`business_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鎿嶄綔鏃ュ織琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_operation_log`
--

LOCK TABLES `sys_operation_log` WRITE;
/*!40000 ALTER TABLE `sys_operation_log` DISABLE KEYS */;
INSERT INTO `sys_operation_log` VALUES (1,1,'admin','LOGIN','绯荤粺绠＄悊','POST','/user/login',NULL,NULL,'127.0.0.1',NULL,'Chrome','Windows',0,NULL,156,'2026-03-23 08:28:38','2026-03-23 08:28:38'),(2,1,'admin','QUERY','鐢虫姤绠＄悊','GET','/declaration/list',NULL,NULL,'127.0.0.1',NULL,'Chrome','Windows',0,NULL,89,'2026-03-23 08:28:38','2026-03-23 08:28:38'),(3,1,'admin','CREATE','鍚堝悓绠＄悊','POST','/contract/generate',NULL,NULL,'127.0.0.1',NULL,'Chrome','Windows',0,NULL,234,'2026-03-23 08:28:38','2026-03-23 08:28:38');
/*!40000 ALTER TABLE `sys_operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_org`
--

DROP TABLE IF EXISTS `sys_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `org_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '缁勭粐鍚嶇О',
  `org_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '缁勭粐缂栫爜',
  `parent_id` bigint DEFAULT NULL COMMENT '鐖剁骇ID',
  `level` int NOT NULL DEFAULT '1' COMMENT '缁勭粐灞傜骇',
  `sort` int NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `leader` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '璐熻矗浜?,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鑱旂郴鐢佃瘽',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '閭',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-绂佺敤 1-鍚敤',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑 0-鏈垹闄?1-宸插垹闄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_org_code` (`org_code`) USING BTREE COMMENT '缁勭粐缂栫爜鍞竴绱㈠紩',
  KEY `idx_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='缁勭粐鏈烘瀯琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_org`
--

LOCK TABLES `sys_org` WRITE;
/*!40000 ALTER TABLE `sys_org` DISABLE KEYS */;
INSERT INTO `sys_org` VALUES (1,'绯荤粺','SYSTEM',0,1,1,'寮犱笁','13800138000','company@example.com',1,0,'2026-03-13 05:11:53','2026-03-14 13:34:44',NULL,NULL),(2,'瀹佹尝姊撶啝绉戞妧鏈夐檺鍏徃','NINGBO ZIYI',1,2,1,'鏉庡洓','13800138001','tech@example.com',1,0,'2026-03-13 05:11:53','2026-03-14 13:36:05',NULL,NULL),(3,'鏉窞闆嗘礇','HANGZHOU JILUO',1,2,2,'鐜嬩簲','13800138002','sales@example.com',1,0,'2026-03-13 05:11:53','2026-03-14 13:40:05',NULL,NULL),(4,'璐㈠姟閮?,'ZIYI FINANCE ',2,3,0,'鏉庡洓','13800138001','tech@example.com',1,0,'2026-03-14 13:37:54','2026-03-14 13:37:54',NULL,NULL),(5,'涓氬姟閮?,'BUSINESS',3,3,0,'','','',1,0,'2026-03-14 13:41:29','2026-03-14 13:41:29',NULL,NULL);
/*!40000 ALTER TABLE `sys_org` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '瑙掕壊鍚嶇О',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '瑙掕壊缂栫爜',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '瑙掕壊鎻忚堪',
  `data_scope` tinyint NOT NULL DEFAULT '1' COMMENT '鏁版嵁鏉冮檺鑼冨洿 1-鍏ㄩ儴 2-鏈骇 3-鏈骇鍙婁笅绾?4-鑷畾涔?,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-绂佺敤 1-鍚敤',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑 0-鏈垹闄?1-宸插垹闄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_code` (`role_code`) USING BTREE COMMENT '瑙掕壊缂栫爜鍞竴绱㈠紩'
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='瑙掕壊琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'瓒呯骇绠＄悊鍛?,'ADMIN','绯荤粺瓒呯骇绠＄悊鍛橈紝鎷ユ湁鎵€鏈夋潈闄?,1,1,0,'2026-03-13 05:11:53','2026-03-13 05:11:53',NULL,NULL),(2,'鏅€氱敤鎴?,'USER','鏅€氱敤鎴疯鑹?,3,1,1,'2026-03-13 05:11:53','2026-03-14 13:55:22',NULL,NULL),(3,'閮ㄩ棬绠＄悊鍛?,'DEPT_ADMIN','閮ㄩ棬绠＄悊鍛樿鑹?,3,1,1,'2026-03-13 05:11:53','2026-03-14 13:55:24',NULL,NULL),(4,'璐㈠姟','FINANCE','璐㈠姟鏉冮檺,鐢ㄤ簬瀹℃牳',1,1,0,'2026-03-20 09:31:35','2026-03-20 09:31:50',NULL,NULL),(5,'涓氬姟鍛?,'SALESPERSON','',1,1,0,'2026-03-20 09:32:38','2026-03-20 09:32:38',NULL,NULL);
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `role_id` bigint NOT NULL COMMENT '瑙掕壊ID',
  `menu_id` bigint NOT NULL COMMENT '鑿滃崟ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`) USING BTREE COMMENT '瑙掕壊鑿滃崟鍞竴绱㈠紩',
  KEY `idx_role_id` (`role_id`) USING BTREE,
  KEY `idx_menu_id` (`menu_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=158 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='瑙掕壊鑿滃崟鍏宠仈琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` VALUES (1,1,1,'2026-03-24 10:12:15',NULL),(2,1,2,'2026-03-24 10:12:15',NULL),(3,1,3,'2026-03-24 10:12:15',NULL),(4,1,100,'2026-03-24 10:12:15',NULL),(5,1,200,'2026-03-24 10:12:15',NULL),(6,1,300,'2026-03-24 10:12:15',NULL),(7,1,400,'2026-03-24 10:12:15',NULL),(8,1,500,'2026-03-24 10:12:15',NULL),(9,1,600,'2026-03-24 10:12:15',NULL),(10,1,4,'2026-03-24 10:12:15',NULL),(11,1,101,'2026-03-24 10:12:15',NULL),(12,1,102,'2026-03-24 10:12:15',NULL),(13,1,103,'2026-03-24 10:12:15',NULL),(14,1,104,'2026-03-24 10:12:15',NULL),(15,1,105,'2026-03-24 10:12:15',NULL),(16,1,106,'2026-03-24 10:12:15',NULL),(17,1,107,'2026-03-24 10:12:15',NULL),(18,1,108,'2026-03-24 10:12:15',NULL),(19,1,109,'2026-03-24 10:12:15',NULL),(20,1,110,'2026-03-24 10:12:15',NULL),(21,1,111,'2026-03-24 10:12:15',NULL),(22,1,1011,'2026-03-24 10:12:15',NULL),(23,1,1012,'2026-03-24 10:12:15',NULL),(24,1,1013,'2026-03-24 10:12:15',NULL),(25,1,1014,'2026-03-24 10:12:15',NULL),(26,1,1015,'2026-03-24 10:12:15',NULL),(27,1,1016,'2026-03-24 10:12:15',NULL),(28,1,1021,'2026-03-24 10:12:15',NULL),(29,1,1022,'2026-03-24 10:12:15',NULL),(30,1,1023,'2026-03-24 10:12:15',NULL),(31,1,1024,'2026-03-24 10:12:15',NULL),(32,1,1025,'2026-03-24 10:12:15',NULL),(33,1,1026,'2026-03-24 10:12:15',NULL),(34,1,1027,'2026-03-24 10:12:15',NULL),(35,1,1028,'2026-03-24 10:12:15',NULL),(36,1,1031,'2026-03-24 10:12:15',NULL),(37,1,1032,'2026-03-24 10:12:15',NULL),(38,1,1033,'2026-03-24 10:12:15',NULL),(39,1,1034,'2026-03-24 10:12:15',NULL),(40,1,1035,'2026-03-24 10:12:15',NULL),(41,1,1036,'2026-03-24 10:12:15',NULL),(42,1,1041,'2026-03-24 10:12:15',NULL),(43,1,1042,'2026-03-24 10:12:15',NULL),(44,1,1043,'2026-03-24 10:12:15',NULL),(45,1,1044,'2026-03-24 10:12:15',NULL),(46,1,1045,'2026-03-24 10:12:15',NULL),(47,1,1051,'2026-03-24 10:12:15',NULL),(48,1,1052,'2026-03-24 10:12:15',NULL),(49,1,1053,'2026-03-24 10:12:15',NULL),(50,1,1054,'2026-03-24 10:12:15',NULL),(51,1,1055,'2026-03-24 10:12:15',NULL),(52,1,1061,'2026-03-24 10:12:15',NULL),(53,1,1062,'2026-03-24 10:12:15',NULL),(54,1,1063,'2026-03-24 10:12:15',NULL),(55,1,1064,'2026-03-24 10:12:15',NULL),(56,1,1065,'2026-03-24 10:12:15',NULL),(57,1,1071,'2026-03-24 10:12:15',NULL),(58,1,1072,'2026-03-24 10:12:15',NULL),(59,1,1073,'2026-03-24 10:12:15',NULL),(60,1,1074,'2026-03-24 10:12:15',NULL),(61,1,1075,'2026-03-24 10:12:15',NULL),(62,1,1091,'2026-03-24 10:12:15',NULL),(63,1,1092,'2026-03-24 10:12:15',NULL),(64,1,1093,'2026-03-24 10:12:15',NULL),(65,1,1094,'2026-03-24 10:12:15',NULL),(66,1,1095,'2026-03-24 10:12:15',NULL),(67,1,1101,'2026-03-24 10:12:15',NULL),(68,1,1102,'2026-03-24 10:12:15',NULL),(69,1,1103,'2026-03-24 10:12:15',NULL),(70,1,1104,'2026-03-24 10:12:15',NULL),(71,1,1105,'2026-03-24 10:12:15',NULL),(72,1,1111,'2026-03-24 10:12:15',NULL),(73,1,1112,'2026-03-24 10:12:15',NULL),(74,1,1113,'2026-03-24 10:12:15',NULL),(75,1,1114,'2026-03-24 10:12:15',NULL),(76,1,201,'2026-03-24 10:12:15',NULL),(77,1,202,'2026-03-24 10:12:15',NULL),(78,1,203,'2026-03-24 10:12:15',NULL),(79,1,204,'2026-03-24 10:12:15',NULL),(80,1,205,'2026-03-24 10:12:15',NULL),(81,1,2010,'2026-03-24 10:12:15',NULL),(82,1,2011,'2026-03-24 10:12:15',NULL),(83,1,2012,'2026-03-24 10:12:15',NULL),(84,1,2013,'2026-03-24 10:12:15',NULL),(85,1,2014,'2026-03-24 10:12:15',NULL),(86,1,2015,'2026-03-24 10:12:15',NULL),(87,1,2016,'2026-03-24 10:12:15',NULL),(88,1,2017,'2026-03-24 10:12:15',NULL),(89,1,2018,'2026-03-24 10:12:15',NULL),(90,1,2019,'2026-03-24 10:12:15',NULL),(91,1,2020,'2026-03-24 10:12:15',NULL),(92,1,2021,'2026-03-24 10:12:15',NULL),(93,1,2022,'2026-03-24 10:12:15',NULL),(94,1,2023,'2026-03-24 10:12:15',NULL),(95,1,2024,'2026-03-24 10:12:15',NULL),(96,1,2025,'2026-03-24 10:12:15',NULL),(97,1,301,'2026-03-24 10:12:15',NULL),(98,1,302,'2026-03-24 10:12:15',NULL),(99,1,303,'2026-03-24 10:12:15',NULL),(100,1,3011,'2026-03-24 10:12:15',NULL),(101,1,3012,'2026-03-24 10:12:15',NULL),(102,1,3013,'2026-03-24 10:12:15',NULL),(103,1,3014,'2026-03-24 10:12:15',NULL),(104,1,3015,'2026-03-24 10:12:15',NULL),(105,1,3016,'2026-03-24 10:12:15',NULL),(106,1,3021,'2026-03-24 10:12:15',NULL),(107,1,3022,'2026-03-24 10:12:15',NULL),(108,1,3023,'2026-03-24 10:12:15',NULL),(109,1,3024,'2026-03-24 10:12:15',NULL),(110,1,3025,'2026-03-24 10:12:15',NULL),(111,1,3026,'2026-03-24 10:12:15',NULL),(112,1,401,'2026-03-24 10:12:15',NULL),(113,1,402,'2026-03-24 10:12:15',NULL),(114,1,403,'2026-03-24 10:12:15',NULL),(115,1,404,'2026-03-24 10:12:15',NULL),(116,1,405,'2026-03-24 10:12:15',NULL),(117,1,4011,'2026-03-24 10:12:15',NULL),(118,1,4012,'2026-03-24 10:12:15',NULL),(119,1,4013,'2026-03-24 10:12:15',NULL),(120,1,4041,'2026-03-24 10:12:15',NULL),(121,1,4042,'2026-03-24 10:12:15',NULL),(122,1,4043,'2026-03-24 10:12:15',NULL),(123,1,4044,'2026-03-24 10:12:15',NULL),(124,1,4051,'2026-03-24 10:12:15',NULL),(125,1,4052,'2026-03-24 10:12:15',NULL),(126,1,4053,'2026-03-24 10:12:15',NULL),(127,1,4054,'2026-03-24 10:12:15',NULL),(128,1,4055,'2026-03-24 10:12:15',NULL),(129,1,501,'2026-03-24 10:12:15',NULL),(130,1,502,'2026-03-24 10:12:15',NULL),(131,1,5011,'2026-03-24 10:12:15',NULL),(132,1,5012,'2026-03-24 10:12:15',NULL),(133,1,5013,'2026-03-24 10:12:15',NULL),(134,1,5014,'2026-03-24 10:12:15',NULL),(135,1,5015,'2026-03-24 10:12:15',NULL),(136,1,5016,'2026-03-24 10:12:15',NULL),(137,1,5021,'2026-03-24 10:12:15',NULL),(138,1,5022,'2026-03-24 10:12:15',NULL),(139,1,5023,'2026-03-24 10:12:15',NULL),(140,1,601,'2026-03-24 10:12:15',NULL),(141,1,602,'2026-03-24 10:12:15',NULL),(142,1,603,'2026-03-24 10:12:15',NULL),(143,1,6011,'2026-03-24 10:12:15',NULL),(144,1,6012,'2026-03-24 10:12:15',NULL),(145,1,6013,'2026-03-24 10:12:15',NULL),(146,1,6014,'2026-03-24 10:12:15',NULL),(147,1,6015,'2026-03-24 10:12:15',NULL),(148,1,6021,'2026-03-24 10:12:15',NULL),(149,1,6022,'2026-03-24 10:12:15',NULL),(150,1,6023,'2026-03-24 10:12:15',NULL),(151,1,6024,'2026-03-24 10:12:15',NULL),(152,1,6025,'2026-03-24 10:12:15',NULL),(153,1,6031,'2026-03-24 10:12:15',NULL),(154,1,6032,'2026-03-24 10:12:15',NULL),(155,1,6033,'2026-03-24 10:12:15',NULL),(156,1,6034,'2026-03-24 10:12:15',NULL),(157,1,6035,'2026-03-24 10:12:15',NULL);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '鐢ㄦ埛鍚?,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '瀵嗙爜',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鏄电О',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鎵嬫満鍙?,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '閭',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '澶村儚',
  `org_id` bigint DEFAULT NULL COMMENT '鎵€灞炵粍缁嘔D',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-绂佺敤 1-鍚敤',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑 0-鏈垹闄?1-宸插垹闄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE COMMENT '鐢ㄦ埛鍚嶅敮涓€绱㈠紩',
  KEY `idx_org_id` (`org_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='绯荤粺鐢ㄦ埛琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','21232f297a57a5a743894a0e4a801fc3','绠＄悊鍛?,'13800138000','admin@example.com',NULL,1,1,0,'2026-03-13 10:00:00','2026-03-13 10:00:00',NULL,NULL),(2,'Finance01','250dfb30df927257fe9401c0c89c0134','Finance01','','',NULL,4,1,0,'2026-03-20 10:01:46','2026-03-20 11:27:54',NULL,NULL),(3,'user1','8a13a81b63c9f02d897e8b39dd21372f','user1','','',NULL,5,1,0,'2026-03-20 11:30:57','2026-03-20 11:30:57',NULL,NULL);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_org`
--

DROP TABLE IF EXISTS `sys_user_org`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `org_id` bigint NOT NULL COMMENT '缁勭粐ID',
  `is_main` tinyint NOT NULL DEFAULT '0' COMMENT '鏄惁涓荤粍缁?0-鍚?1-鏄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_org` (`user_id`,`org_id`) USING BTREE COMMENT '鐢ㄦ埛缁勭粐鍞竴绱㈠紩',
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_org_id` (`org_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='鐢ㄦ埛缁勭粐鍏宠仈琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_org`
--

LOCK TABLES `sys_user_org` WRITE;
/*!40000 ALTER TABLE `sys_user_org` DISABLE KEYS */;
INSERT INTO `sys_user_org` VALUES (1,1,1,1,'2026-03-13 05:11:54',NULL);
/*!40000 ALTER TABLE `sys_user_org` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `role_id` bigint NOT NULL COMMENT '瑙掕壊ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`) USING BTREE COMMENT '鐢ㄦ埛瑙掕壊鍞竴绱㈠紩',
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='鐢ㄦ埛瑙掕壊鍏宠仈琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1,1,'2026-03-13 05:11:53',NULL),(3,2,4,'2026-03-20 03:27:54',NULL),(4,3,5,'2026-03-20 03:30:56',NULL);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tax_refund_application`
--

DROP TABLE IF EXISTS `tax_refund_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tax_refund_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `application_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐢宠缂栧彿',
  `declaration_form_id` bigint NOT NULL COMMENT '鍏宠仈鐢虫姤鍗旾D',
  `initiator_id` bigint NOT NULL COMMENT '鐢宠浜篒D',
  `initiator_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐢宠浜哄鍚?,
  `org_id` bigint DEFAULT NULL COMMENT '鎵€灞炵粍缁嘔D',
  `department_id` bigint DEFAULT NULL COMMENT '鐢宠閮ㄩ棬ID',
  `department_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鐢宠閮ㄩ棬鍚嶇О',
  `application_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐢宠绫诲瀷(EXPORT_REFUND:鍑哄彛閫€绋?VAT_REFUND:澧炲€肩◣閫€绋?OTHER_REFUND:鍏朵粬閫€绋?',
  `amount` decimal(15,2) NOT NULL COMMENT '鐢宠閲戦',
  `invoice_no` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍙戠エ鍙风爜',
  `invoice_amount` decimal(15,2) NOT NULL COMMENT '鍙戠エ閲戦',
  `tax_rate` decimal(5,2) NOT NULL COMMENT '绋庣巼(%)',
  `expected_refund_amount` decimal(15,2) GENERATED ALWAYS AS (((`invoice_amount` * `tax_rate`) / 100)) STORED COMMENT '棰勮閫€绋庨噾棰?,
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '鐢宠璇存槑',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '鐘舵€?0:鑽夌,1:宸叉彁浜?2:璐㈠姟鍒濆,3:鏂囦欢鐢熸垚,4:閫€鍥炶ˉ鍏?5:鍙戠エ鎻愪氦,6:璐㈠姟澶嶅,7:宸插畬鎴?8:宸叉嫆缁?',
  `current_approver_id` bigint DEFAULT NULL COMMENT '褰撳墠瀹℃壒浜篒D',
  `current_approver_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '褰撳墠瀹℃壒浜哄鍚?,
  `reject_reason` text COLLATE utf8mb4_unicode_ci COMMENT '鎷掔粷鍘熷洜',
  `return_reason` text COLLATE utf8mb4_unicode_ci COMMENT '閫€鍥炶ˉ鍏呭師鍥?,
  `first_review_opinion` text COLLATE utf8mb4_unicode_ci COMMENT '鍒濆鎰忚',
  `final_review_opinion` text COLLATE utf8mb4_unicode_ci COMMENT '澶嶅鎰忚',
  `file_path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鐢熸垚鐨勯€€绋庢枃浠惰矾寰?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑(0:鏈垹闄?1:宸插垹闄?',
  `first_reviewer_id` bigint DEFAULT NULL COMMENT '氓藛聺氓庐隆盲潞潞ID',
  `first_reviewer_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '氓藛聺氓庐隆盲潞潞氓聬聧莽搂掳',
  `first_review_time` datetime DEFAULT NULL COMMENT '氓藛聺氓庐隆忙鈥斅睹┾€斅?,
  `final_reviewer_id` bigint DEFAULT NULL COMMENT '氓陇聧氓庐隆盲潞潞ID',
  `final_reviewer_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '氓陇聧氓庐隆盲潞潞氓聬聧莽搂掳',
  `final_review_time` datetime DEFAULT NULL COMMENT '氓陇聧氓庐隆忙鈥斅睹┾€斅?,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_declaration_form_id` (`declaration_form_id`),
  KEY `idx_initiator_id` (`initiator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_tax_refund_org_id` (`org_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='閫€绋庣敵璇疯〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tax_refund_application`
--

LOCK TABLES `tax_refund_application` WRITE;
/*!40000 ALTER TABLE `tax_refund_application` DISABLE KEYS */;
INSERT INTO `tax_refund_application` (`id`, `application_no`, `declaration_form_id`, `initiator_id`, `initiator_name`, `org_id`, `department_id`, `department_name`, `application_type`, `amount`, `invoice_no`, `invoice_amount`, `tax_rate`, `description`, `status`, `current_approver_id`, `current_approver_name`, `reject_reason`, `return_reason`, `first_review_opinion`, `final_review_opinion`, `file_path`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`, `first_reviewer_id`, `first_reviewer_name`, `first_review_time`, `final_reviewer_id`, `final_reviewer_name`, `final_review_time`) VALUES (23,'TR202603201439266038',24,3,'user1',5,NULL,NULL,'EXPORT_REFUND',11.00,'11',11.00,1.00,'111',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-20 06:39:08','2026-03-20 06:48:45',3,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL),(24,'TR202603201440246879',24,3,'user1',5,NULL,NULL,'EXPORT_REFUND',11.00,'11',11.00,1.00,'111',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-20 06:40:24','2026-03-20 06:48:33',3,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL),(25,'TR202603201442051451',24,3,'user1',5,NULL,NULL,'EXPORT_REFUND',11.00,'11',11.00,1.00,'111',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-20 06:41:47','2026-03-20 06:48:32',3,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL),(26,'TR202603201449142109',24,3,'user1',5,NULL,NULL,'EXPORT_REFUND',11.00,'11',11.00,1.00,'111',7,NULL,NULL,NULL,NULL,'111','111',NULL,'2026-03-20 06:49:14','2026-03-20 06:49:14',3,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `tax_refund_application` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tax_refund_attachment`
--

DROP TABLE IF EXISTS `tax_refund_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tax_refund_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `application_id` bigint NOT NULL COMMENT '閫€绋庣敵璇稩D',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鏂囦欢鍚?,
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鏂囦欢璺緞',
  `file_size` bigint NOT NULL COMMENT '鏂囦欢澶у皬(瀛楄妭)',
  `file_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鏂囦欢绫诲瀷',
  `attachment_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '闄勪欢绫诲瀷(INVOICE:鍙戠エ,CONTRACT:鍚堝悓,OTHER:鍏朵粬)',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '闄勪欢鎻忚堪',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '涓婁紶鏃堕棿',
  `uploader_id` bigint NOT NULL COMMENT '涓婁紶浜篒D',
  `uploader_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '涓婁紶浜哄鍚?,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='閫€绋庣敵璇烽檮浠惰〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tax_refund_attachment`
--

LOCK TABLES `tax_refund_attachment` WRITE;
/*!40000 ALTER TABLE `tax_refund_attachment` DISABLE KEYS */;
INSERT INTO `tax_refund_attachment` VALUES (7,15,'32e66d75-f551-49f1-bb06-999c9292cc5e.pdf','/api/v1/files/download?path=202603/32e66d75-f551-49f1-bb06-999c9292cc5e.pdf',0,'application/octet-stream','OTHER','閫€绋庣敵璇烽檮浠?,'2026-03-19 11:44:38',1,'绠＄悊鍛?,0),(8,16,'34fd7434-9dba-4b40-b664-779d38fc78e5.pdf','/api/v1/files/download?path=202603/34fd7434-9dba-4b40-b664-779d38fc78e5.pdf',0,'application/octet-stream','OTHER','閫€绋庣敵璇烽檮浠?,'2026-03-19 11:56:36',1,'绠＄悊鍛?,0),(9,17,'7f933747-54ce-4a46-b18b-59825ddc441d.pdf','/api/v1/files/download?path=202603/7f933747-54ce-4a46-b18b-59825ddc441d.pdf',0,'application/octet-stream','OTHER','閫€绋庣敵璇烽檮浠?,'2026-03-19 12:38:59',1,'绠＄悊鍛?,0),(10,18,'15f9d700-98b6-4c0f-bd84-d5f5e298cceb.png','/api/v1/files/download?path=202603/15f9d700-98b6-4c0f-bd84-d5f5e298cceb.png',0,'application/octet-stream','OTHER','閫€绋庣敵璇烽檮浠?,'2026-03-19 12:39:55',1,'绠＄悊鍛?,0),(11,19,'79a720bb-cb89-432b-b3b1-eb3a0343cebd.pdf','/api/v1/files/download?path=202603/79a720bb-cb89-432b-b3b1-eb3a0343cebd.pdf',0,'application/octet-stream','OTHER','閫€绋庣敵璇烽檮浠?,'2026-03-19 13:00:47',1,'绠＄悊鍛?,0),(12,20,'87326807-1662-41ee-908f-dfe6551d8330.png','/api/v1/files/download?path=202603/87326807-1662-41ee-908f-dfe6551d8330.png',0,'application/octet-stream','OTHER','閫€绋庣敵璇烽檮浠?,'2026-03-19 13:12:15',1,'绠＄悊鍛?,0),(13,21,'dfe4330d-9da5-4463-aae0-1b8043e1de9e.pdf','/api/v1/files/download?path=202603/dfe4330d-9da5-4463-aae0-1b8043e1de9e.pdf',0,'application/octet-stream','OTHER','閫€绋庣敵璇烽檮浠?,'2026-03-19 13:17:56',1,'绠＄悊鍛?,0),(14,22,'c3617ae1-c216-4316-910e-d2db93abd769.pdf','/api/v1/files/download?path=202603/c3617ae1-c216-4316-910e-d2db93abd769.pdf',0,'application/octet-stream','OTHER','閫€绋庣敵璇烽檮浠?,'2026-03-19 13:29:07',1,'绠＄悊鍛?,0),(15,25,'bf53ef9d-4cc5-4153-9745-75749cfd8850.pdf','/api/v1/files/download?path=202603/bf53ef9d-4cc5-4153-9745-75749cfd8850.pdf',0,'application/pdf','OTHER','閫€绋庣敵璇烽檮浠?,'2026-03-20 14:42:06',3,'user1',0),(16,26,'bf53ef9d-4cc5-4153-9745-75749cfd8850.pdf','/api/v1/files/download?path=202603/bf53ef9d-4cc5-4153-9745-75749cfd8850.pdf',0,'application/pdf','OTHER','閫€绋庣敵璇烽檮浠?,'2026-03-20 14:49:15',3,'user1',0);
/*!40000 ALTER TABLE `tax_refund_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tax_refund_process_node`
--

DROP TABLE IF EXISTS `tax_refund_process_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tax_refund_process_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `application_id` bigint NOT NULL COMMENT '閫€绋庣敵璇稩D',
  `node_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鑺傜偣绫诲瀷(SUBMIT:鎻愪氦,FIRST_REVIEW:鍒濆,FILE_GENERATE:鏂囦欢鐢熸垚,INVOICE_SUBMIT:鍙戠エ鎻愪氦,FINAL_REVIEW:澶嶅,COMPLETE:瀹屾垚,REJECT:鎷掔粷)',
  `operator_id` bigint NOT NULL COMMENT '鎿嶄綔浜篒D',
  `operator_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鎿嶄綔浜哄鍚?,
  `operation_result` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鎿嶄綔缁撴灉',
  `remark` text COLLATE utf8mb4_unicode_ci COMMENT '澶囨敞',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鎿嶄綔鏃堕棿',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='閫€绋庢祦绋嬭妭鐐硅〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tax_refund_process_node`
--

LOCK TABLES `tax_refund_process_node` WRITE;
/*!40000 ALTER TABLE `tax_refund_process_node` DISABLE KEYS */;
/*!40000 ALTER TABLE `tax_refund_process_node` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tax_refund_review_record`
--

DROP TABLE IF EXISTS `tax_refund_review_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tax_refund_review_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `application_id` bigint NOT NULL COMMENT '閫€绋庣敵璇稩D',
  `review_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '瀹℃牳绫诲瀷(FIRST_REVIEW:鍒濆,FINAL_REVIEW:澶嶅)',
  `reviewer_id` bigint NOT NULL COMMENT '瀹℃牳浜篒D',
  `reviewer_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '瀹℃牳浜哄鍚?,
  `review_result` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '瀹℃牳缁撴灉(APPROVED:閫氳繃,REJECTED:鎷掔粷,RETURNED:閫€鍥炶ˉ鍏?',
  `review_opinion` text COLLATE utf8mb4_unicode_ci COMMENT '瀹℃牳鎰忚',
  `review_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '瀹℃牳鏃堕棿',
  `next_approver_id` bigint DEFAULT NULL COMMENT '涓嬩竴瀹℃壒浜篒D',
  `next_approver_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '涓嬩竴瀹℃壒浜哄鍚?,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_reviewer_id` (`reviewer_id`),
  KEY `idx_review_time` (`review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='閫€绋庡鏍歌褰曡〃';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tax_refund_review_record`
--

LOCK TABLES `tax_refund_review_record` WRITE;
/*!40000 ALTER TABLE `tax_refund_review_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `tax_refund_review_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transport_mode`
--

DROP TABLE IF EXISTS `transport_mode`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transport_mode` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `name` varchar(100) NOT NULL COMMENT '杩愯緭鏂瑰紡鍚嶇О(鑻辨枃)',
  `chinese_name` varchar(100) DEFAULT '' COMMENT '杩愯緭鏂瑰紡鍚嶇О(涓枃)',
  `code` varchar(50) DEFAULT '' COMMENT '杩愯緭鏂瑰紡浠ｇ爜',
  `description` varchar(255) DEFAULT '' COMMENT '鎻忚堪',
  `status` int DEFAULT '1' COMMENT '鐘舵€?0绂佺敤 1鍚敤)',
  `sort` int DEFAULT '0' COMMENT '鎺掑簭',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  `del_flag` int DEFAULT '0' COMMENT '鍒犻櫎鏍囧織(0姝ｅ父 1鍒犻櫎)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='杩愯緭鏂瑰紡閰嶇疆';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transport_mode`
--

LOCK TABLES `transport_mode` WRITE;
/*!40000 ALTER TABLE `transport_mode` DISABLE KEYS */;
INSERT INTO `transport_mode` VALUES (1,'AIR FREIGHT','绌鸿繍','AIR','',1,1,'2026-03-23 04:43:14','2026-03-23 04:43:14',NULL,NULL,0),(2,'TRUCK','闄嗚繍','TRUCK','',1,2,'2026-03-23 04:43:14','2026-03-23 04:43:14',NULL,NULL,0),(3,'BY SEA','娴疯繍','SEA','',1,3,'2026-03-23 04:43:14','2026-03-23 04:43:14',NULL,NULL,0),(4,'EXPRESS','蹇€?,'EXPRESS','',1,4,'2026-03-23 04:43:14','2026-03-23 04:43:14',NULL,NULL,0);
/*!40000 ALTER TABLE `transport_mode` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_process_definition`
--

DROP TABLE IF EXISTS `wf_process_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_process_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `process_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '娴佺▼瀹氫箟KEY',
  `process_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '娴佺▼鍚嶇О',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '娴佺▼鎻忚堪',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '娴佺▼鍒嗙被',
  `version` int NOT NULL DEFAULT '1' COMMENT '鐗堟湰鍙?,
  `bpmn_xml` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '娴佺▼XML鍐呭',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '鐘舵€?0-鍋滅敤 1-鍚敤',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑 0-鏈垹闄?1-宸插垹闄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_process_key_version` (`process_key`,`version`) USING BTREE COMMENT '娴佺▼KEY鍜岀増鏈敮涓€绱㈠紩',
  KEY `idx_process_key` (`process_key`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='娴佺▼瀹氫箟琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_process_definition`
--

LOCK TABLES `wf_process_definition` WRITE;
/*!40000 ALTER TABLE `wf_process_definition` DISABLE KEYS */;
INSERT INTO `wf_process_definition` VALUES (1,'leave_process','璇峰亣瀹℃壒娴佺▼','鍛樺伐璇峰亣瀹℃壒娴佺▼','HR',1,'<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<bpmn2:definitions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xsi:schemaLocation=\"http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd\" id=\"sample-diagram\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\r\n  <bpmn2:process id=\"leave_process\" isExecutable=\"true\">\r\n    <bpmn2:startEvent id=\"StartEvent_1\" name=\"寮€濮媆" />\r\n    <bpmn2:userTask id=\"UserTask_1\" name=\"閮ㄩ棬缁忕悊瀹℃壒\" />\r\n    <bpmn2:userTask id=\"UserTask_2\" name=\"浜轰簨瀹℃壒\" />\r\n    <bpmn2:endEvent id=\"EndEvent_1\" name=\"缁撴潫\" />\r\n    <bpmn2:sequenceFlow id=\"SequenceFlow_1\" sourceRef=\"StartEvent_1\" targetRef=\"UserTask_1\" />\r\n    <bpmn2:sequenceFlow id=\"SequenceFlow_2\" sourceRef=\"UserTask_1\" targetRef=\"UserTask_2\" />\r\n    <bpmn2:sequenceFlow id=\"SequenceFlow_3\" sourceRef=\"UserTask_2\" targetRef=\"EndEvent_1\" />\r\n  </bpmn2:process>\r\n</bpmn2:definitions>',1,0,'2026-03-13 05:11:54','2026-03-13 05:11:54',NULL,NULL);
/*!40000 ALTER TABLE `wf_process_definition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_process_instance`
--

DROP TABLE IF EXISTS `wf_process_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_process_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '娴佺▼瀹炰緥ID',
  `definition_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '娴佺▼瀹氫箟ID',
  `process_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '娴佺▼瀹氫箟KEY',
  `process_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '娴佺▼鍚嶇О',
  `business_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '涓氬姟KEY',
  `starter_id` bigint DEFAULT NULL COMMENT '鍙戣捣浜篒D',
  `starter_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鍙戣捣浜哄鍚?,
  `current_activity_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '褰撳墠鑺傜偣ID',
  `current_activity_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '褰撳墠鑺傜偣鍚嶇О',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '娴佺▼鐘舵€?0-杩愯涓?1-宸插畬鎴?2-宸茬粓姝?3-宸叉寕璧?,
  `start_time` datetime DEFAULT NULL COMMENT '寮€濮嬫椂闂?,
  `end_time` datetime DEFAULT NULL COMMENT '缁撴潫鏃堕棿',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑 0-鏈垹闄?1-宸插垹闄?,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_instance_id` (`instance_id`) USING BTREE COMMENT '娴佺▼瀹炰緥ID鍞竴绱㈠紩',
  KEY `idx_definition_id` (`definition_id`) USING BTREE,
  KEY `idx_process_key` (`process_key`) USING BTREE,
  KEY `idx_business_key` (`business_key`) USING BTREE,
  KEY `idx_starter_id` (`starter_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='娴佺▼瀹炰緥琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_process_instance`
--

LOCK TABLES `wf_process_instance` WRITE;
/*!40000 ALTER TABLE `wf_process_instance` DISABLE KEYS */;
INSERT INTO `wf_process_instance` VALUES (1,'5001','declarationProcess:1:2504','declarationProcess',NULL,'19',NULL,NULL,NULL,NULL,0,'2026-03-18 11:41:06',NULL,0,'2026-03-18 11:41:06','2026-03-18 11:41:06',NULL,NULL),(2,'5010','declarationProcess:1:2504','declarationProcess',NULL,'19',NULL,NULL,NULL,NULL,0,'2026-03-18 11:41:06',NULL,0,'2026-03-18 11:41:06','2026-03-18 11:41:06',NULL,NULL),(3,'12501','declarationProcess:1:10004','declarationProcess',NULL,'22',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-18 14:20:07',NULL,0,'2026-03-18 14:20:07','2026-03-18 14:20:07',NULL,NULL),(4,'20021','declarationProcess:1:10004','declarationProcess',NULL,'23',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-18 15:59:05',NULL,0,'2026-03-18 07:59:04','2026-03-18 15:59:05',NULL,NULL),(5,'67501','declarationProcess:1:10004','declarationProcess',NULL,'24',3,'鐢ㄦ埛3',NULL,NULL,0,'2026-03-20 13:25:41',NULL,0,'2026-03-20 05:25:22','2026-03-20 13:25:41',NULL,NULL),(6,'75001','declarationProcess:1:10004','declarationProcess',NULL,'25',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-20 16:41:05',NULL,0,'2026-03-20 08:40:46','2026-03-20 16:41:05',NULL,NULL),(7,'77501','declarationProcess:1:10004','declarationProcess',NULL,'26',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-20 16:57:29',NULL,0,'2026-03-20 08:57:29','2026-03-20 16:57:29',NULL,NULL),(8,'77521','declarationProcess:1:10004','declarationProcess',NULL,'27',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-20 17:06:54',NULL,0,'2026-03-20 09:06:35','2026-03-20 17:06:54',NULL,NULL),(9,'80001','declarationProcess:1:10004','declarationProcess',NULL,'28',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-23 09:56:30',NULL,0,'2026-03-23 01:56:03','2026-03-23 09:56:30',NULL,NULL),(10,'82501','declarationProcess:1:10004','declarationProcess',NULL,'29',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-23 10:10:30',NULL,0,'2026-03-23 02:10:04','2026-03-23 10:10:30',NULL,NULL),(11,'87505','declarationProcess:2:87504','declarationProcess',NULL,'30',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-24 12:52:43',NULL,0,'2026-03-24 04:52:14','2026-03-24 12:52:43',NULL,NULL),(12,'90001','declarationProcess:2:87504','declarationProcess',NULL,'31',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-24 15:31:18',NULL,0,'2026-03-24 07:30:49','2026-03-24 15:31:18',NULL,NULL),(13,'95005','declarationProcess:6:95004','declarationProcess',NULL,'32',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-24 16:58:21',NULL,0,'2026-03-24 08:57:51','2026-03-24 16:58:21',NULL,NULL),(14,'100001','declarationProcess:6:95004','declarationProcess',NULL,'33',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-24 18:04:47',NULL,0,'2026-03-24 10:04:47','2026-03-24 18:04:47',NULL,NULL),(15,'100045','declarationProcess:6:95004','declarationProcess',NULL,'34',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-24 18:16:32',NULL,0,'2026-03-24 10:16:01','2026-03-24 18:16:32',NULL,NULL),(16,'102505','declarationProcess:7:102504','declarationProcess',NULL,'35',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 09:23:18',NULL,0,'2026-03-25 01:23:17','2026-03-25 09:23:18',NULL,NULL),(17,'102531','declarationProcess:7:102504','declarationProcess',NULL,'36',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 09:36:42',NULL,0,'2026-03-25 01:36:11','2026-03-25 09:36:42',NULL,NULL),(18,'105011','declarationProcess:7:102504','declarationProcess',NULL,'37',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 09:41:01',NULL,0,'2026-03-25 01:41:01','2026-03-25 09:41:01',NULL,NULL),(19,'105033','declarationProcess:7:102504','declarationProcess',NULL,'38',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 09:44:20',NULL,0,'2026-03-25 01:44:19','2026-03-25 09:44:20',NULL,NULL),(20,'107501','declarationProcess:7:102504','declarationProcess',NULL,'39',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 09:53:44',NULL,0,'2026-03-25 01:53:43','2026-03-25 09:53:44',NULL,NULL),(21,'110001','declarationProcess:7:102504','declarationProcess',NULL,'40',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 09:59:06',NULL,0,'2026-03-25 01:58:35','2026-03-25 09:59:06',NULL,NULL),(22,'112505','declarationProcess:8:112504','declarationProcess',NULL,'41',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 10:07:09',NULL,0,'2026-03-25 02:06:38','2026-03-25 10:07:09',NULL,NULL),(23,'115001','declarationProcess:8:112504','declarationProcess',NULL,'42',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 10:43:31',NULL,0,'2026-03-25 02:42:59','2026-03-25 10:43:31',NULL,NULL),(24,'115021','declarationProcess:8:112504','declarationProcess',NULL,'42',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 10:44:50',NULL,0,'2026-03-25 02:44:49','2026-03-25 10:44:50',NULL,NULL),(25,'117505','declarationProcess:8:112504','declarationProcess',NULL,'43',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 10:54:19',NULL,0,'2026-03-25 02:53:48','2026-03-25 10:54:19',NULL,NULL),(26,'122501','declarationProcess:8:112504','declarationProcess',NULL,'44',1,'鐢ㄦ埛1',NULL,NULL,0,'2026-03-25 15:06:56',NULL,0,'2026-03-25 07:06:56','2026-03-25 15:06:56',NULL,NULL);
/*!40000 ALTER TABLE `wf_process_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wf_task_instance`
--

DROP TABLE IF EXISTS `wf_task_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wf_task_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '浠诲姟ID',
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '浠诲姟鍚嶇О',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '浠诲姟鎻忚堪',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '娴佺▼瀹炰緥ID',
  `definition_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '娴佺▼瀹氫箟ID',
  `activity_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '鑺傜偣ID',
  `activity_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鑺傜偣鍚嶇О',
  `assignee_id` bigint DEFAULT NULL COMMENT '鍔炵悊浜篒D',
  `assignee_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鍔炵悊浜哄鍚?,
  `candidate_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鍊欓€変汉IDs',
  `candidate_group_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '鍊欓€夌粍IDs',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '浠诲姟鐘舵€?0-寰呭姙 1-宸插姙 2-宸叉挙鍥?3-宸茬粓姝?,
  `priority` int NOT NULL DEFAULT '50' COMMENT '浼樺厛绾?,
  `create_time` datetime NOT NULL COMMENT '鍒涘缓鏃堕棿',
  `claim_time` datetime DEFAULT NULL COMMENT '绛炬敹鏃堕棿',
  `end_time` datetime DEFAULT NULL COMMENT '瀹屾垚鏃堕棿',
  `due_time` datetime DEFAULT NULL COMMENT '鍒版湡鏃堕棿',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '鍒犻櫎鏍囪瘑 0-鏈垹闄?1-宸插垹闄?,
  `create_time_db` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鏁版嵁搴撳垱寤烘椂闂?,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `create_by` bigint DEFAULT NULL COMMENT '鍒涘缓浜?,
  `update_by` bigint DEFAULT NULL COMMENT '鏇存柊浜?,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_task_id` (`task_id`) USING BTREE COMMENT '浠诲姟ID鍞竴绱㈠紩',
  KEY `idx_instance_id` (`instance_id`) USING BTREE,
  KEY `idx_definition_id` (`definition_id`) USING BTREE,
  KEY `idx_activity_id` (`activity_id`) USING BTREE,
  KEY `idx_assignee_id` (`assignee_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='浠诲姟瀹炰緥琛?;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wf_task_instance`
--

LOCK TABLES `wf_task_instance` WRITE;
/*!40000 ALTER TABLE `wf_task_instance` DISABLE KEYS */;
/*!40000 ALTER TABLE `wf_task_instance` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-25  7:12:25
