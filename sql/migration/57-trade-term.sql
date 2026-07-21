-- ============================================================
-- 贸易方式(Incoterms)管理表
--   用于管理国际贸易术语(如 EXW/FOB/CIF 等)
-- ============================================================

CREATE TABLE IF NOT EXISTS `trade_term` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(20) NOT NULL COMMENT '贸易方式代码(如 EXW/FOB/CIF)',
  `name` VARCHAR(100) NOT NULL COMMENT '英文名称',
  `chinese_name` VARCHAR(100) NOT NULL COMMENT '中文名称',
  `transport_scope` VARCHAR(100) DEFAULT NULL COMMENT '适用运输方式(如 任何运输方式/仅海运)',
  `group_name` VARCHAR(50) DEFAULT NULL COMMENT '分组名称(如 E组/F组/C组/D组)',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='贸易方式(Incoterms)配置表';

-- 初始化数据
INSERT INTO `trade_term` (`code`, `name`, `chinese_name`, `transport_scope`, `group_name`, `sort`, `status`) VALUES
('EXW', 'Ex Works', '工厂交货', '任何运输方式', 'E组', 1, 1),
('FCA', 'Free Carrier', '货交承运人', '任何运输方式', 'F组', 2, 1),
('FAS', 'Free Alongside Ship', '装运港船边交货', '仅海运/内河', 'F组', 3, 1),
('FOB', 'Free On Board', '装运港船上交货', '仅海运/内河', 'F组', 4, 1),
('CFR', 'Cost and Freight', '成本加运费', '仅海运/内河', 'C组', 5, 1),
('CIF', 'Cost Insurance and Freight', '成本+保险+运费', '仅海运/内河', 'C组', 6, 1),
('CPT', 'Carriage Paid To', '运费付至', '任何运输方式', 'C组', 7, 1),
('CIP', 'Carriage and Insurance Paid To', '运费+保险付至', '任何运输方式', 'C组', 8, 1),
('DAP', 'Delivered At Place', '目的地交货', '任何运输方式', 'D组', 9, 1),
('DPU', 'Delivered At Place Unloaded', '卸货地交货', '任何运输方式', 'D组', 10, 1),
('DDP', 'Delivered Duty Paid', '完税后交货', '任何运输方式', 'D组', 11, 1);

-- 菜单：贸易方式管理（挂在系统管理下）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`)
VALUES (
  (SELECT id FROM (SELECT id FROM sys_menu WHERE menu_name = '系统管理' AND menu_type = 1 LIMIT 1) tmp),
  '贸易方式', 2, '/system/trade-term', 'system/trade-term/index', 'system:tradeterm:view', 'SwapOutlined', 20, 1
);

-- 按钮权限
SET @tradeTermMenuId = (SELECT id FROM sys_menu WHERE menu_name = '贸易方式' AND permission = 'system:tradeterm:view' LIMIT 1);

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_type`, `permission`, `sort`, `status`) VALUES
(@tradeTermMenuId, '查询', 3, 'system:tradeterm:view', 1, 1),
(@tradeTermMenuId, '新增', 3, 'system:tradeterm:create', 2, 1),
(@tradeTermMenuId, '修改', 3, 'system:tradeterm:update', 3, 1),
(@tradeTermMenuId, '删除', 3, 'system:tradeterm:delete', 4, 1);
