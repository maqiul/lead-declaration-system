-- 税务退费审核权限补充配置

-- 为税务退费列表添加审核相关权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
    '申请审核', 'system_tax_refund_approve', 3, '', '', 'system:tax-refund:approve', '', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_approve');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
    '财务初审', 'system_tax_refund_first_review', 3, '', '', 'system:tax-refund:first-review', '', 6, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_first_review');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
    '财务复审', 'system_tax_refund_final_review', 3, '', '', 'system:tax-refund:final-review', '', 7, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_final_review');

-- 为财务角色分配审核权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 2, id FROM `sys_menu` 
WHERE `menu_code` IN ('system_tax_refund_approve', 'system_tax_refund_first_review', 'system_tax_refund_final_review')
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 验证权限配置
SELECT 
    r.role_name,
    m.menu_name,
    m.menu_code,
    m.permission
FROM sys_role_menu rm
JOIN sys_menu m ON rm.menu_id = m.id
JOIN sys_role r ON rm.role_id = r.id
WHERE m.menu_code LIKE 'system_tax_refund%'
    AND r.role_name IN ('管理员', '财务')
ORDER BY r.role_name, m.sort;