-- =============================================
-- 申报管理权限优化SQL
-- 执行日期: 2026-04-21
-- 说明: 将审批权限独立出来，重新规划权限结构
-- =============================================

-- =============================================
-- 第一部分: 删除旧的权限菜单（如果有）
-- =============================================

-- =============================================
-- 第二部分: 添加新的权限菜单
-- =============================================

-- 1. 查看权限组 (ID: 1001-1010)
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
VALUES 
(1001, 102, '查看申报单', 'declaration_view', 3, NULL, NULL, 'business:declaration:view', NULL, 1, 0),
(1002, 102, '导出申报单', 'declaration_export', 3, NULL, NULL, 'business:declaration:export', NULL, 10, 0)
ON DUPLICATE KEY UPDATE 
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `update_time` = NOW();

-- 2. 操作权限组 (ID: 1011-1020)
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
VALUES 
(1011, 102, '创建申报单', 'declaration_create', 3, NULL, NULL, 'business:declaration:create', NULL, 2, 0),
(1012, 102, '编辑申报单', 'declaration_edit', 3, NULL, NULL, 'business:declaration:edit', NULL, 3, 0),
(1013, 102, '删除申报单', 'declaration_delete', 3, NULL, NULL, 'business:declaration:delete', NULL, 4, 0),
(1014, 102, '提交申报单', 'declaration_submit', 3, NULL, NULL, 'business:declaration:submit', NULL, 5, 0)
ON DUPLICATE KEY UPDATE 
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `update_time` = NOW();

-- 3. 审批权限组 - 独立分组 (ID: 1031-1040)
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
VALUES 
(1031, 102, '部门初审', 'declaration_audit_initial', 3, NULL, NULL, 'business:declaration:audit:initial', NULL, 6, 0),
(1032, 102, '财务审核', 'declaration_audit_finance', 3, NULL, NULL, 'business:declaration:audit:finance', NULL, 7, 0),
(1033, 102, '审核退回申请', 'declaration_audit_return', 3, NULL, NULL, 'business:declaration:audit:return', NULL, 8, 0),
(1034, 102, '审核提货单', 'declaration_audit_delivery', 3, NULL, NULL, 'business:declaration:audit:delivery', NULL, 9, 0)
ON DUPLICATE KEY UPDATE 
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `update_time` = NOW();

-- 4. 财务权限组 - 独立分组 (ID: 1041-1050)
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
VALUES 
(1041, 102, '财务补充材料', 'declaration_finance_supplement', 3, NULL, NULL, 'business:declaration:finance:supplement', NULL, 11, 0),
(1042, 102, '关联水单', 'declaration_finance_remittance', 3, NULL, NULL, 'business:declaration:finance:remittance', NULL, 12, 0)
ON DUPLICATE KEY UPDATE 
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `update_time` = NOW();

-- 5. 退货权限组 (ID: 1051-1055)
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
VALUES 
(1051, 102, '申请退回草稿', 'declaration_return_apply', 3, NULL, NULL, 'business:declaration:return:apply', NULL, 13, 0)
ON DUPLICATE KEY UPDATE 
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `update_time` = NOW();

-- 6. 提货单权限组 (ID: 1061-1070)
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
VALUES 
(1061, 102, '查看提货单', 'declaration_delivery_view', 3, NULL, NULL, 'business:declaration:delivery:view', NULL, 14, 0),
(1062, 102, '创建提货单', 'declaration_delivery_create', 3, NULL, NULL, 'business:declaration:delivery:create', NULL, 15, 0),
(1063, 102, '编辑提货单', 'declaration_delivery_edit', 3, NULL, NULL, 'business:declaration:delivery:edit', NULL, 16, 0),
(1064, 102, '删除提货单', 'declaration_delivery_delete', 3, NULL, NULL, 'business:declaration:delivery:delete', NULL, 17, 0)
ON DUPLICATE KEY UPDATE 
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `update_time` = NOW();

-- =============================================
-- 第三部分: 为角色分配权限
-- =============================================

-- 1. 业务员角色 (假设role_id=2)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) 
VALUES 
-- 查看权限
(2, 1001),  -- 查看申报单
(2, 1002),  -- 导出申报单
-- 操作权限
(2, 1011),  -- 创建申报单
(2, 1012),  -- 编辑申报单
(2, 1013),  -- 删除申报单
(2, 1014),  -- 提交申报单
-- 提货单权限
(2, 1061),  -- 查看提货单
(2, 1062),  -- 创建提货单
(2, 1063),  -- 编辑提货单
(2, 1064),  -- 删除提货单
-- 退货权限
(2, 1051);  -- 申请退回草稿

-- 2. 部门经理角色 (假设role_id=3)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) 
VALUES 
-- 查看权限
(3, 1001),  -- 查看申报单
(3, 1002),  -- 导出申报单
-- 审批权限
(3, 1031),  -- 部门初审
(3, 1033);  -- 审核退回申请

-- 3. 财务角色 (假设role_id=4)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) 
VALUES 
-- 查看权限
(4, 1001),  -- 查看申报单
(4, 1002),  -- 导出申报单
-- 财务权限
(4, 1041),  -- 财务补充材料
(4, 1042),  -- 关联水单
-- 审批权限
(4, 1032),  -- 财务审核
(4, 1033),  -- 审核退回申请
(4, 1034),  -- 审核提货单
-- 提货单权限
(4, 1061);  -- 查看提货单

-- 4. 管理员角色 (role_id=1) - 分配所有权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) 
VALUES 
(1, 1001), (1, 1002), (1, 1011), (1, 1012), (1, 1013), (1, 1014),
(1, 1031), (1, 1032), (1, 1033), (1, 1034),
(1, 1041), (1, 1042),
(1, 1051),
(1, 1061), (1, 1062), (1, 1063), (1, 1064);

-- =============================================
-- 第四部分: 验证权限配置
-- =============================================

-- 1. 查看所有新增的权限菜单
SELECT 
  id,
  parent_id,
  menu_name,
  menu_code,
  permission,
  status
FROM sys_menu
WHERE id IN (1001, 1002, 1011, 1012, 1013, 1014, 1031, 1032, 1033, 1034, 1041, 1042, 1051, 1061, 1062, 1063, 1064)
ORDER BY id;

-- 2. 查看业务员角色的权限
SELECT 
  r.role_name,
  m.menu_name,
  m.permission
FROM sys_role r
INNER JOIN sys_role_menu rm ON r.role_id = rm.role_id
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE r.role_id = 2
ORDER BY m.id;

-- 3. 查看部门经理角色的权限
SELECT 
  r.role_name,
  m.menu_name,
  m.permission
FROM sys_role r
INNER JOIN sys_role_menu rm ON r.role_id = rm.role_id
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE r.role_id = 3
ORDER BY m.id;

-- 4. 查看财务角色的权限
SELECT 
  r.role_name,
  m.menu_name,
  m.permission
FROM sys_role r
INNER JOIN sys_role_menu rm ON r.role_id = rm.role_id
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE r.role_id = 4
ORDER BY m.id;

-- 5. 统计每个角色的权限数量
SELECT 
  r.role_name,
  COUNT(rm.menu_id) AS permission_count
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.role_id = rm.role_id
WHERE r.role_id IN (1, 2, 3, 4)
GROUP BY r.role_id, r.role_name
ORDER BY r.role_id;
