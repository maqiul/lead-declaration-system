-- 70-supplement-initiate-grant.sql
-- 修复：「发起资料补交」权限点（menu_id=81083, business:declaration:supplement:initiate）
-- 此前仅授权给超级管理员（见 65）与 SUPPLEMENT_AUDITOR（见 69），
-- 导致普通申报角色在资料区看不到「发起资料补交」按钮。
-- 补丁：凡拥有「提交资料」权限（menu_id IN 81010/2032，permission=business:declaration:material:submit）
-- 的角色同步授予发起补交权限——能提交资料者理应可发起补交。

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 81083
FROM `sys_role_menu` rm
WHERE rm.menu_id IN (81010, 2032);
