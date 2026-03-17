-- 临时修复国家信息管理权限问题
-- 确保管理员角色拥有system:country:*相关权限

-- 1. 检查当前管理员角色权限
SELECT 
    '当前权限状态' as info,
    r.role_name,
    m.menu_name,
    m.permission
FROM sys_role r
JOIN sys_role_menu rm ON r.id = rm.role_id
JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.role_code = 'ADMIN' 
AND m.permission LIKE '%country%'
ORDER BY m.menu_type, m.sort;

-- 2. 检查是否有国家信息相关菜单
SELECT 
    '国家信息菜单' as info,
    m.id,
    m.menu_name,
    m.menu_code,
    m.permission,
    CASE 
        WHEN rm.menu_id IS NOT NULL THEN '✓ 已分配'
        ELSE '✗ 未分配'
    END as assigned_status
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id AND rm.role_id = (SELECT id FROM sys_role WHERE role_code = 'ADMIN')
WHERE m.menu_code LIKE '%country%'
ORDER BY m.menu_type, m.sort;

-- 3. 如果权限缺失，手动添加国家信息相关权限
-- 先找出所有国家信息相关的菜单ID
SET @admin_role_id = (SELECT id FROM sys_role WHERE role_code = 'ADMIN');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 
    @admin_role_id,
    m.id,
    NOW()
FROM sys_menu m
WHERE m.menu_code LIKE '%country%'
AND m.status = 1;

-- 4. 验证修复结果
SELECT 
    '修复后验证' as info,
    r.role_name,
    COUNT(rm.menu_id) as country_menu_count,
    GROUP_CONCAT(m.menu_name ORDER BY m.menu_type, m.sort) as country_menus
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
LEFT JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.role_code = 'ADMIN' 
AND m.menu_code LIKE '%country%'
GROUP BY r.id, r.role_name;

-- 5. 检查具体的权限标识
SELECT 
    '具体权限标识' as info,
    r.role_name,
    m.menu_name,
    m.permission,
    m.menu_type
FROM sys_role r
JOIN sys_role_menu rm ON r.id = rm.role_id
JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.role_code = 'ADMIN' 
AND m.permission LIKE '%country%'
ORDER BY m.menu_type, m.sort;