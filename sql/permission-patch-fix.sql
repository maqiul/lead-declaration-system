-- ========================================
-- 权限配置补丁修复脚本
-- 日期: 2026-03-20
-- 说明: 修复权限标识不一致、添加缺失权限、完善角色分配
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 第一步：修复银行账户权限标识
-- ========================================
-- 将 query 改为 list（与后端Controller一致）
UPDATE sys_menu SET permission='system:bank-account:list', menu_name='银行账户列表' WHERE id=1051;
-- 添加缺失的 view 权限
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1055, '银行账户查看', 'bank-account-view', 105, 3, NULL, NULL, 'system:bank-account:view', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW());

-- ========================================
-- 第二步：修复国家信息权限标识
-- ========================================
-- query 改为 list，update 改为 edit（与后端Controller一致）
UPDATE sys_menu SET permission='system:country:list', menu_name='国家信息列表' WHERE id=1061;
UPDATE sys_menu SET permission='system:country:edit', menu_name='国家信息编辑' WHERE id=1063;
-- 添加缺失的 view 权限
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1065, '国家信息查看', 'country-view', 106, 3, NULL, NULL, 'system:country:view', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW());

-- ========================================
-- 第三步：添加缺失的工作流按钮权限
-- ========================================
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(4011, '流程部署', 'workflow-definition-deploy', 401, 3, NULL, NULL, 'workflow:definition:deploy', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(4012, '流程更新', 'workflow-definition-update', 401, 3, NULL, NULL, 'workflow:definition:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(4041, '流程实例终止', 'workflow-instance-terminate', 404, 3, NULL, NULL, 'workflow:instance:terminate', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW());

-- ========================================
-- 第四步：添加缺失的合同生成权限
-- ========================================
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(5016, '合同生成', 'contract-generate', 501, 3, NULL, NULL, 'business:contract:generate', NULL, 6, 0, 0, 1, 1, 0, NOW(), NOW());

-- ========================================
-- 第五步：为部门管理员(role_id=3)分配权限
-- ========================================
INSERT INTO sys_role_menu (role_id, menu_id, create_time) VALUES
-- 首页
(3, 1, NOW()),
(3, 2, NOW()),
-- 系统管理-组织管理
(3, 100, NOW()),
(3, 103, NOW()),
(3, 1031, NOW()),
(3, 1032, NOW()),
(3, 1033, NOW()),
(3, 1034, NOW()),
(3, 1035, NOW()),
(3, 1036, NOW()),
-- 出口申报（查看/编辑/提交/审核）
(3, 200, NOW()),
(3, 201, NOW()),
(3, 2011, NOW()),
(3, 2013, NOW()),
(3, 2015, NOW()),
(3, 2016, NOW()),
(3, 2017, NOW()),
(3, 2018, NOW());

-- ========================================
-- 第六步：为普通用户(role_id=2)补充申报统计权限
-- ========================================
INSERT INTO sys_role_menu (role_id, menu_id, create_time) VALUES
(2, 204, NOW());

-- ========================================
-- 第七步：重建管理员权限（确保管理员拥有所有新增权限）
-- ========================================
DELETE FROM sys_role_menu WHERE role_id = 1;
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW() FROM sys_menu WHERE deleted = 0 AND status = 1;

-- ========================================
-- 第八步：更新财务角色权限（添加新增的权限）
-- ========================================
-- 添加银行账户view、国家信息view/list/edit、合同生成
INSERT INTO sys_role_menu (role_id, menu_id, create_time) VALUES
(4, 1055, NOW()),
(4, 1065, NOW()),
(4, 5016, NOW());

-- ========================================
-- 第九步：更新销售员角色权限
-- ========================================
INSERT INTO sys_role_menu (role_id, menu_id, create_time) VALUES
(5, 5016, NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 验证
-- ========================================
SELECT '权限补丁修复完成!' AS message;
SELECT COUNT(*) AS total_menus FROM sys_menu WHERE deleted = 0;
SELECT menu_type, CASE menu_type WHEN 1 THEN '目录' WHEN 2 THEN '菜单' WHEN 3 THEN '按钮' END AS type_name, COUNT(*) AS count FROM sys_menu WHERE deleted = 0 GROUP BY menu_type ORDER BY menu_type;
SELECT r.role_name, COUNT(rm.id) AS permission_count FROM sys_role r LEFT JOIN sys_role_menu rm ON r.id = rm.role_id GROUP BY r.id, r.role_name ORDER BY r.id;
