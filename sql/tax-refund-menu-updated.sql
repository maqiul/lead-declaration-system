-- 税务退费独立菜单配置（更新版）

-- 删除旧的业务管理下的税务退费菜单
DELETE FROM sys_menu WHERE menu_code LIKE 'business_tax_refund%';

-- 获取系统管理根菜单ID（作为新的父级菜单）
SET @system_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'system' AND menu_type = 1);

-- 插入税务退费根菜单（作为系统管理的子菜单）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT @system_parent_id, '税务退费', 'system_tax_refund', 1, '/tax-refund', 'Layout', 'system:tax-refund:view', 'DollarOutlined', 7, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund');

-- 获取税务退费根菜单ID
SET @tax_refund_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund' AND menu_type = 1);

-- 插入税务退费子菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@tax_refund_parent_id, '退税申请', 'system_tax_refund_apply', 2, 'apply', 'tax-refund/apply/index', 'system:tax-refund:apply', 'PlusOutlined', 1, 1),
(@tax_refund_parent_id, '申请列表', 'system_tax_refund_list', 2, 'list', 'tax-refund/list/index', 'system:tax-refund:list', 'UnorderedListOutlined', 2, 1),
(@tax_refund_parent_id, '财务审核', 'system_tax_refund_finance', 2, 'finance-review', 'tax-refund/finance-review/index', 'system:tax-refund:finance', 'AuditOutlined', 3, 1);

-- 插入按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
    '申请查询', 'system_tax_refund_query', 3, '', '', 'system:tax-refund:query', '', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_query');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
    '申请新增', 'system_tax_refund_add', 3, '', '', 'system:tax-refund:add', '', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_add');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
    '申请修改', 'system_tax_refund_edit', 3, '', '', 'system:tax-refund:edit', '', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_edit');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
    '申请删除', 'system_tax_refund_delete', 3, '', '', 'system:tax-refund:delete', '', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_delete');

-- 为管理员角色分配权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 1, id FROM `sys_menu` WHERE `menu_code` LIKE 'system_tax_refund%'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 验证菜单配置
SELECT 
    m1.menu_name as parent_menu,
    m2.menu_name as child_menu,
    m2.menu_code,
    m2.path,
    m2.component,
    m2.permission,
    m2.menu_type
FROM sys_menu m1
JOIN sys_menu m2 ON m1.id = m2.parent_id
WHERE m1.menu_code = 'system_tax_refund'
ORDER BY m2.sort;

-- 验证路由路径对应关系
SELECT 
    '税务退费功能' as module,
    '/tax-refund' as route_base,
    'tax-refund/apply/index' as apply_component,
    'tax-refund/list/index' as list_component,
    'tax-refund/detail/index' as detail_component,
    'tax-refund/finance-review/index' as finance_component;