-- 税务退费菜单配置

-- 获取业务管理根菜单ID
SET @business_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'business' AND menu_type = 1);

-- 插入税务退费根菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT @business_parent_id, '税务退费', 'business_tax_refund', 1, '/business/tax-refund', 'Layout', 'business:tax-refund:view', 'DollarOutlined', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'business_tax_refund');

-- 获取税务退费根菜单ID
SET @tax_refund_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund' AND menu_type = 1);

-- 插入税务退费子菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@tax_refund_parent_id, '退税申请', 'business_tax_refund_apply', 2, 'apply', 'business/tax-refund/application', 'business:tax-refund:apply', 'PlusOutlined', 1, 1),
(@tax_refund_parent_id, '申请列表', 'business_tax_refund_list', 2, 'list', 'business/tax-refund/list', 'business:tax-refund:list', 'UnorderedListOutlined', 2, 1),
(@tax_refund_parent_id, '财务审核', 'business_tax_refund_finance', 2, 'finance-review', 'business/tax-refund/finance-review', 'business:tax-refund:finance', 'AuditOutlined', 3, 1);

-- 插入按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund_list'), 
    '申请查询', 'business_tax_refund_query', 3, '', '', 'business:tax-refund:query', '', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'business_tax_refund_query');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund_list'), 
    '申请新增', 'business_tax_refund_add', 3, '', '', 'business:tax-refund:add', '', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'business_tax_refund_add');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund_list'), 
    '申请修改', 'business_tax_refund_edit', 3, '', '', 'business:tax-refund:edit', '', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'business_tax_refund_edit');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund_list'), 
    '申请删除', 'business_tax_refund_delete', 3, '', '', 'business:tax-refund:delete', '', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'business_tax_refund_delete');

-- 为财务角色和管理员角色分配权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 1, id FROM `sys_menu` WHERE `menu_code` LIKE 'business_tax_refund%'
ON DUPLICATE KEY UPDATE role_id = role_id;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 2, id FROM `sys_menu` WHERE `menu_code` LIKE 'business_tax_refund%'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 验证菜单配置
SELECT 
    m1.menu_name as parent_menu,
    m2.menu_name as child_menu,
    m2.menu_code,
    m2.path,
    m2.component,
    m2.permission
FROM sys_menu m1
JOIN sys_menu m2 ON m1.id = m2.parent_id
WHERE m1.menu_code = 'business_tax_refund'
ORDER BY m2.sort;