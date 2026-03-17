-- 检查和修复管理员权限配置
-- 确保admin用户拥有正确的角色和权限

-- 1. 检查admin用户信息
SELECT '用户信息' as info_type, id, username, nickname, status FROM sys_user WHERE username = 'admin';

-- 2. 检查admin用户的角色分配
SELECT 
    '用户角色' as info_type,
    u.username,
    r.role_name,
    r.role_code,
    ur.create_time
FROM sys_user u
JOIN sys_user_role ur ON u.id = ur.user_id
JOIN sys_role r ON ur.role_id = r.id
WHERE u.username = 'admin';

-- 3. 检查管理员角色的菜单权限
SELECT 
    '角色权限' as info_type,
    r.role_name,
    COUNT(rm.menu_id) as menu_count,
    GROUP_CONCAT(m.menu_name ORDER BY m.sort) as menus
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
LEFT JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.role_code = 'ADMIN'
GROUP BY r.id, r.role_name;

-- 4. 检查具体的权限标识
SELECT 
    '具体权限' as info_type,
    r.role_name,
    m.menu_name,
    m.permission,
    m.menu_type
FROM sys_role r
JOIN sys_role_menu rm ON r.id = rm.role_id
JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.role_code = 'ADMIN' 
AND m.permission IS NOT NULL 
AND m.permission != ''
ORDER BY m.menu_type, m.sort;

-- 5. 如果权限不足，重新分配所有权限给管理员角色
-- 注意：这里会先清空现有权限，然后重新分配
DELETE FROM sys_role_menu WHERE role_id = (SELECT id FROM sys_role WHERE role_code = 'ADMIN');

INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 
    (SELECT id FROM sys_role WHERE role_code = 'ADMIN'), 
    id, 
    NOW()
FROM sys_menu 
WHERE status = 1;

-- 6. 验证修复结果
SELECT 
    '修复后验证' as info_type,
    r.role_name,
    COUNT(rm.menu_id) as menu_count,
    CASE 
        WHEN COUNT(rm.menu_id) > 0 THEN '✓ 权限已配置'
        ELSE '✗ 权限未配置'
    END as status
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
WHERE r.role_code = 'ADMIN'
GROUP BY r.id, r.role_name;