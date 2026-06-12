-- ============================================================
-- 申报管理 · "退回上一步" 按钮专用权限（申请 + 审核）
--   申请权限码: business:declaration:rollback
--   审核权限码: business:declaration:rollback:audit
--   用途  : 审核通过进入下一阶段后，申请退回到上一个审核节点重新审核
--   支持状态: 4→3、6→5、8→7
--   仅授权给超级管理员（role_id=1），业务操作员默认不可见
-- ============================================================

-- 1. 申请按钮菜单（父菜单 ID=202 申报管理页）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (81016, '退回上一步', 'business-declaration-rollback', 202, 3, NULL, NULL, 'business:declaration:rollback', NULL, 56, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `status`     = 1,
  `deleted`    = 0;

-- 2. 审核按钮菜单
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (81017, '审核退回上一步', 'business-declaration-rollback-audit', 202, 3, NULL, NULL, 'business:declaration:rollback:audit', NULL, 57, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `status`     = 1,
  `deleted`    = 0;

-- 3. 仅授权给超级管理员（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (1, 81016), (1, 81017);

-- 4. 验证
SELECT id, menu_name, permission, parent_id, status
FROM `sys_menu`
WHERE `permission` IN ('business:declaration:rollback', 'business:declaration:rollback:audit');

SELECT r.role_id, r.menu_id, m.permission
FROM `sys_role_menu` r
JOIN `sys_menu` m ON m.id = r.menu_id
WHERE m.permission IN ('business:declaration:rollback', 'business:declaration:rollback:audit');
