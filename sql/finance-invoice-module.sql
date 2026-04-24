-- ========================================

-- 财务发票台账模块 - 数据库初始化脚本
-- 说明: 包含表结构创建、菜单配置及权限分配
-- 执行日期: 2026-04-23
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 第一步：创建发票记录表
-- ========================================
-- 该表同时支持"业务上传的发票(category=1)"和"财务独立录入的发票(category=2)"
CREATE TABLE IF NOT EXISTS `declaration_invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` bigint NOT NULL COMMENT '关联申报单ID (必填)',
  `category` tinyint NOT NULL DEFAULT '1' COMMENT '发票大类: 1-业务发票(随申报上传), 2-财务留底(独立台账)',
  `invoice_type` tinyint NOT NULL DEFAULT '1' COMMENT '发票类型: 1-进项发票, 2-出项发票',
  `invoice_no` varchar(100) DEFAULT NULL COMMENT '发票号码',
  `invoice_name` varchar(200) DEFAULT NULL COMMENT '发票名称',
  `invoice_date` date DEFAULT NULL COMMENT '开票日期',
  `amount` decimal(18, 2) DEFAULT '0.00' COMMENT '不含税金额',
  `tax_amount` decimal(18, 2) DEFAULT '0.00' COMMENT '税额',
  `total_amount` decimal(18, 2) DEFAULT '0.00' COMMENT '价税合计',
  `tax_rate` decimal(5, 2) DEFAULT '0.00' COMMENT '税率(%)',
  `buyer_name` varchar(200) DEFAULT NULL COMMENT '购方名称',
  `seller_name` varchar(200) DEFAULT NULL COMMENT '销方名称',
  `file_url` varchar(500) DEFAULT NULL COMMENT '发票文件URL',
  `file_name` varchar(200) DEFAULT NULL COMMENT '发票文件名',
  `remarks` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除标识: 0-正常, 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_category` (`category`),
  KEY `idx_invoice_no` (`invoice_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='发票记录表 (业务/财务共用)';

-- ========================================
-- 第二步：插入菜单与权限配置
-- ========================================
-- 说明: ID 从 700 开始，请确保这些 ID 在你的数据库中没有被占用

-- 1. 一级菜单：发票台账 (放在侧边栏，Type=1 代表目录)
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_show`, `status`, `deleted`, `create_time`, `update_time`) VALUES
(700, '发票管理', 'finance-invoice', 0, 1, '/finance-invoice', 'Layout', NULL, 'FileTextOutlined', 9, 1, 1, 0, NOW(), NOW());

-- 2. 二级菜单：发票台账列表页 (Type=2 代表页面)
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_show`, `status`, `deleted`, `create_time`, `update_time`) VALUES
(701, '发票台账', 'finance-invoice-index', 700, 2, 'index', '@/views/finance/invoice/index.vue', NULL, 'FileTextOutlined', 1, 1, 1, 0, NOW(), NOW());

-- 3. 按钮权限 (Type=3 代表按钮/接口权限)
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_show`, `status`, `deleted`, `create_time`, `update_time`) VALUES
(7011, '查看发票', 'finance-invoice-view',   701, 3, NULL, NULL, 'finance:invoice:view',   NULL, 1, 1, 1, 0, NOW(), NOW()),
(7012, '录入发票', 'finance-invoice-create',  701, 3, NULL, NULL, 'finance:invoice:create',  NULL, 2, 1, 1, 0, NOW(), NOW()),
(7013, '编辑发票', 'finance-invoice-update',   701, 3, NULL, NULL, 'finance:invoice:update',   NULL, 3, 1, 1, 0, NOW(), NOW()),
(7014, '删除发票', 'finance-invoice-delete', 701, 3, NULL, NULL, 'finance:invoice:delete', NULL, 4, 1, 1, 0, NOW(), NOW());

-- ========================================
-- 第三步：角色权限分配
-- ========================================

-- 1. 分配给管理员 (假设 Role ID = 1)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`) 
SELECT 1, id, NOW() FROM `sys_menu` WHERE id IN (700, 701, 7011, 7012, 7013, 7014);

-- 2. 分配给财务经理 (假设 Role ID = 4)
-- 请根据你实际的数据库表结构调整 Role ID
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`) 
SELECT 4, id, NOW() FROM `sys_menu` WHERE id IN (700, 701, 7011, 7012, 7013, 7014);

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 验证
-- ========================================
SELECT '发票模块 SQL 执行完成！请检查 sys_menu 表是否新增了 700-7014 的记录。' AS message;
