-- ============================================================
-- 为普通申报角色补全开票金额提交权限（menu_id=2039）
-- 适用：已执行 26 但申报员角色仅有资料/补充资料权限、无 2039 的情况
-- ============================================================

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 2039
FROM `sys_role_menu` rm
WHERE rm.menu_id IN (2032, 2037, 209);
