-- 修复菜单配置脚本
-- 解决菜单路径与前端路由不匹配的问题

-- 1. 删除旧的不匹配菜单项
DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT id FROM sys_menu 
    WHERE menu_code IN ('demo', 'business', 'declaration')
       OR menu_code LIKE 'demo_%' 
       OR menu_code LIKE 'business_%' 
       OR menu_code LIKE 'declaration_%'
);

DELETE FROM sys_menu 
WHERE menu_code IN ('demo', 'business', 'declaration')
   OR menu_code LIKE 'demo_%' 
   OR menu_code LIKE 'business_%' 
   OR menu_code LIKE 'declaration_%';

-- 2. 重新插入正确的菜单配置

-- 插入演示功能根菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(0, '演示功能', 'demo', 1, '/demo', 'Layout', '', 'ExperimentOutlined', 5, 1);

-- 获取演示功能根菜单ID
SET @demo_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'demo' AND menu_type = 1);

-- 插入演示功能子菜单（与前端路由匹配）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@demo_parent_id, '发货清单', 'demo_shipping', 2, 'shipping-list', 'demo/simple-shipping-demo', 'demo:shipping:list', 'UnorderedListOutlined', 1, 1),
(@demo_parent_id, '申报表单', 'demo_declaration_form', 2, 'declaration-form', 'demo/declaration-form-demo', 'demo:declaration:form:list', 'FileTextOutlined', 2, 1),
(@demo_parent_id, '历史记录', 'demo_declaration_history', 2, 'declaration-history', 'demo/declaration-history', 'demo:declaration:history:list', 'HistoryOutlined', 3, 1);

-- 插入业务管理根菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(0, '业务管理', 'business', 1, '/business', 'Layout', '', 'ShoppingOutlined', 6, 1);

-- 获取业务管理根菜单ID
SET @business_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'business' AND menu_type = 1);

-- 插入业务管理子菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@business_parent_id, '财务审核', 'business_finance_review', 2, 'finance-review', 'business/tax-refund/finance-review', 'business:finance:review:list', 'AuditOutlined', 1, 1);

-- 插入出口申报根菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(0, '出口申报', 'declaration', 1, '/declaration', 'Layout', '', 'FileProtectOutlined', 7, 1);

-- 获取出口申报根菜单ID
SET @declaration_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'declaration' AND menu_type = 1);

-- 插入出口申报子菜单（与前端路由匹配）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@declaration_parent_id, '申报管理', 'declaration_manage', 2, 'manage', 'declaration/manage/index', 'declaration:manage:list', 'ContainerOutlined', 1, 1),
(@declaration_parent_id, '申报统计', 'declaration_statistics', 2, 'statistics', 'declaration/statistics/index', 'declaration:statistics:list', 'BarChartOutlined', 2, 1);

-- 3. 为管理员角色分配所有菜单权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE menu_code IN ('demo', 'business', 'declaration')
   OR menu_code LIKE 'demo_%' 
   OR menu_code LIKE 'business_%' 
   OR menu_code LIKE 'declaration_%'
   OR parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code IN ('demo', 'business', 'declaration')
   );

-- 4. 验证修复后的菜单配置
SELECT 
    m.id,
    m.menu_name,
    m.menu_code,
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
WHERE m.menu_code IN ('demo', 'business', 'declaration')
   OR m.menu_code LIKE 'demo_%' 
   OR m.menu_code LIKE 'business_%' 
   OR m.menu_code LIKE 'declaration_%'
   OR m.parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code IN ('demo', 'business', 'declaration')
   )
ORDER BY m.menu_type, m.sort;

-- 5. 显示菜单总数统计
SELECT 
    '演示功能' as module,
    COUNT(*) as menu_count
FROM sys_menu 
WHERE menu_code LIKE 'demo%'
UNION ALL
SELECT 
    '业务管理' as module,
    COUNT(*) as menu_count
FROM sys_menu 
WHERE menu_code LIKE 'business%'
UNION ALL
SELECT 
    '出口申报' as module,
    COUNT(*) as menu_count
FROM sys_menu 
WHERE menu_code LIKE 'declaration%';