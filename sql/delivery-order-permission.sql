-- ========================================
-- 提货单菜单权限配置脚本
-- 生成时间: 2026-03-24
-- 说明: 配置提货单管理相关的菜单和按钮权限
-- ========================================

SET NAMES utf8mb4;

-- ========================================
-- 第一步：插入提货单管理菜单（作为申报管理的子菜单）
-- 参考：出口申报目录 parent_id=200，申报管理 id=201
-- ========================================

-- 使用存储过程实现幂等插入
DROP PROCEDURE IF EXISTS add_delivery_order_menu;

DELIMITER //

CREATE PROCEDURE add_delivery_order_menu()
BEGIN
    DECLARE delivery_menu_id BIGINT DEFAULT 0;
    
    -- 检查是否已存在提货单管理菜单
    SELECT id INTO delivery_menu_id FROM sys_menu 
    WHERE menu_code = 'declaration-delivery-order' AND deleted = 0 LIMIT 1;
    
    -- 如果不存在，则插入提货单管理菜单
    IF delivery_menu_id = 0 OR delivery_menu_id IS NULL THEN
        -- 插入二级菜单：提货单管理 (parent_id=200 出口申报)
        INSERT INTO sys_menu (menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) 
        VALUES ('提货单管理', 'declaration-delivery-order', 200, 2, 'delivery-order', '@/views/declaration/delivery-order/index.vue', 'business:declaration:deliveryOrder', 'CarOutlined', 5, 0, 0, 1, 1, 0, NOW(), NOW());
        
        SET delivery_menu_id = LAST_INSERT_ID();
        
        -- 插入三级按钮权限
        INSERT INTO sys_menu (menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
        ('提货单查询', 'delivery-order-list', delivery_menu_id, 3, NULL, NULL, 'business:declaration:deliveryOrder:list', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
        ('提货单新增', 'delivery-order-add', delivery_menu_id, 3, NULL, NULL, 'business:declaration:deliveryOrder:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
        ('提货单编辑', 'delivery-order-edit', delivery_menu_id, 3, NULL, NULL, 'business:declaration:deliveryOrder:edit', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
        ('提货单删除', 'delivery-order-delete', delivery_menu_id, 3, NULL, NULL, 'business:declaration:deliveryOrder:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
        ('提货单审核', 'delivery-order-audit', delivery_menu_id, 3, NULL, NULL, 'business:declaration:deliveryOrder:audit', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW()),
        ('提货单查看', 'delivery-order-view', delivery_menu_id, 3, NULL, NULL, 'business:declaration:deliveryOrder:view', NULL, 6, 0, 0, 1, 1, 0, NOW(), NOW());
        
        SELECT CONCAT('提货单菜单创建成功! 菜单ID: ', delivery_menu_id) AS message;
    ELSE
        SELECT CONCAT('提货单菜单已存在，跳过创建。菜单ID: ', delivery_menu_id) AS message;
    END IF;
END //

DELIMITER ;

-- 执行存储过程
CALL add_delivery_order_menu();

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_delivery_order_menu;

-- ========================================
-- 第二步：为管理员角色分配提货单权限
-- ========================================

-- 为管理员角色(role_id=1)分配所有提货单相关权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW() 
FROM sys_menu 
WHERE menu_code LIKE 'delivery-order%' 
   OR menu_code = 'declaration-delivery-order'
   AND deleted = 0 AND status = 1;

-- 为财务角色(role_id=4)分配提货单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 4, id, NOW() 
FROM sys_menu 
WHERE menu_code LIKE 'delivery-order%' 
   OR menu_code = 'declaration-delivery-order'
   AND deleted = 0 AND status = 1;

-- 为销售员角色(role_id=5)分配提货单查看权限（不含审核权限）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 5, id, NOW() 
FROM sys_menu 
WHERE (menu_code = 'declaration-delivery-order'
   OR menu_code IN ('delivery-order-list', 'delivery-order-add', 'delivery-order-view'))
   AND deleted = 0 AND status = 1;

-- ========================================
-- 第三步：验证权限配置结果
-- ========================================

-- 查看提货单相关菜单
SELECT 
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
    p.menu_name as parent_name
FROM sys_menu m
LEFT JOIN sys_menu p ON m.parent_id = p.id
WHERE m.menu_code LIKE 'delivery-order%' 
   OR m.menu_code = 'declaration-delivery-order'
ORDER BY m.menu_type, m.sort;

-- 查看角色权限分配情况
SELECT 
    r.role_name,
    COUNT(rm.menu_id) as delivery_order_menu_count,
    GROUP_CONCAT(m.menu_name ORDER BY m.menu_type, m.sort) as menus
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
LEFT JOIN sys_menu m ON rm.menu_id = m.id
WHERE m.menu_code LIKE 'delivery-order%' 
   OR m.menu_code = 'declaration-delivery-order'
GROUP BY r.id, r.role_name
ORDER BY r.id;

-- ========================================
-- 完成提示
-- ========================================
SELECT '提货单权限配置完成!' AS message;
