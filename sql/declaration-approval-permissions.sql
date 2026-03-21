-- 为申报管理添加审批权限配置
-- 包括各种状态下的审批按钮权限

-- 获取申报管理菜单ID
SET @declaration_manage_id = (SELECT id FROM sys_menu WHERE menu_code = 'declaration_manage' AND menu_type = 2);

-- 1. 添加申报管理审批相关权限按钮
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    @declaration_manage_id, 
    '申报初审', 'declaration_manage_first_audit', 3, '', '', 'declaration:manage:first-audit', '', 6, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage_first_audit');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    @declaration_manage_id, 
    '定金审核', 'declaration_manage_deposit_audit', 3, '', '', 'declaration:manage:deposit-audit', '', 7, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage_deposit_audit');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    @declaration_manage_id, 
    '尾款审核', 'declaration_manage_balance_audit', 3, '', '', 'declaration:manage:balance-audit', '', 8, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage_balance_audit');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    @declaration_manage_id, 
    '提货单审核', 'declaration_manage_pickup_audit', 3, '', '', 'declaration:manage:pickup-audit', '', 9, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage_pickup_audit');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    @declaration_manage_id, 
    '申报审批', 'declaration_manage_approval', 3, '', '', 'declaration:manage:approval', '', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage_approval');

-- 2. 为管理员角色分配审批权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE menu_code IN (
    'declaration_manage_first_audit',
    'declaration_manage_deposit_audit', 
    'declaration_manage_balance_audit',
    'declaration_manage_pickup_audit',
    'declaration_manage_approval'
)
ON DUPLICATE KEY UPDATE create_time = NOW();

-- 3. 为财务角色分配相关审批权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 
    (SELECT id FROM sys_role WHERE role_code = 'finance'), 
    id, 
    NOW()
FROM sys_menu 
WHERE menu_code IN (
    'declaration_manage_deposit_audit',
    'declaration_manage_balance_audit',
    'declaration_manage_pickup_audit'
)
ON DUPLICATE KEY UPDATE create_time = NOW();

-- 4. 验证权限配置
SELECT 
    '申报管理审批权限配置' as config_section,
    m.menu_name,
    m.menu_code,
    m.permission,
    m.sort,
    CASE 
        WHEN rm_admin.menu_id IS NOT NULL THEN '✓ 管理员有权限'
        ELSE '✗ 管理员无权限'
    END as admin_permission,
    CASE 
        WHEN rm_finance.menu_id IS NOT NULL THEN '✓ 财务有权限'
        ELSE '✗ 财务无权限'
    END as finance_permission
FROM sys_menu m
LEFT JOIN sys_role_menu rm_admin ON m.id = rm_admin.menu_id AND rm_admin.role_id = 1
LEFT JOIN sys_role_menu rm_finance ON m.id = rm_finance.menu_id AND rm_finance.role_id = (SELECT id FROM sys_role WHERE role_code = 'finance')
WHERE m.menu_code LIKE 'declaration_manage_%audit' 
   OR m.menu_code = 'declaration_manage_approval'
ORDER BY m.sort;

-- 5. 显示申报管理完整的权限配置
SELECT 
    '申报管理完整权限配置' as summary,
    COUNT(*) as total_permissions,
    SUM(CASE WHEN rm.role_id = 1 THEN 1 ELSE 0 END) as admin_permissions,
    SUM(CASE WHEN r.role_code = 'finance' THEN 1 ELSE 0 END) as finance_permissions
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id
LEFT JOIN sys_role r ON rm.role_id = r.id
WHERE m.menu_code LIKE 'declaration_manage%'
   OR m.parent_id = @declaration_manage_id;