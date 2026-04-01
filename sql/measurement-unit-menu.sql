-- 计量单位管理菜单配置 SQL
-- 用于在系统管理模块中添加计量单位管理功能

-- 1. 先获取系统管理父菜单 ID
SET @parent_menu_id = (SELECT id FROM sys_menu WHERE menu_name = '系统管理' LIMIT 1);

-- 2. 插入主菜单记录（menu_type=2 表示菜单）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_show`, `status`, `create_time`, `update_time`) 
VALUES 
(
    @parent_menu_id,
    '计量单位管理', 
    'measurement-unit',
    2,  -- menu_type: 2=菜单
    '/system/measurement-unit', 
    'system/measurement-unit/index',
    'system:measurement-unit:list',
    'DashboardOutlined', 
    100,
    1,
    1,
    NOW(), 
    NOW()
);

-- 3. 获取刚插入的菜单 ID
SET @measurement_unit_menu_id = LAST_INSERT_ID();

-- 4. 插入按钮权限配置（作为子菜单，menu_type=3 表示按钮）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_show`, `status`, `create_time`, `update_time`)
VALUES 
-- 查询按钮
(
    @measurement_unit_menu_id, 
    '计量单位查询', 
    'measurement-unit-query',
    3,  -- menu_type: 3=按钮
    '', 
    '',
    'system:measurement-unit:query',
    '', 
    1,
    0,
    1,
    NOW(), 
    NOW()
),
-- 新增按钮
(
    @measurement_unit_menu_id, 
    '计量单位新增', 
    'measurement-unit-add',
    3,  -- menu_type: 3=按钮
    '', 
    '',
    'system:measurement-unit:add',
    '', 
    2,
    0,
    1,
    NOW(), 
    NOW()
),
-- 编辑按钮
(
    @measurement_unit_menu_id, 
    '计量单位编辑', 
    'measurement-unit-edit',
    3,  -- menu_type: 3=按钮
    '', 
    '',
    'system:measurement-unit:edit',
    '', 
    3,
    0,
    1,
    NOW(), 
    NOW()
),
-- 删除按钮
(
    @measurement_unit_menu_id, 
    '计量单位删除', 
    'measurement-unit-delete',
    3,  -- menu_type: 3=按钮
    '', 
    '',
    'system:measurement-unit:delete',
    '', 
    4,
    0,
    1,
    NOW(), 
    NOW()
);

-- 5. 获取管理员角色 ID
SET @admin_role_id = (SELECT id FROM sys_role WHERE role_code = 'ADMIN' LIMIT 1);

-- 6. 将菜单和权限分配给管理员角色
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`)
SELECT 
    @admin_role_id,
    id,
    NOW()
FROM 
    sys_menu
WHERE 
    parent_id = @measurement_unit_menu_id 
    OR id = @measurement_unit_menu_id;

-- 7. 验证插入结果
SELECT 
    m.menu_name AS '菜单名称',
    m.path AS '路径',
    m.permission AS '权限标识'
FROM 
    sys_menu m
WHERE 
    m.parent_id = @measurement_unit_menu_id 
    OR m.id = @measurement_unit_menu_id
ORDER BY 
    m.sort;
