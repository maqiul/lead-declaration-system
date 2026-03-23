-- =============================================
-- 货币信息管理 - 建表和初始化数据
-- =============================================

-- 1. 创建货币信息表
CREATE TABLE IF NOT EXISTS `currency_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `currency_code` varchar(10) NOT NULL COMMENT '货币代码(ISO 4217)',
  `currency_name` varchar(50) DEFAULT NULL COMMENT '英文名称',
  `chinese_name` varchar(50) NOT NULL COMMENT '中文名称',
  `unit_cn` varchar(20) NOT NULL COMMENT '中文单位',
  `symbol` varchar(10) DEFAULT NULL COMMENT '货币符号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_currency_code` (`currency_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='货币信息表';

-- 2. 初始化常用货币数据
INSERT INTO `currency_info` (`currency_code`, `currency_name`, `chinese_name`, `unit_cn`, `symbol`, `status`, `sort`) VALUES
('CNY', 'Chinese Yuan', '人民币', '元', '¥', 1, 1),
('USD', 'US Dollar', '美元', '美分', '$', 1, 2),
('EUR', 'Euro', '欧元', '分', '€', 1, 3),
('GBP', 'British Pound', '英镑', '便士', '£', 1, 4),
('JPY', 'Japanese Yen', '日元', '円', '¥', 1, 5),
('HKD', 'Hong Kong Dollar', '港币', '仙', 'HK$', 1, 6),
('KRW', 'South Korean Won', '韩元', '元', '₩', 1, 7),
('SGD', 'Singapore Dollar', '新加坡元', '分', 'S$', 1, 8),
('AUD', 'Australian Dollar', '澳元', '分', 'A$', 1, 9),
('CAD', 'Canadian Dollar', '加元', '分', 'CA$', 1, 10),
('TWD', 'New Taiwan Dollar', '新台币', '角', 'NT$', 1, 11),
('THB', 'Thai Baht', '泰铢', '萨当', '฿', 1, 12),
('MYR', 'Malaysian Ringgit', '马来西亚林吉特', '仙', 'RM', 1, 13),
('VND', 'Vietnamese Dong', '越南盾', '毫', '₫', 1, 14),
('RUB', 'Russian Ruble', '俄罗斯卢布', '戈比', '₽', 1, 15),
('INR', 'Indian Rupee', '印度卢比', '派士', '₹', 1, 16)
ON DUPLICATE KEY UPDATE currency_name = VALUES(currency_name);
