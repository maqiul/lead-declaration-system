-- 银行账户配置菜单项
-- 添加到系统管理目录下

-- 1. 获取系统管理根菜单ID
SET @system_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'system' AND menu_type = 1);

-- 2. 插入银行账户配置菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT @system_parent_id, '银行账户', 'system_bank_account', 2, 'bank-account', 'system/bank-account/index', 'system:bank:list', 'BankOutlined', 6, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_bank_account');

-- 3. 插入银行账户操作权限（按钮类型）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_bank_account'), 
    '账户查询', 'system_bank_query', 3, '', '', 'system:bank:query', '', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_bank_query');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_bank_account'), 
    '账户新增', 'system_bank_add', 3, '', '', 'system:bank:add', '', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_bank_add');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_bank_account'), 
    '账户修改', 'system_bank_update', 3, '', '', 'system:bank:edit', '', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_bank_update');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_bank_account'), 
    '账户删除', 'system_bank_delete', 3, '', '', 'system:bank:delete', '', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_bank_delete');

-- 4. 为管理员角色分配银行账户相关权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE menu_code IN ('system_bank_account')
   OR menu_code LIKE 'system_bank_%'
   OR parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code = 'system_bank_account'
   )
ON DUPLICATE KEY UPDATE create_time = NOW();

-- 5. 验证菜单配置结果
SELECT 
    m.id,
    m.menu_name,
    m.menu_code,
    m.menu_type,
    CASE m.menu_type 
        WHEN 1 THEN '目录'
        WHEN 2 THEN '菜单'
        WHEN 3 THEN '按钮'
    END as menu_type_desc,
    m.path,
    m.component,
    m.permission,
    m.icon,
    m.sort,
    p.menu_name as parent_name,
    CASE WHEN rm.menu_id IS NOT NULL THEN '✓' ELSE '✗' END as admin_has_permission
FROM sys_menu m
LEFT JOIN sys_menu p ON m.parent_id = p.id
LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id AND rm.role_id = 1
WHERE m.menu_code LIKE 'system_bank%' 
   OR m.parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code = 'system_bank_account'
   )
ORDER BY m.menu_type, m.sort;

-- 6. 验证文件路径对应关系
SELECT 
    '银行账户配置' as module,
    '/system/bank-account' as route_path,
    'system/bank-account/index.vue' as component_path
UNION ALL
SELECT 
    '国家信息管理' as module,
    '/system/country' as route_path,
    'system/country/index.vue' as component_path;