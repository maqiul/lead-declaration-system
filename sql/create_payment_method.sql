-- 支付方式配置表与菜单 SQL 脚本

-- 1. 创建支付方式表
DROP TABLE IF EXISTS `payment_method`;
CREATE TABLE `payment_method` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '支付方式名称(英文)',
  `chinese_name` varchar(100) DEFAULT NULL COMMENT '支付方式名称(中文)',
  `code` varchar(50) DEFAULT NULL COMMENT '支付方式代码',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付方式配置表';

-- 2. 插入默认数据
INSERT INTO `payment_method` (`name`, `chinese_name`, `code`, `description`, `sort`) VALUES 
('Telegraphic Transfer', '电汇', 'T/T', '最常用的国际贸易支付方式', 1),
('Letter of Credit', '信用证', 'L/C', '银行担保的支付方式', 2),
('Documents against Payment', '付款交单', 'D/P', '买方付款后才能拿到单据', 3),
('Documents against Acceptance', '承兑交单', 'D/A', '买方承兑后即可拿到单据', 4),
('Open Account', '赊销', 'O/A', '先发货后收款', 5),
('Western Union', '西联汇款', 'Western Union', '快速个人汇款服务', 6),
('PayPal', '贝宝', 'PayPal', '在线支付平台', 7),
('MoneyGram', '速汇金', 'MoneyGram', '国际快速汇款', 8),
('Cash', '现金', 'Cash', '现金支付', 9),
('Other', '其他', 'Other', '其他支付方式', 10);

-- 3. 添加对应的系统管理菜单
SET @system_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'system' AND menu_type = 1);

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT @system_parent_id, '支付方式', 'system_payment_method', 2, 'payment-method', 'system/payment-method/index', 'system:payment:list', 'MoneyCollectOutlined', 9, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_payment_method');

-- 4. 插入各项操作按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_payment_method'), 
    '列表查看', 'system_payment_method_list', 3, '', '', 'system:payment:list', '', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_payment_method_list');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_payment_method'), 
    '详情查看', 'system_payment_method_view', 3, '', '', 'system:payment:view', '', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_payment_method_view');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_payment_method'), 
    '支付方式新增', 'system_payment_method_add', 3, '', '', 'system:payment:add', '', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_payment_method_add');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_payment_method'), 
    '支付方式编辑', 'system_payment_method_edit', 3, '', '', 'system:payment:edit', '', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_payment_method_edit');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_payment_method'), 
    '支付方式删除', 'system_payment_method_delete', 3, '', '', 'system:payment:delete', '', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_payment_method_delete');

-- 5. 为管理员角色分配权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE menu_code IN ('system_payment_method')
   OR menu_code LIKE 'system_payment_method_%'
   OR parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code = 'system_payment_method'
   )
ON DUPLICATE KEY UPDATE create_time = NOW();
