-- HS商品类型表添加单位字段
-- 添加计量单位相关字段

ALTER TABLE `product_type_config` 
ADD COLUMN `unit_type` varchar(20) COMMENT '计量单位类型' AFTER `elements_config`,
ADD COLUMN `unit_code` varchar(10) COMMENT '计量单位代码' AFTER `unit_type`,
ADD COLUMN `unit_name` varchar(50) COMMENT '计量单位名称' AFTER `unit_code`;

-- 更新现有数据，添加默认单位信息
UPDATE `product_type_config` SET 
  `unit_type` = '个',
  `unit_code` = '01',
  `unit_name` = '个'
WHERE `hs_code` = '85235210';

UPDATE `product_type_config` SET 
  `unit_type` = '台',
  `unit_code` = '02',
  `unit_name` = '台'
WHERE `hs_code` = '85235290';

UPDATE `product_type_config` SET 
  `unit_type` = '个',
  `unit_code` = '01',
  `unit_name` = '个'
WHERE `hs_code` = '39269090';

UPDATE `product_type_config` SET 
  `unit_type` = '个',
  `unit_code` = '01',
  `unit_name` = '个'
WHERE `hs_code` = '48192000';

UPDATE `product_type_config` SET 
  `unit_type` = '个',
  `unit_code` = '01',
  `unit_name` = '个'
WHERE `hs_code` = '58071000';

UPDATE `product_type_config` SET 
  `unit_type` = '个',
  `unit_code` = '01',
  `unit_name` = '个'
WHERE `hs_code` = '48211000';

UPDATE `product_type_config` SET 
  `unit_type` = '个',
  `unit_code` = '01',
  `unit_name` = '个'
WHERE `hs_code` = '39232900';

-- 创建常用计量单位配置表
CREATE TABLE IF NOT EXISTS `measurement_units` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `unit_code` varchar(10) NOT NULL COMMENT '单位代码',
  `unit_name` varchar(50) NOT NULL COMMENT '单位名称',
  `unit_name_en` varchar(50) COMMENT '英文单位名称',
  `unit_type` varchar(20) NOT NULL COMMENT '单位类型',
  `description` varchar(200) COMMENT '单位描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unit_code` (`unit_code`),
  KEY `idx_unit_type` (`unit_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计量单位配置表';

-- 插入常用计量单位数据
INSERT INTO `measurement_units` (`unit_code`, `unit_name`, `unit_name_en`, `unit_type`, `description`, `sort`) VALUES
('01', '个', 'PCS', '数量单位', '基本计数单位', 1),
('02', '台', 'SETS', '数量单位', '设备台数', 2),
('03', '套', 'SUITS', '数量单位', '成套设备', 3),
('04', '件', 'ITEMS', '数量单位', '一般物品件数', 4),
('05', '只', 'ONLY', '数量单位', '动物、器具等', 5),
('06', '张', 'SHEETS', '数量单位', '纸张、票证等', 6),
('07', '支', 'BRANCHES', '数量单位', '笔、管状物等', 7),
('08', '根', 'ROOTS', '数量单位', '棒状物', 8),
('09', '条', 'STRIPS', '数量单位', '长条形物品', 9),
('10', '块', 'BLOCKS', '数量单位', '块状物品', 10),
('11', '千克', 'KILOS', '重量单位', '公斤', 11),
('12', '克', 'GRAMS', '重量单位', '克', 12),
('13', '吨', 'TONS', '重量单位', '公吨', 13),
('14', '立方米', 'CBM', '体积单位', '立方米', 14),
('15', '平方米', 'SQM', '面积单位', '平方米', 15),
('16', '米', 'METERS', '长度单位', '米', 16),
('17', '厘米', 'CM', '长度单位', '厘米', 17),
('18', '毫米', 'MM', '长度单位', '毫米', 18),
('19', '升', 'LITERS', '容量单位', '升', 19),
('20', '毫升', 'ML', '容量单位', '毫升', 20);