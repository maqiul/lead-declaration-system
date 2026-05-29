-- =============================================
-- 申报管理菜单拆分SQL
-- 执行日期: 2026-05-11
-- 说明: 将"申报管理"拆分为4个独立菜单：申报录入/资料提交/发票提交/归档查询
-- =============================================

-- 1. 隐藏旧的"申报管理"菜单
UPDATE `sys_menu` SET `is_show` = 0, `update_time` = NOW() WHERE `id` = 201;

-- 2. 新增4个子菜单（parent_id=200 即"出口申报"）
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`, `create_time`, `update_time`, `create_by`, `update_by`)
VALUES
(205, '申报录入', 'declaration-entry', 200, 2, 'entry', '@/views/declaration/entry/index.vue', NULL, 'EditOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL),
(206, '资料提交', 'declaration-material', 200, 2, 'material', '@/views/declaration/material/index.vue', NULL, 'UploadOutlined', 2, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL),
(207, '发票提交', 'declaration-invoice', 200, 2, 'invoice', '@/views/declaration/invoice/index.vue', NULL, 'FileTextOutlined', 3, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL),
(208, '归档查询', 'declaration-archive', 200, 2, 'archive', '@/views/declaration/archive/index.vue', NULL, 'FolderOpenOutlined', 4, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`),
  `is_show` = VALUES(`is_show`),
  `update_time` = NOW();

-- 3. 将新菜单权限分配给管理员角色（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 205), (1, 206), (1, 207), (1, 208);

-- 4. 如果存在其他角色拥有旧菜单(201)的权限，也赋予新菜单权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 205 FROM `sys_role_menu` rm WHERE rm.menu_id = 201;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 206 FROM `sys_role_menu` rm WHERE rm.menu_id = 201;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 207 FROM `sys_role_menu` rm WHERE rm.menu_id = 201;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 208 FROM `sys_role_menu` rm WHERE rm.menu_id = 201;
