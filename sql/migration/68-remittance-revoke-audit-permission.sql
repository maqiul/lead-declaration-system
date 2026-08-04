-- ============================================================
-- 68-remittance-revoke-audit-permission.sql
-- 水单/出款水单反审核独立权限点
-- 原先反审核与审核共用 audit 权限点，拆分为独立权限点单独控制
-- ============================================================

-- 1. 注册权限点（水单按钮权限段位 6011-6019，6017 空闲）
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
                        `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
                        `create_time`, `update_time`)
VALUES (6017, '水单反审核', 'remittance-revoke-audit', 601, 3, NULL, NULL,
        'business:remittance:revoke-audit', NULL, 7, 0, 0, 1, 1, 0, NOW(), NOW());

-- 2. 授权：超级管理员
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 6017);

-- 3. 授权：已拥有"水单审核"(6016) 权限点的角色同步获得反审核权限，保证存量用户能力不中断
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 6017
FROM `sys_role_menu` rm
WHERE rm.menu_id = 6016;

-- ---------- 出款水单反审核（business:payment-remittance:revoke-audit） ----------
-- 4. 注册权限点（出款水单按钮权限段位 810-815，816 空闲）
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
                        `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
                        `create_time`, `update_time`)
VALUES (816, '反审核出款水单', 'payment-remittance-revoke-audit', 800, 3, '', NULL,
        'business:payment-remittance:revoke-audit', NULL, 16, 0, 0, 1, 1, 0, NOW(), NOW());

-- 5. 授权：超级管理员
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 816);

-- 6. 授权：已拥有"审核出款水单"(814) 权限点的角色同步获得反审核权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 816
FROM `sys_role_menu` rm
WHERE rm.menu_id = 814;
