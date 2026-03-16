-- 正式申报单菜单配置
-- 配置申报管理列表页面和统计页面

-- 1. 确保申报单根菜单存在
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 0, '出口申报', 'declaration', 1, '/declaration', 'Layout', '', 'FileProtectOutlined', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration' AND menu_type = 1);

-- 获取申报单根菜单ID
SET @declaration_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'declaration' AND menu_type = 1);

-- 2. 插入申报管理菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT @declaration_parent_id, '申报管理', 'declaration_manage', 2, 'manage', 'declaration/manage/index', 'declaration:manage:list', 'ContainerOutlined', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage');

-- 3. 插入申报统计菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT @declaration_parent_id, '申报统计', 'declaration_statistics', 2, 'statistics', 'declaration/statistics/index', 'declaration:statistics:list', 'BarChartOutlined', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_statistics');

-- 4. 插入申报管理操作权限（按钮类型）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'declaration_manage'), 
    '申报查询', 'declaration_manage_query', 3, '', '', 'declaration:manage:query', '', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage_query');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'declaration_manage'), 
    '申报新增', 'declaration_manage_add', 3, '', '', 'declaration:manage:add', '', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage_add');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'declaration_manage'), 
    '申报修改', 'declaration_manage_update', 3, '', '', 'declaration:manage:update', '', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage_update');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'declaration_manage'), 
    '申报删除', 'declaration_manage_delete', 3, '', '', 'declaration:manage:delete', '', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage_delete');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'declaration_manage'), 
    '申报导出', 'declaration_manage_export', 3, '', '', 'declaration:manage:export', '', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_manage_export');

-- 5. 插入申报统计操作权限（按钮类型）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'declaration_statistics'), 
    '统计查询', 'declaration_statistics_query', 3, '', '', 'declaration:statistics:query', '', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_statistics_query');

-- 6. 为管理员角色分配申报单相关权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE menu_code IN ('declaration', 'declaration_manage', 'declaration_statistics')
   OR menu_code LIKE 'declaration_manage_%'
   OR menu_code LIKE 'declaration_statistics_%'
   OR parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code IN ('declaration_manage', 'declaration_statistics')
   )
ON DUPLICATE KEY UPDATE create_time = NOW();

-- 7. 验证菜单配置结果
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
WHERE m.menu_code LIKE 'declaration%' 
   OR m.parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code = 'declaration'
   )
ORDER BY m.menu_type, m.sort;

-- 8. 验证文件路径对应关系
SELECT 
    '申报管理' as module,
    '/declaration/manage' as route_path,
    'declaration/manage/index.vue' as component_path
UNION ALL
SELECT 
    '申报统计' as module,
    '/declaration/statistics' as route_path,
    'declaration/statistics/index.vue' as component_path;
