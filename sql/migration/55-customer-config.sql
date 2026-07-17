-- ============================================================
-- 常用客户功能
--   1. 新建 customer_config 表（用户级常用收货人信息）
--   2. 新增顶级菜单"常用客户"及 CRUD 权限按钮
--   3. 分配给所有角色
-- ============================================================

SET NAMES utf8mb4;

-- 1. 新建 customer_config 表
CREATE TABLE IF NOT EXISTS `customer_config` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`               BIGINT       NOT NULL               COMMENT '所属用户ID',
  `customer_name`         VARCHAR(200) NOT NULL               COMMENT '收货人公司名',
  `customer_address`      VARCHAR(500) NOT NULL DEFAULT ''    COMMENT '收货人地址',
  `destination_country`   VARCHAR(100)          DEFAULT NULL  COMMENT '目的国',
  `trade_country`         VARCHAR(100)          DEFAULT NULL  COMMENT '贸易国',
  `sort`                  INT          NOT NULL DEFAULT 0     COMMENT '排序',
  `status`                INT          NOT NULL DEFAULT 1     COMMENT '状态 0-禁用 1-启用',
  `create_time`           DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`           DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`             BIGINT                DEFAULT NULL  COMMENT '创建人',
  `update_by`             BIGINT                DEFAULT NULL  COMMENT '更新人',
  `del_flag`              INT          NOT NULL DEFAULT 0     COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='常用客户配置表';

-- 2. 顶级菜单：常用客户（id=1200, parent_id=0）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
VALUES
  (1200, '常用客户', 'customer-config', 0, 1, '/customer', 'Layout',
   NULL, 'UserOutlined', 10, 0, 0, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `component`  = VALUES(`component`),
  `status`     = 1,
  `deleted`    = 0;

-- 3. 子菜单（页面）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
VALUES
  (1201, '客户管理', 'customer-config-list', 1200, 2, 'index',
   '@/views/customer/index.vue', NULL, 'TeamOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `component`  = VALUES(`component`),
  `status`     = 1,
  `deleted`    = 0;

-- 4. 权限按钮（parent_id=1201，ID 1211-1214）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
VALUES
  (1211, '客户查看', 'customer-config-view',   1201, 3, NULL, NULL, 'customer:config:view',   NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
  (1212, '客户新增', 'customer-config-add',    1201, 3, NULL, NULL, 'customer:config:add',    NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
  (1213, '客户编辑', 'customer-config-update', 1201, 3, NULL, NULL, 'customer:config:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
  (1214, '客户删除', 'customer-config-delete', 1201, 3, NULL, NULL, 'customer:config:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `status`     = 1,
  `deleted`    = 0;

-- 5. 将菜单分配给所有已有角色（动态获取所有角色）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1200 FROM `sys_role` r WHERE r.deleted = 0;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1201 FROM `sys_role` r WHERE r.deleted = 0;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1211 FROM `sys_role` r WHERE r.deleted = 0;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1212 FROM `sys_role` r WHERE r.deleted = 0;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1213 FROM `sys_role` r WHERE r.deleted = 0;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1214 FROM `sys_role` r WHERE r.deleted = 0;

-- 6. 验证
SELECT id, menu_name, permission, parent_id, status
FROM `sys_menu`
WHERE `id` IN (1200, 1201, 1211, 1212, 1213, 1214);
