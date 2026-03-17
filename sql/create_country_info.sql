-- 国家信息表
CREATE TABLE `country_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `country_code` varchar(10) NOT NULL COMMENT '国家代码(ISO标准)',
  `chinese_name` varchar(100) NOT NULL COMMENT '中文名称',
  `english_name` varchar(100) NOT NULL COMMENT '英文名称',
  `abbreviation` varchar(10) COMMENT '简称/缩写',
  `continent` varchar(50) COMMENT '所属洲',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_country_code` (`country_code`),
  UNIQUE KEY `uk_abbreviation` (`abbreviation`),
  KEY `idx_chinese_name` (`chinese_name`),
  KEY `idx_english_name` (`english_name`),
  KEY `idx_continent` (`continent`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='国家信息表';

-- 插入常用国家数据
INSERT INTO `country_info` (`country_code`, `chinese_name`, `english_name`, `abbreviation`, `continent`, `sort`) VALUES
('CHN', '中国', 'China', 'CHN', '亚洲', 1),
('USA', '美国', 'United States', 'USA', '北美洲', 2),
('GBR', '英国', 'United Kingdom', 'GBR', '欧洲', 3),
('DEU', '德国', 'Germany', 'DEU', '欧洲', 4),
('FRA', '法国', 'France', 'FRA', '欧洲', 5),
('JPN', '日本', 'Japan', 'JPN', '亚洲', 6),
('KOR', '韩国', 'South Korea', 'KOR', '亚洲', 7),
('AUS', '澳大利亚', 'Australia', 'AUS', '大洋洲', 8),
('CAN', '加拿大', 'Canada', 'CAN', 9),
('SGP', '新加坡', 'Singapore', 'SGP', '亚洲', 10),
('MYS', '马来西亚', 'Malaysia', 'MYS', '亚洲', 11),
('THA', '泰国', 'Thailand', 'THA', '亚洲', 12),
('VNM', '越南', 'Vietnam', 'VNM', '亚洲', 13),
('IND', '印度', 'India', 'IND', '亚洲', 14),
('BRA', '巴西', 'Brazil', 'BRA', '南美洲', 15),
('RUS', '俄罗斯', 'Russia', 'RUS', '欧洲', 16),
('ITA', '意大利', 'Italy', 'ITA', '欧洲', 17),
('ESP', '西班牙', 'Spain', 'ESP', '欧洲', 18),
('NLD', '荷兰', 'Netherlands', 'NLD', '欧洲', 19),
('BEL', '比利时', 'Belgium', 'BEL', '欧洲', 20);

-- 申报单表添加目的国字段
ALTER TABLE `declaration_form` 
ADD COLUMN `destination_country` varchar(10) COMMENT '目的国(3位代码)' AFTER `departure_city`;

-- HS商品表添加中文名字段（如果不存在）
ALTER TABLE `product_type_config` 
ADD COLUMN `chinese_name` varchar(255) COMMENT '中文名称' AFTER `english_name`;