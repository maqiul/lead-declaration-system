-- 权限命名对照表
-- 用于记录权限标准化前后的对应关系，便于追溯和维护

-- ========================================
-- 1. 权限标准化映射表
-- ========================================

-- 创建权限映射表用于记录变更
CREATE TABLE IF NOT EXISTS permission_mapping (
    id INT AUTO_INCREMENT PRIMARY KEY,
    old_permission VARCHAR(255) NOT NULL COMMENT '旧权限标识',
    new_permission VARCHAR(255) NOT NULL COMMENT '新权限标识',
    module_name VARCHAR(100) COMMENT '所属模块',
    description TEXT COMMENT '权限描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入权限映射数据
INSERT INTO permission_mapping (old_permission, new_permission, module_name, description) VALUES
-- 系统管理模块
('user:list', 'system:user:list', '用户管理', '用户列表'),
('user:query', 'system:user:query', '用户管理', '用户查询'),
('user:add', 'system:user:add', '用户管理', '用户新增'),
('user:update', 'system:user:update', '用户管理', '用户修改'),
('user:delete', 'system:user:delete', '用户管理', '用户删除'),
('user:resetPwd', 'system:user:resetPwd', '用户管理', '重置用户密码'),

('role:list', 'system:role:list', '角色管理', '角色列表'),
('role:query', 'system:role:query', '角色管理', '角色查询'),
('role:add', 'system:role:add', '角色管理', '角色新增'),
('role:update', 'system:role:update', '角色管理', '角色修改'),
('role:delete', 'system:role:delete', '角色管理', '角色删除'),
('role:user', 'system:role:user', '角色管理', '用户角色管理'),
('role:assign', 'system:role:assign', '角色管理', '角色分配'),
('role:menu', 'system:role:menu', '角色管理', '菜单权限管理'),

('menu:list', 'system:menu:list', '菜单管理', '菜单列表'),
('menu:query', 'system:menu:query', '菜单管理', '菜单查询'),
('menu:add', 'system:menu:add', '菜单管理', '菜单新增'),
('menu:update', 'system:menu:update', '菜单管理', '菜单修改'),
('menu:delete', 'system:menu:delete', '菜单管理', '菜单删除'),

('org:list', 'system:org:list', '组织管理', '组织列表'),
('org:query', 'system:org:query', '组织管理', '组织查询'),
('org:add', 'system:org:add', '组织管理', '组织新增'),
('org:update', 'system:org:update', '组织管理', '组织修改'),
('org:delete', 'system:org:delete', '组织管理', '组织删除'),
('org:user', 'system:org:user', '组织管理', '组织用户管理'),

('system:bank-account:query', 'system:bank-account:query', '银行账户配置', '银行账户查询'),
('system:bank-account:view', 'system:bank-account:view', '银行账户配置', '银行账户查看'),
('system:bank-account:add', 'system:bank-account:add', '银行账户配置', '银行账户新增'),
('system:bank-account:update', 'system:bank-account:update', '银行账户配置', '银行账户修改'),
('system:bank-account:delete', 'system:bank-account:delete', '银行账户配置', '银行账户删除'),

('system:city-info:list', 'system:city-info:list', '城市信息管理', '城市信息列表'),
('system:city-info:query', 'system:city-info:query', '城市信息管理', '城市信息查询'),
('system:city-info:add', 'system:city-info:add', '城市信息管理', '城市信息新增'),
('system:city-info:update', 'system:city-info:update', '城市信息管理', '城市信息修改'),
('system:city-info:delete', 'system:city-info:delete', '城市信息管理', '城市信息删除'),

('system:country:query', 'system:country:query', '国家信息管理', '国家信息查询'),
('system:country:add', 'system:country:add', '国家信息管理', '国家信息新增'),
('system:country:update', 'system:country:update', '国家信息管理', '国家信息修改'),
('system:country:delete', 'system:country:delete', '国家信息管理', '国家信息删除'),

('system:product:query', 'system:product:query', '产品类型管理', '产品类型查询'),
('system:product:add', 'system:product:add', '产品类型管理', '产品类型新增'),
('system:product:update', 'system:product:update', '产品类型管理', '产品类型修改'),
('system:product:delete', 'system:product:delete', '产品类型管理', '产品类型删除'),

('system:transport:list', 'system:transport:list', '运输方式管理', '运输方式列表'),
('system:transport:view', 'system:transport:view', '运输方式管理', '运输方式查看'),
('system:transport:add', 'system:transport:add', '运输方式管理', '运输方式新增'),
('system:transport:edit', 'system:transport:edit', '运输方式管理', '运输方式编辑'),
('system:transport:delete', 'system:transport:delete', '运输方式管理', '运输方式删除'),

-- 业务功能模块
('business:declaration:list', 'business:declaration:list', '申报管理', '申报列表'),
('business:declaration:statistics', 'business:declaration:statistics', '申报管理', '申报统计'),
('business:declaration:edit', 'business:declaration:edit', '申报管理', '申报编辑'),
('business:declaration:view', 'business:declaration:view', '申报管理', '申报查看'),
('business:declaration:delete', 'business:declaration:delete', '申报管理', '申报删除'),
('business:declaration:audit', 'business:declaration:audit', '申报管理', '申报审核'),
('business:declaration:query', 'business:declaration:query', '申报管理', '申报查询'),
('business:declaration:add', 'business:declaration:add', '申报管理', '申报新增'),
('business:declaration:submit', 'business:declaration:submit', '申报管理', '申报提交'),
('business:declaration:export', 'business:declaration:export', '申报管理', '申报导出'),
('business:declaration:download', 'business:declaration:download', '申报管理', '申报下载'),
('business:declaration:contract', 'business:declaration:contract', '申报管理', '申报合同'),
('business:declaration:payment', 'business:declaration:payment', '申报管理', '申报付款'),
('business:declaration:pickup-submit', 'business:declaration:pickup-submit', '申报管理', '提货单提交'),
('business:declaration:pickup-audit', 'business:declaration:pickup-audit', '申报管理', '提货单审核'),
('business:declaration:pickup-delete', 'business:declaration:pickup-delete', '申报管理', '提货单删除'),

('business:contract:template:query', 'business:contract:template:query', '合同管理', '合同模板查询'),
('business:contract:template:add', 'business:contract:template:add', '合同管理', '合同模板新增'),
('business:contract:template:update', 'business:contract:template:update', '合同管理', '合同模板修改'),
('business:contract:template:delete', 'business:contract:template:delete', '合同管理', '合同模板删除'),
('business:contract:template:upload', 'business:contract:template:upload', '合同管理', '合同模板上传'),
('business:contract:generate', 'business:contract:generate', '合同管理', '合同生成'),
('business:contract:generation:query', 'business:contract:generation:query', '合同管理', '合同生成记录查询'),
('business:contract:download', 'business:contract:download', '合同管理', '合同下载'),
('business:contract:generation:delete', 'business:contract:generation:delete', '合同管理', '合同生成记录删除'),
('business:contract:edit', 'business:contract:edit', '合同管理', '合同编辑'),

('business:tax-refund:apply', 'business:tax-refund:apply', '税务退费', '税务退费申请'),
('business:tax-refund:list', 'business:tax-refund:list', '税务退费', '税务退费列表'),
('business:tax-refund:finance', 'business:tax-refund:finance', '税务退费', '税务退费财务'),
('business:tax-refund:approve', 'business:tax-refund:approve', '税务退费', '税务退费审批'),
('business:tax-refund:edit', 'business:tax-refund:edit', '税务退费', '税务退费编辑'),
('business:tax-refund:detail', 'business:tax-refund:detail', '税务退费', '税务退费详情'),
('business:tax-refund:submit', 'business:tax-refund:submit', '税务退费', '税务退费提交'),

('business:delivery-order:list', 'business:delivery-order:list', '提货单', '提货单列表'),
('business:delivery-order:add', 'business:delivery-order:add', '提货单', '提货单新增'),
('business:delivery-order:view', 'business:delivery-order:view', '提货单', '提货单查看'),

-- 工作流模块
('workflow:definition:deploy', 'workflow:definition:deploy', '工作流', '流程定义部署'),
('workflow:definition:list', 'workflow:definition:list', '工作流', '流程定义列表'),
('workflow:definition:update', 'workflow:definition:update', '工作流', '流程定义更新'),
('workflow:definition:delete', 'workflow:definition:delete', '工作流', '流程定义删除'),
('workflow:definition:view', 'workflow:definition:view', '工作流', '流程定义查看'),

('workflow:instance:start', 'workflow:instance:start', '工作流', '流程实例启动'),
('workflow:instance:list', 'workflow:instance:list', '工作流', '流程实例列表'),
('workflow:instance:suspend', 'workflow:instance:suspend', '工作流', '流程实例挂起'),
('workflow:instance:activate', 'workflow:instance:activate', '工作流', '流程实例激活'),
('workflow:instance:terminate', 'workflow:instance:terminate', '工作流', '流程实例终止'),

('workflow:task:list', 'workflow:task:list', '工作流', '任务列表'),
('workflow:task:claim', 'workflow:task:claim', '工作流', '任务签收'),
('workflow:task:complete', 'workflow:task:complete', '工作流', '任务完成'),
('workflow:task:reject', 'workflow:task:reject', '工作流', '任务驳回'),
('workflow:task:transfer', 'workflow:task:transfer', '工作流', '任务转办'),

('workflow:monitor:view', 'workflow:monitor:view', '工作流', '监控查看');

-- ========================================
-- 2. 权限分类统计
-- ========================================

-- 按域统计权限数量
SELECT 
    '按域统计权限数量' as statistic_type,
    LEFT(permission, LOCATE(':', permission) - 1) as domain,
    COUNT(*) as permission_count
FROM sys_menu 
WHERE permission IS NOT NULL AND permission != ''
GROUP BY LEFT(permission, LOCATE(':', permission) - 1)
ORDER BY domain;

-- 按模块统计权限数量
SELECT 
    '按模块统计权限数量' as statistic_type,
    SUBSTRING_INDEX(SUBSTRING_INDEX(permission, ':', 2), ':', -1) as module,
    COUNT(*) as permission_count
FROM sys_menu 
WHERE permission IS NOT NULL AND permission != ''
GROUP BY SUBSTRING_INDEX(SUBSTRING_INDEX(permission, ':', 2), ':', -1)
ORDER BY module;

-- 按操作类型统计权限数量
SELECT 
    '按操作类型统计权限数量' as statistic_type,
    LOWER(RIGHT(permission, LENGTH(permission) - LOCATE(':', REVERSE(permission)))) as operation_type,
    COUNT(*) as permission_count
FROM sys_menu 
WHERE permission IS NOT NULL AND permission != ''
GROUP BY RIGHT(permission, LENGTH(permission) - LOCATE(':', REVERSE(permission)))
ORDER BY permission_count DESC;

-- ========================================
-- 3. 权限命名规范验证查询
-- ========================================

-- 验证权限命名是否符合规范格式
SELECT 
    '权限命名规范验证' as validation_type,
    menu_name,
    permission,
    CASE 
        WHEN permission REGEXP '^(system|business|workflow):[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$' THEN '✓ 符合标准格式(域:模块:操作)'
        WHEN permission REGEXP '^(system|business|workflow):[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$' THEN '✓ 符合标准格式(域:模块:功能:操作)'
        WHEN permission REGEXP '^[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$' THEN '? 基本符合但域不规范'
        WHEN permission REGEXP '^[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$' THEN '? 基本符合但域不规范'
        ELSE '✗ 不符合规范格式'
    END as format_status
FROM sys_menu 
WHERE permission IS NOT NULL AND permission != ''
ORDER BY format_status, permission;

-- ========================================
-- 4. 权限使用频率分析（基于角色分配）
-- ========================================

-- 统计各权限在角色中的分配情况
SELECT 
    '权限使用频率分析' as analysis_type,
    m.permission,
    m.menu_name,
    COUNT(rm.role_id) as assigned_roles_count,
    GROUP_CONCAT(DISTINCT r.role_name ORDER BY r.role_name) as assigned_roles
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id
LEFT JOIN sys_role r ON rm.role_id = r.id
WHERE m.permission IS NOT NULL AND m.permission != ''
GROUP BY m.id, m.permission, m.menu_name
ORDER BY assigned_roles_count DESC, m.permission;

-- ========================================
-- 5. 权限标准化实施建议
-- ========================================

/*
权限标准化实施步骤：

1. 备份数据库：在执行任何更改之前，请务必备份数据库
   ```sql
   mysqldump -u username -p database_name > backup.sql
   ```

2. 执行权限标准化脚本：
   ```sql
   USE database_name;
   SOURCE permission-naming-standardization.sql;
   ```

3. 更新后端代码：修改所有 @RequiresPermissions 注解中的权限标识
   - 例如：@RequiresPermissions("user:add") -> @RequiresPermissions("system:user:add")

4. 更新前端代码：修改所有权限验证相关的代码
   - 例如：v-permission="['user:add']" -> v-permission="['system:user:add']"

5. 测试验证：确保所有功能在权限更改后仍能正常工作

注意事项：
- 权限更改可能影响现有用户的角色分配，需要重新分配权限
- 建议在非生产环境中先行测试
- 记录所有权限变更以便回滚
*/