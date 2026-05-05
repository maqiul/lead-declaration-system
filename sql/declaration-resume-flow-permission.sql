-- ============================================================
-- 申报管理 · "恢复流程" 按钮专用权限
--   权限码: business:declaration:resume:flow
--   用途  : 将老 BPMN 中已结束但业务未走完的申报单一键迁移到新版流程对应节点
--   仅授权给超级管理员（role_id=1），业务操作员默认不可见，避免误操作
-- ============================================================

-- 1. 按钮菜单（父菜单 ID=202 申报管理页，沿用 invoice-flow-permission.sql 的段位）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (81015, '恢复流程', 'business-declaration-resume-flow', 202, 3, NULL, NULL, 'business:declaration:resume:flow', NULL, 55, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `status`     = 1,
  `deleted`    = 0;

-- 2. 仅授权给超级管理员（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (1, 81015);

-- 3. 验证
SELECT id, menu_name, permission, parent_id, status
FROM `sys_menu`
WHERE `permission` = 'business:declaration:resume:flow';

SELECT r.role_id, r.menu_id, m.permission
FROM `sys_role_menu` r
JOIN `sys_menu` m ON m.id = r.menu_id
WHERE m.permission = 'business:declaration:resume:flow';
