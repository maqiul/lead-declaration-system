-- 权限命名标准化执行脚本
-- 该脚本整合了所有权限标准化操作，按执行顺序排列

-- ========================================
-- 第一步：备份当前权限数据
-- ========================================
CREATE TABLE IF NOT EXISTS sys_menu_backup AS SELECT * FROM sys_menu WHERE permission IS NOT NULL AND permission != '';

-- ========================================
-- 第二步：更新权限标识为标准化格式
-- ========================================

-- 系统管理模块权限标准化
UPDATE sys_menu SET permission = CONCAT('system:', SUBSTRING(permission, LOCATE(':', permission) + 1)) 
WHERE permission REGEXP '^(user|role|menu|org|bank-account|city-info|country|product|transport):' 
  AND permission NOT LIKE 'system:%';

-- 业务功能模块权限（已符合规范，保持不变）
-- business:开头的权限标识已符合规范

-- 工作流模块权限（已符合规范，保持不变）
-- workflow:开头的权限标识已符合规范

-- 特别处理一些特殊情况
UPDATE sys_menu SET permission = 'system:user:list' WHERE permission = 'user:list';
UPDATE sys_menu SET permission = 'system:user:query' WHERE permission = 'user:query';
UPDATE sys_menu SET permission = 'system:user:add' WHERE permission = 'user:add';
UPDATE sys_menu SET permission = 'system:user:update' WHERE permission = 'user:update';
UPDATE sys_menu SET permission = 'system:user:delete' WHERE permission = 'user:delete';
UPDATE sys_menu SET permission = 'system:user:resetPwd' WHERE permission = 'user:resetPwd';

UPDATE sys_menu SET permission = 'system:role:list' WHERE permission = 'role:list';
UPDATE sys_menu SET permission = 'system:role:query' WHERE permission = 'role:query';
UPDATE sys_menu SET permission = 'system:role:add' WHERE permission = 'role:add';
UPDATE sys_menu SET permission = 'system:role:update' WHERE permission = 'role:update';
UPDATE sys_menu SET permission = 'system:role:delete' WHERE permission = 'role:delete';
UPDATE sys_menu SET permission = 'system:role:user' WHERE permission = 'role:user';
UPDATE sys_menu SET permission = 'system:role:assign' WHERE permission = 'role:assign';
UPDATE sys_menu SET permission = 'system:role:menu' WHERE permission = 'role:menu';

UPDATE sys_menu SET permission = 'system:menu:list' WHERE permission = 'menu:list';
UPDATE sys_menu SET permission = 'system:menu:query' WHERE permission = 'menu:query';
UPDATE sys_menu SET permission = 'system:menu:add' WHERE permission = 'menu:add';
UPDATE sys_menu SET permission = 'system:menu:update' WHERE permission = 'menu:update';
UPDATE sys_menu SET permission = 'system:menu:delete' WHERE permission = 'menu:delete';

UPDATE sys_menu SET permission = 'system:org:list' WHERE permission = 'org:list';
UPDATE sys_menu SET permission = 'system:org:query' WHERE permission = 'org:query';
UPDATE sys_menu SET permission = 'system:org:add' WHERE permission = 'org:add';
UPDATE sys_menu SET permission = 'system:org:update' WHERE permission = 'org:update';
UPDATE sys_menu SET permission = 'system:org:delete' WHERE permission = 'org:delete';
UPDATE sys_menu SET permission = 'system:org:user' WHERE permission = 'org:user';

-- ========================================
-- 第三步：验证权限标准化结果
-- ========================================

-- 检查权限格式是否符合规范
SELECT 
    '权限标准化验证结果' as check_item,
    CASE 
        WHEN COUNT(*) = 0 THEN '✓ 所有权限均符合规范格式'
        ELSE CONCAT('✗ 发现 ', COUNT(*), ' 个不符合规范的权限')
    END as result
FROM sys_menu 
WHERE permission IS NOT NULL 
  AND permission != ''
  AND permission NOT REGEXP '^(system|business|workflow):[a-zA-Z0-9_-]+(:[a-zA-Z0-9_-]+)+$';

-- 显示不符合规范的权限
SELECT 
    '不符合规范的权限列表' as check_item,
    menu_name,
    permission,
    '需要修正' as status
FROM sys_menu 
WHERE permission IS NOT NULL 
  AND permission != ''
  AND permission NOT REGEXP '^(system|business|workflow):[a-zA-Z0-9_-]+(:[a-zA-Z0-9_-]+)+$';

-- 显示符合规范的权限统计
SELECT 
    '权限标准化统计' as check_item,
    CONCAT('system域权限: ', COUNT(CASE WHEN permission LIKE 'system:%' THEN 1 END)) as system_permissions,
    CONCAT('business域权限: ', COUNT(CASE WHEN permission LIKE 'business:%' THEN 1 END)) as business_permissions,
    CONCAT('workflow域权限: ', COUNT(CASE WHEN permission LIKE 'workflow:%' THEN 1 END)) as workflow_permissions,
    CONCAT('总计权限数: ', COUNT(*)) as total_permissions
FROM sys_menu 
WHERE permission IS NOT NULL AND permission != '';

-- ========================================
-- 第四步：重新分配管理员权限
-- ========================================

-- 删除管理员现有权限
DELETE FROM sys_role_menu WHERE role_id = 1;

-- 为管理员分配所有权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE permission IS NOT NULL AND permission != '';

-- ========================================
-- 第五步：清理临时表
-- ========================================
DROP TABLE IF EXISTS sys_menu_backup;

-- ========================================
-- 第六步：最终验证
-- ========================================

-- 显示管理员权限数量
SELECT 
    '管理员权限分配验证' as check_item,
    COUNT(*) as admin_permission_count
FROM sys_role_menu rm
JOIN sys_menu m ON rm.menu_id = m.id
WHERE rm.role_id = 1;

-- 显示所有权限按域分类
SELECT 
    '权限域分布' as check_item,
    LEFT(permission, LOCATE(':', permission) - 1) as domain,
    COUNT(*) as permission_count
FROM sys_menu 
WHERE permission IS NOT NULL AND permission != ''
GROUP BY LEFT(permission, LOCATE(':', permission) - 1)
ORDER BY domain;

-- ========================================
-- 执行完成后验证步骤
-- ========================================

/*
执行完此脚本后，请执行以下验证步骤：

1. 检查应用程序是否正常运行
2. 测试不同角色的权限访问
3. 验证管理员权限是否完整
4. 检查前端权限控制是否正常

如果发现问题，请使用以下回滚命令：
```sql
-- 回滚权限更改
UPDATE sys_menu sm 
JOIN sys_menu_backup sb ON sm.id = sb.id 
SET sm.permission = sb.permission;
```
*/