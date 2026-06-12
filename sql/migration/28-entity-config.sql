-- ============================================================
-- 多主体配置功能
--   1. 新建 entity_config 表（公司主体 + 模板路径配置）
--   2. bank_account_config 新增 entity_id 字段（关联主体）
--   3. declaration_form 新增 entity_id 字段（关联主体）
--   4. 系统管理菜单下新增"主体配置"菜单及 CRUD 权限
-- ============================================================

-- 1. 新建 entity_config 表
CREATE TABLE IF NOT EXISTS `entity_config` (
  `id`                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `entity_name`              VARCHAR(200) NOT NULL               COMMENT '公司英文名（如 NINGBO ZIYI TECHNOLOGY CO.,LTD）',
  `entity_address`           VARCHAR(500) NOT NULL DEFAULT ''    COMMENT '英文地址',
  `entity_name_cn`           VARCHAR(200) NOT NULL DEFAULT ''    COMMENT '公司中文名',
  `entity_address_cn`        VARCHAR(500) NOT NULL DEFAULT ''    COMMENT '中文地址',
  `invoice_template`         VARCHAR(500)          DEFAULT NULL  COMMENT '发票模板文件名（空=用系统默认）',
  `packing_list_template`    VARCHAR(500)          DEFAULT NULL  COMMENT '装箱单模板文件名',
  `full_documents_template`  VARCHAR(500)          DEFAULT NULL  COMMENT '海关附件模板文件名',
  `pickup_list_template`     VARCHAR(500)          DEFAULT NULL  COMMENT '提货单模板文件名',
  `remittance_template`      VARCHAR(500)          DEFAULT NULL  COMMENT '水单模板文件名',
  `is_default`               INT          NOT NULL DEFAULT 0     COMMENT '是否默认主体 0-否 1-是',
  `status`                   INT          NOT NULL DEFAULT 1     COMMENT '状态 0-禁用 1-启用',
  `sort`                     INT          NOT NULL DEFAULT 0     COMMENT '排序',
  `create_time`              DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`              DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`                BIGINT                DEFAULT NULL  COMMENT '创建人',
  `update_by`                BIGINT                DEFAULT NULL  COMMENT '更新人',
  `del_flag`                 INT          NOT NULL DEFAULT 0     COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主体配置表';

-- 2. bank_account_config 新增 entity_id
ALTER TABLE `bank_account_config`
  ADD COLUMN `entity_id` BIGINT DEFAULT NULL COMMENT '所属主体ID（关联 entity_config.id）' AFTER `id`;

-- 3. declaration_form 新增 entity_id
ALTER TABLE `declaration_form`
  ADD COLUMN `entity_id` BIGINT DEFAULT NULL COMMENT '所属主体ID（关联 entity_config.id）' AFTER `id`;

-- 4. 菜单：在系统管理（parent_id=100）下新增"主体配置"菜单（id=116）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (116, '主体配置', 'system-entity-config', 100, 2, 'entity-config',
   '@/views/system/entity-config/index.vue', NULL, 'ShopOutlined', 16, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `component`  = VALUES(`component`),
  `status`     = 1,
  `deleted`    = 0;

-- 5. 权限按钮（parent_id=116，段位 1161-1169）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (1161, '主体查看', 'entity-config-view',   116, 3, NULL, NULL, 'system:entity-config:view',   NULL, 1, 0, 0, 1, 1, 0),
  (1162, '主体新增', 'entity-config-add',    116, 3, NULL, NULL, 'system:entity-config:add',    NULL, 2, 0, 0, 1, 1, 0),
  (1163, '主体编辑', 'entity-config-update', 116, 3, NULL, NULL, 'system:entity-config:update', NULL, 3, 0, 0, 1, 1, 0),
  (1164, '主体删除', 'entity-config-delete', 116, 3, NULL, NULL, 'system:entity-config:delete', NULL, 4, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `status`     = 1,
  `deleted`    = 0;

-- 6. 将新菜单分配给超级管理员（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (1, 116), (1, 1161), (1, 1162), (1, 1163), (1, 1164);

-- 7. 验证
SELECT id, menu_name, permission, parent_id, status
FROM `sys_menu`
WHERE `id` IN (116, 1161, 1162, 1163, 1164);
