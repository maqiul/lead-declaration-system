-- ============================================================
-- 乙方配置功能（范式与"常用客户"完全一致）
--   1. 新建 party_b_config 表（用户级乙方/销货方信息）
--   2. declaration_form 增加 party_b_id 列（申报单关联乙方）
--   3. 新增顶级菜单"乙方配置"及 CRUD 权限按钮
--   4. 分配给所有角色
-- 说明：迁移按序号执行一次，ALTER 重复执行会报 Duplicate column，可忽略
-- ============================================================

SET NAMES utf8mb4;

-- 1. 新建 party_b_config 表
CREATE TABLE IF NOT EXISTS `party_b_config` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT       NOT NULL               COMMENT '所属用户ID',
  `party_b_name`    VARCHAR(200) NOT NULL               COMMENT '乙方公司名称',
  `party_b_address` VARCHAR(500) NOT NULL DEFAULT ''    COMMENT '乙方公司地址',
  `contact_person`  VARCHAR(100)          DEFAULT NULL  COMMENT '联系人',
  `contact_phone`   VARCHAR(50)           DEFAULT NULL  COMMENT '联系电话',
  `bank_name`       VARCHAR(200)          DEFAULT NULL  COMMENT '开户银行',
  `bank_account`    VARCHAR(100)          DEFAULT NULL  COMMENT '银行账号',
  `tax_id`          VARCHAR(100)          DEFAULT NULL  COMMENT '纳税人识别号',
  `sort`            INT          NOT NULL DEFAULT 0     COMMENT '排序',
  `status`          INT          NOT NULL DEFAULT 1     COMMENT '状态 0-禁用 1-启用',
  `create_time`     DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`       BIGINT                DEFAULT NULL  COMMENT '创建人',
  `update_by`       BIGINT                DEFAULT NULL  COMMENT '更新人',
  `del_flag`        INT          NOT NULL DEFAULT 0     COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='乙方配置表';

-- 2. 申报单关联乙方（为空=不填乙方，单证保持原有留空口径）
ALTER TABLE `declaration_form`
  ADD COLUMN `party_b_id` BIGINT DEFAULT NULL COMMENT '乙方配置ID（关联 party_b_config.id）' AFTER `entity_id`;

-- 3. 顶级菜单：乙方配置（id=1300, parent_id=0）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
VALUES
  (1300, '乙方配置', 'party-b-config', 0, 1, '/party-b', 'Layout',
   NULL, 'ShopOutlined', 9, 0, 0, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `component`  = VALUES(`component`),
  `status`     = 1,
  `deleted`    = 0;

-- 4. 子菜单（页面）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
VALUES
  (1301, '乙方管理', 'party-b-config-list', 1300, 2, 'index',
   '@/views/party-b/index.vue', NULL, 'TeamOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `component`  = VALUES(`component`),
  `status`     = 1,
  `deleted`    = 0;

-- 5. 权限按钮（parent_id=1301，ID 1311-1314）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
VALUES
  (1311, '乙方查看', 'party-b-config-view',   1301, 3, NULL, NULL, 'party-b:config:view',   NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
  (1312, '乙方新增', 'party-b-config-add',    1301, 3, NULL, NULL, 'party-b:config:add',    NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
  (1313, '乙方编辑', 'party-b-config-update', 1301, 3, NULL, NULL, 'party-b:config:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
  (1314, '乙方删除', 'party-b-config-delete', 1301, 3, NULL, NULL, 'party-b:config:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `status`     = 1,
  `deleted`    = 0;

-- 6. 将菜单分配给所有已有角色（动态获取所有角色）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1300 FROM `sys_role` r WHERE r.deleted = 0;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1301 FROM `sys_role` r WHERE r.deleted = 0;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1311 FROM `sys_role` r WHERE r.deleted = 0;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1312 FROM `sys_role` r WHERE r.deleted = 0;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1313 FROM `sys_role` r WHERE r.deleted = 0;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 1314 FROM `sys_role` r WHERE r.deleted = 0;

-- 7. 验证
SELECT id, menu_name, permission, parent_id, status
FROM `sys_menu`
WHERE `id` IN (1300, 1301, 1311, 1312, 1313, 1314);
