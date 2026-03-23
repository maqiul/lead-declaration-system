-- 运输方式配置菜单项
-- 添加到系统管理目录下

-- 1. 获取系统管理根菜单ID
SET @system_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'system' AND menu_type = 1);

-- 2. 插入运输方式配置菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT @system_parent_id, '运输方式', 'system_transport', 2, 'transport-mode', 'system/transport-mode/index', 'system:transport:list', 'CarOutlined', 8, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_transport');

-- 3. 插入运输方式操作权限（按钮类型）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_transport'), 
    '列表查看', 'system_transport_list', 3, '', '', 'system:transport:list', '', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_transport_list');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_transport'), 
    '详情查看', 'system_transport_view', 3, '', '', 'system:transport:view', '', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_transport_view');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_transport'), 
    '运输方式新增', 'system_transport_add', 3, '', '', 'system:transport:add', '', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_transport_add');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_transport'), 
    '运输方式编辑', 'system_transport_edit', 3, '', '', 'system:transport:edit', '', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_transport_edit');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    (SELECT id FROM sys_menu WHERE menu_code = 'system_transport'), 
    '运输方式删除', 'system_transport_delete', 3, '', '', 'system:transport:delete', '', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_transport_delete');

-- 4. 为管理员角色分配运输方式相关权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE menu_code IN ('system_transport')
   OR menu_code LIKE 'system_transport_%'
   OR parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code = 'system_transport'
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
WHERE m.menu_code LIKE 'system_transport%' 
   OR m.parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code = 'system_transport'
   )
ORDER BY m.menu_type, m.sort;

-- 6. 验证文件路径对应关系
SELECT 
    '运输方式配置' as module,
    '/system/transport-mode' as route_path,
    'system/transport-mode/index.vue' as component_path;
