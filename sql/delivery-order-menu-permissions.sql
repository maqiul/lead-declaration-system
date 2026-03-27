-- 提货单菜单权限配置脚本
-- 为提货单管理功能添加完整的菜单和按钮权限

-- 1. 添加提货单主菜单（如果不存在）
INSERT IGNORE INTO sys_menu (
    parent_id, 
    menu_name, 
    menu_code, 
    menu_type, 
    path, 
    component, 
    permission, 
    icon, 
    sort, 
    status
) VALUES
(200, '提货单管理', 'declaration_delivery_order', 2, 'delivery-order', 'declaration/delivery-order/index', 'business:declaration:deliveryOrder', 'CarOutlined', 6, 1);

-- 2. 获取提货单菜单ID
SET @delivery_order_menu_id = (SELECT id FROM sys_menu WHERE menu_code = 'declaration_delivery_order' AND menu_type = 2);

-- 3. 添加提货单相关按钮权限
INSERT IGNORE INTO sys_menu (
    parent_id, 
    menu_name, 
    menu_code, 
    menu_type, 
    path, 
    component, 
    permission, 
    icon, 
    sort, 
    status
) VALUES
(@delivery_order_menu_id, '提货单查询', 'delivery_order_list', 3, NULL, NULL, 'business:declaration:deliveryOrder', NULL, 1, 1),
(@delivery_order_menu_id, '提货单新增', 'delivery_order_add', 3, NULL, NULL, 'business:declaration:deliveryOrder:add', NULL, 2, 1),
(@delivery_order_menu_id, '提货单编辑', 'delivery_order_edit', 3, NULL, NULL, 'business:declaration:deliveryOrder:edit', NULL, 3, 1),
(@delivery_order_menu_id, '提货单删除', 'delivery_order_delete', 3, NULL, NULL, 'business:declaration:deliveryOrder:delete', NULL, 4, 1),
(@delivery_order_menu_id, '提货单审核', 'delivery_order_audit', 3, NULL, NULL, 'business:declaration:deliveryOrder:audit', NULL, 5, 1);

-- 4. 为管理员角色分配所有提货单相关权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE menu_code IN (
    'declaration_delivery_order',
    'delivery_order_list',
    'delivery_order_add', 
    'delivery_order_edit',
    'delivery_order_delete',
    'delivery_order_audit'
)
OR parent_id = @delivery_order_menu_id;

-- 5. 验证提货单权限配置
SELECT 
    '提货单权限配置结果' as config_section,
    m.id,
    m.menu_name,
    m.menu_code,
    CASE m.menu_type 
        WHEN 1 THEN '目录'
        WHEN 2 THEN '菜单'
        WHEN 3 THEN '按钮'
    END as menu_type_desc,
    m.permission,
    m.sort,
    p.menu_name as parent_menu
FROM sys_menu m
LEFT JOIN sys_menu p ON m.parent_id = p.id
WHERE m.menu_code LIKE 'delivery_order_%' 
   OR m.menu_code = 'declaration_delivery_order'
ORDER BY m.menu_type, m.sort;