-- 权限命名标准化脚本
-- 此脚本将系统中所有的权限标识规范化为统一格式：域:模块:操作
-- 其中域分为system(系统管理)、business(业务功能)、workflow(工作流)
-- 模块名使用小写字母和连字符，操作类型使用标准词汇

-- ========================================
-- 1. 系统管理模块权限 (system域)
-- ========================================

-- 用户管理权限
UPDATE sys_menu SET permission = 'system:user:list' WHERE permission = 'user:list';
UPDATE sys_menu SET permission = 'system:user:query' WHERE permission = 'user:query';
UPDATE sys_menu SET permission = 'system:user:add' WHERE permission = 'user:add';
UPDATE sys_menu SET permission = 'system:user:update' WHERE permission = 'user:update';
UPDATE sys_menu SET permission = 'system:user:delete' WHERE permission = 'user:delete';
UPDATE sys_menu SET permission = 'system:user:resetPwd' WHERE permission = 'user:resetPwd';

-- 角色管理权限
UPDATE sys_menu SET permission = 'system:role:list' WHERE permission = 'role:list';
UPDATE sys_menu SET permission = 'system:role:query' WHERE permission = 'role:query';
UPDATE sys_menu SET permission = 'system:role:add' WHERE permission = 'role:add';
UPDATE sys_menu SET permission = 'system:role:update' WHERE permission = 'role:update';
UPDATE sys_menu SET permission = 'system:role:delete' WHERE permission = 'role:delete';
UPDATE sys_menu SET permission = 'system:role:user' WHERE permission = 'role:user';
UPDATE sys_menu SET permission = 'system:role:assign' WHERE permission = 'role:assign';
UPDATE sys_menu SET permission = 'system:role:menu' WHERE permission = 'role:menu';

-- 菜单管理权限
UPDATE sys_menu SET permission = 'system:menu:list' WHERE permission = 'menu:list';
UPDATE sys_menu SET permission = 'system:menu:query' WHERE permission = 'menu:query';
UPDATE sys_menu SET permission = 'system:menu:add' WHERE permission = 'menu:add';
UPDATE sys_menu SET permission = 'system:menu:update' WHERE permission = 'menu:update';
UPDATE sys_menu SET permission = 'system:menu:delete' WHERE permission = 'menu:delete';

-- 组织管理权限
UPDATE sys_menu SET permission = 'system:org:list' WHERE permission = 'org:list';
UPDATE sys_menu SET permission = 'system:org:query' WHERE permission = 'org:query';
UPDATE sys_menu SET permission = 'system:org:add' WHERE permission = 'org:add';
UPDATE sys_menu SET permission = 'system:org:update' WHERE permission = 'org:update';
UPDATE sys_menu SET permission = 'system:org:delete' WHERE permission = 'org:delete';
UPDATE sys_menu SET permission = 'system:org:user' WHERE permission = 'org:user';

-- 银行账户配置权限
UPDATE sys_menu SET permission = 'system:bank-account:query' WHERE permission = 'system:bank-account:query';
UPDATE sys_menu SET permission = 'system:bank-account:view' WHERE permission = 'system:bank-account:view';
UPDATE sys_menu SET permission = 'system:bank-account:add' WHERE permission = 'system:bank-account:add';
UPDATE sys_menu SET permission = 'system:bank-account:update' WHERE permission = 'system:bank-account:update';
UPDATE sys_menu SET permission = 'system:bank-account:delete' WHERE permission = 'system:bank-account:delete';

-- 城市信息管理权限
UPDATE sys_menu SET permission = 'system:city-info:list' WHERE permission = 'system:city-info:list';
UPDATE sys_menu SET permission = 'system:city-info:query' WHERE permission = 'system:city-info:query';
UPDATE sys_menu SET permission = 'system:city-info:add' WHERE permission = 'system:city-info:add';
UPDATE sys_menu SET permission = 'system:city-info:update' WHERE permission = 'system:city-info:update';
UPDATE sys_menu SET permission = 'system:city-info:delete' WHERE permission = 'system:city-info:delete';

-- 国家信息管理权限
UPDATE sys_menu SET permission = 'system:country:query' WHERE permission = 'system:country:query';
UPDATE sys_menu SET permission = 'system:country:add' WHERE permission = 'system:country:add';
UPDATE sys_menu SET permission = 'system:country:update' WHERE permission = 'system:country:update';
UPDATE sys_menu SET permission = 'system:country:delete' WHERE permission = 'system:country:delete';

-- 产品/商品类型管理权限
UPDATE sys_menu SET permission = 'system:product:query' WHERE permission = 'system:product:query';
UPDATE sys_menu SET permission = 'system:product:add' WHERE permission = 'system:product:add';
UPDATE sys_menu SET permission = 'system:product:update' WHERE permission = 'system:product:update';
UPDATE sys_menu SET permission = 'system:product:delete' WHERE permission = 'system:product:delete';

-- 运输方式管理权限
UPDATE sys_menu SET permission = 'system:transport:list' WHERE permission = 'system:transport:list';
UPDATE sys_menu SET permission = 'system:transport:view' WHERE permission = 'system:transport:view';
UPDATE sys_menu SET permission = 'system:transport:add' WHERE permission = 'system:transport:add';
UPDATE sys_menu SET permission = 'system:transport:edit' WHERE permission = 'system:transport:edit';
UPDATE sys_menu SET permission = 'system:transport:delete' WHERE permission = 'system:transport:delete';

-- ========================================
-- 2. 业务功能模块权限 (business域)
-- ========================================

-- 申报管理权限
UPDATE sys_menu SET permission = 'business:declaration:list' WHERE permission = 'business:declaration:list';
UPDATE sys_menu SET permission = 'business:declaration:statistics' WHERE permission = 'business:declaration:statistics';
UPDATE sys_menu SET permission = 'business:declaration:edit' WHERE permission = 'business:declaration:edit';
UPDATE sys_menu SET permission = 'business:declaration:view' WHERE permission = 'business:declaration:view';
UPDATE sys_menu SET permission = 'business:declaration:delete' WHERE permission = 'business:declaration:delete';
UPDATE sys_menu SET permission = 'business:declaration:audit' WHERE permission = 'business:declaration:audit';
UPDATE sys_menu SET permission = 'business:declaration:query' WHERE permission = 'business:declaration:query';
UPDATE sys_menu SET permission = 'business:declaration:add' WHERE permission = 'business:declaration:add';
UPDATE sys_menu SET permission = 'business:declaration:submit' WHERE permission = 'business:declaration:submit';
UPDATE sys_menu SET permission = 'business:declaration:export' WHERE permission = 'business:declaration:export';
UPDATE sys_menu SET permission = 'business:declaration:download' WHERE permission = 'business:declaration:download';
UPDATE sys_menu SET permission = 'business:declaration:contract' WHERE permission = 'business:declaration:contract';
UPDATE sys_menu SET permission = 'business:declaration:payment' WHERE permission = 'business:declaration:payment';
UPDATE sys_menu SET permission = 'business:declaration:pickup-submit' WHERE permission = 'business:declaration:pickup-submit';
UPDATE sys_menu SET permission = 'business:declaration:pickup-audit' WHERE permission = 'business:declaration:pickup-audit';
UPDATE sys_menu SET permission = 'business:declaration:pickup-delete' WHERE permission = 'business:declaration:pickup-delete';

-- 合同管理权限
UPDATE sys_menu SET permission = 'business:contract:template:query' WHERE permission = 'business:contract:template:query';
UPDATE sys_menu SET permission = 'business:contract:template:add' WHERE permission = 'business:contract:template:add';
UPDATE sys_menu SET permission = 'business:contract:template:update' WHERE permission = 'business:contract:template:update';
UPDATE sys_menu SET permission = 'business:contract:template:delete' WHERE permission = 'business:contract:template:delete';
UPDATE sys_menu SET permission = 'business:contract:template:upload' WHERE permission = 'business:contract:template:upload';
UPDATE sys_menu SET permission = 'business:contract:generate' WHERE permission = 'business:contract:generate';
UPDATE sys_menu SET permission = 'business:contract:generation:query' WHERE permission = 'business:contract:generation:query';
UPDATE sys_menu SET permission = 'business:contract:download' WHERE permission = 'business:contract:download';
UPDATE sys_menu SET permission = 'business:contract:generation:delete' WHERE permission = 'business:contract:generation:delete';
UPDATE sys_menu SET permission = 'business:contract:edit' WHERE permission = 'business:contract:edit';

-- 税务退费权限
UPDATE sys_menu SET permission = 'business:tax-refund:apply' WHERE permission = 'business:tax-refund:apply';
UPDATE sys_menu SET permission = 'business:tax-refund:list' WHERE permission = 'business:tax-refund:list';
UPDATE sys_menu SET permission = 'business:tax-refund:finance' WHERE permission = 'business:tax-refund:finance';
UPDATE sys_menu SET permission = 'business:tax-refund:approve' WHERE permission = 'business:tax-refund:approve';
UPDATE sys_menu SET permission = 'business:tax-refund:edit' WHERE permission = 'business:tax-refund:edit';
UPDATE sys_menu SET permission = 'business:tax-refund:detail' WHERE permission = 'business:tax-refund:detail';
UPDATE sys_menu SET permission = 'business:tax-refund:submit' WHERE permission = 'business:tax-refund:submit';

-- 提货单权限
UPDATE sys_menu SET permission = 'business:delivery-order:list' WHERE permission = 'business:delivery-order:list';
UPDATE sys_menu SET permission = 'business:delivery-order:add' WHERE permission = 'business:delivery-order:add';
UPDATE sys_menu SET permission = 'business:delivery-order:view' WHERE permission = 'business:delivery-order:view';

-- ========================================
-- 3. 工作流模块权限 (workflow域)
-- ========================================

-- 工作流定义权限
UPDATE sys_menu SET permission = 'workflow:definition:deploy' WHERE permission = 'workflow:definition:deploy';
UPDATE sys_menu SET permission = 'workflow:definition:list' WHERE permission = 'workflow:definition:list';
UPDATE sys_menu SET permission = 'workflow:definition:update' WHERE permission = 'workflow:definition:update';
UPDATE sys_menu SET permission = 'workflow:definition:delete' WHERE permission = 'workflow:definition:delete';
UPDATE sys_menu SET permission = 'workflow:definition:view' WHERE permission = 'workflow:definition:view';

-- 工作流实例权限
UPDATE sys_menu SET permission = 'workflow:instance:start' WHERE permission = 'workflow:instance:start';
UPDATE sys_menu SET permission = 'workflow:instance:list' WHERE permission = 'workflow:instance:list';
UPDATE sys_menu SET permission = 'workflow:instance:suspend' WHERE permission = 'workflow:instance:suspend';
UPDATE sys_menu SET permission = 'workflow:instance:activate' WHERE permission = 'workflow:instance:activate';
UPDATE sys_menu SET permission = 'workflow:instance:terminate' WHERE permission = 'workflow:instance:terminate';

-- 工作流任务权限
UPDATE sys_menu SET permission = 'workflow:task:list' WHERE permission = 'workflow:task:list';
UPDATE sys_menu SET permission = 'workflow:task:claim' WHERE permission = 'workflow:task:claim';
UPDATE sys_menu SET permission = 'workflow:task:complete' WHERE permission = 'workflow:task:complete';
UPDATE sys_menu SET permission = 'workflow:task:reject' WHERE permission = 'workflow:task:reject';
UPDATE sys_menu SET permission = 'workflow:task:transfer' WHERE permission = 'workflow:task:transfer';

-- 工作流监控权限
UPDATE sys_menu SET permission = 'workflow:monitor:view' WHERE permission = 'workflow:monitor:view';

-- ========================================
-- 4. 验证权限标准化结果
-- ========================================

-- 检查所有权限是否符合标准化格式
SELECT 
    '权限标准化验证' as validation_step,
    CASE 
        WHEN permission REGEXP '^(system|business|workflow):[a-z0-9-]+:[a-z0-9-]+$' THEN '符合标准格式(域:模块:操作)'
        WHEN permission REGEXP '^(system|business|workflow):[a-z0-9-]+:[a-z0-9-]+:[a-z0-9-]+$' THEN '符合标准格式(域:模块:功能:操作)'
        ELSE '不符合标准格式'
    END as format_check,
    menu_name,
    permission,
    menu_type
FROM sys_menu 
WHERE permission IS NOT NULL AND permission != ''
ORDER BY menu_type, permission;

-- ========================================
-- 5. 为管理员角色重新分配标准化后的权限
-- ========================================

-- 删除管理员现有权限
DELETE FROM sys_role_menu WHERE role_id = 1;

-- 为管理员分配所有标准化后的权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE permission IS NOT NULL AND permission != '';

-- ========================================
-- 6. 权限命名规范说明
-- ========================================

/*
权限命名标准化规范：

1. 格式：域:模块:功能:操作 或 域:模块:操作
   - 域(domain): system(系统管理)、business(业务功能)、workflow(工作流)
   - 模块(module): 使用小写字母和连字符，如user、role、declaration、contract
   - 功能(feature): 可选，如template、generation
   - 操作(action): 标准化词汇，如list、query、add、update、delete、view、edit等

2. 标准化词汇：
   - 查询类：list、query
   - 操作类：add、create、update、edit、delete、remove
   - 查看类：view、detail
   - 业务类：approve、submit、audit、download、upload、generate等

3. 示例：
   - 系统管理：system:user:add、system:role:list
   - 业务功能：business:declaration:edit、business:contract:generate
   - 工作流：workflow:task:complete、workflow:definition:deploy
*/