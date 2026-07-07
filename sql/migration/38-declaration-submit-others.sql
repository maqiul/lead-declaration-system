-- ============================================================
-- 新增权限：代提交申报单（允许提交非本人创建的申报单）
-- ============================================================

-- 1. 新增菜单/权限按钮
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`, `create_by`, `update_by`)
VALUES
  (2041, '代提交申报单', 'declaration-submit-others', 201, 3, NULL, NULL,
   'business:declaration:submit:others', NULL, 31, 0, 0, 1, 1, 0,
   NOW(), NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 2. 将新权限分配给超级管理员（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2041);
