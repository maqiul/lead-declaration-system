-- 修复管理员角色权限配置
-- 确保管理员角色(role_id=1)拥有所有菜单权限

-- 1. 删除管理员角色现有的权限配置（避免重复）
DELETE FROM sys_role_menu WHERE role_id = 1;

-- 2. 为管理员角色分配所有启用的菜单权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE status = 1;

-- 3. 验证权限分配结果
SELECT 
    r.role_name,
    COUNT(rm.menu_id) as menu_count,
    GROUP_CONCAT(m.menu_name ORDER BY m.id) as menus
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
LEFT JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.id = 1
GROUP BY r.id, r.role_name;

-- 4. 检查管理员用户是否拥有管理员角色
SELECT 
    u.username,
    u.nickname,
    r.role_name,
    r.role_code
FROM sys_user u
JOIN sys_user_role ur ON u.id = ur.user_id
JOIN sys_role r ON ur.role_id = r.id
WHERE u.id = 1;

-- 5. 验证特定权限是否存在
SELECT 
    r.role_name,
    m.menu_name,
    m.permission
FROM sys_role r
JOIN sys_role_menu rm ON r.id = rm.role_id
JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.id = 1 
AND m.permission LIKE '%config%'
ORDER BY m.id;