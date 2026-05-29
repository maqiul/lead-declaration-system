-- =============================================
-- 水单关联申报单权限配置SQL
-- 执行日期: 2026-04-21
-- 说明: 为水单管理添加关联申报单的权限
-- =============================================

-- 注意: 使用INSERT IGNORE避免覆盖已存在的菜单
-- 如果ID已存在,请先在数据库中查找可用的ID

-- 1. 添加水单关联申报单权限按钮 (ID: 6011)
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
VALUES 
(6011, 601, '水单关联申报单', 'remittance_relate_form', 3, NULL, NULL, 'business:remittance:relate-form', NULL, 4, 0);

-- 2. 添加申报单关联水单权限按钮 (ID: 1060,避免与首页菜单冲突)
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
VALUES 
(1060, 102, '关联水单', 'declaration_relate_remittance', 3, NULL, NULL, 'business:declaration:relateRemittance', NULL, 5, 0);

-- 3. 为管理员角色(ID=1)分配新权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) 
VALUES 
(1, 6011),  -- 水单关联申报单
(1, 1060);  -- 申报单关联水单

-- 4. 验证权限是否添加成功
SELECT 
  m.id,
  m.parent_id,
  m.menu_name,
  m.menu_code,
  m.permission,
  m.status
FROM sys_menu m
WHERE m.menu_code IN ('remittance_relate_form', 'declaration_relate_remittance')
ORDER BY m.id;

-- 5. 验证管理员角色是否有这些权限
SELECT 
  r.role_name,
  m.menu_name,
  m.permission
FROM sys_role r
INNER JOIN sys_role_menu rm ON r.role_id = rm.role_id
INNER JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.role_id = 1
  AND m.menu_code IN ('remittance_relate_form', 'declaration_relate_remittance');

