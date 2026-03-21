-- 完整菜单配置更新脚本
-- 包含银行账户、国家信息、税务退费等所有新增功能的菜单配置

-- 1. 清理旧的菜单配置
DELETE FROM sys_menu WHERE menu_code IN ('business_tax_refund', 'business_tax_refund_apply', 'business_tax_refund_list', 'business_tax_refund_detail', 'business_tax_refund_finance');

-- 2. 获取系统管理根菜单ID
SET @system_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'system' AND menu_type = 1);

-- 3. 添加银行账户菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT @system_parent_id, '银行账户', 'system_bank_account', 2, '/system/bank-account', 'system/bank-account/index', 'system:bank-account:view', 'BankOutlined', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_bank_account');

-- 4. 添加国家信息菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT @system_parent_id, '国家信息', 'system_country', 2, '/system/country', 'system/country/index', 'system:country:view', 'GlobalOutlined', 6, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_country');

-- 5. 添加税务退费根菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT @system_parent_id, '税务退费', 'system_tax_refund', 1, '/tax-refund', 'Layout', 'system:tax-refund:view', 'DollarOutlined', 7, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund');

-- 6. 获取税务退费根菜单ID
SET @tax_refund_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund' AND menu_type = 1);

-- 7. 添加税务退费子菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@tax_refund_parent_id, '退税申请', 'system_tax_refund_apply', 2, 'apply', 'tax-refund/apply/index', 'system:tax-refund:apply', 'PlusOutlined', 1, 1),
(@tax_refund_parent_id, '申请列表', 'system_tax_refund_list', 2, 'list', 'tax-refund/list/index', 'system:tax-refund:list', 'UnorderedListOutlined', 2, 1),
(@tax_refund_parent_id, '财务审核', 'system_tax_refund_finance', 2, 'finance-review', 'tax-refund/finance-review/index', 'system:tax-refund:finance', 'AuditOutlined', 3, 1);

-- 8. 为管理员角色分配新菜单权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 1, id FROM `sys_menu` 
WHERE `menu_code` IN ('system_bank_account', 'system_country', 'system_tax_refund', 'system_tax_refund_apply', 'system_tax_refund_list', 'system_tax_refund_finance')
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 9. 验证菜单配置结果
SELECT 
    m1.menu_name as parent_menu,
    m2.menu_name as child_menu,
    m2.menu_code,
    m2.path,
    m2.component,
    m2.permission,
    m2.icon,
    m2.sort
FROM sys_menu m1
LEFT JOIN sys_menu m2 ON m1.id = m2.parent_id
WHERE m1.menu_code IN ('system', 'system_tax_refund')
ORDER BY m1.sort, m2.sort;

-- 10. 显示完整的系统管理菜单结构
SELECT 
    CONCAT(REPEAT('  ', LEVEL - 1), menu_name) as menu_structure,
    menu_code,
    path,
    component,
    permission
FROM (
    SELECT 
        id,
        menu_name,
        menu_code,
        path,
        component,
        permission,
        parent_id,
        1 as LEVEL
    FROM sys_menu 
    WHERE menu_code = 'system'
    
    UNION ALL
    
    SELECT 
        m.id,
        m.menu_name,
        m.menu_code,
        m.path,
        m.component,
        m.permission,
        m.parent_id,
        2 as LEVEL
    FROM sys_menu m
    JOIN sys_menu parent ON m.parent_id = parent.id
    WHERE parent.menu_code = 'system'
    
    UNION ALL
    
    SELECT 
        m.id,
        m.menu_name,
        m.menu_code,
        m.path,
        m.component,
        m.permission,
        m.parent_id,
        3 as LEVEL
    FROM sys_menu m
    JOIN sys_menu parent ON m.parent_id = parent.id
    JOIN sys_menu grandparent ON parent.parent_id = grandparent.id
    WHERE grandparent.menu_code = 'system'
) menu_tree
ORDER BY LEVEL, sort;