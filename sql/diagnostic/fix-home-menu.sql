-- =============================================
-- 检查并修复首页菜单不显示问题
-- 执行日期: 2026-04-21
-- =============================================

-- 1. 先检查首页菜单是否存在
SELECT id, menu_name, menu_code, parent_id, menu_type, path, status, deleted 
FROM sys_menu 
WHERE id IN (1, 2) OR menu_name = '首页';

-- 2. 检查ID 1025是否已存在
SELECT id, menu_name, menu_code, parent_id 
FROM sys_menu 
WHERE id = 1025;

-- 3. 检查ID 6011是否已存在
SELECT id, menu_name, menu_code, parent_id 
FROM sys_menu 
WHERE id = 6011;

-- 4. 检查申报单管理菜单的ID
SELECT id, menu_name, menu_code, parent_id 
FROM sys_menu 
WHERE menu_code LIKE '%declaration%' 
ORDER BY id;

-- 5. 检查水单管理菜单的ID
SELECT id, menu_name, menu_code, parent_id 
FROM sys_menu 
WHERE menu_code LIKE '%remittance%' 
ORDER BY id;

-- 6. 修复首页菜单（如果被意外修改）
UPDATE sys_menu 
SET menu_name = '首页', 
    menu_code = NULL,
    parent_id = 0,
    menu_type = 1,
    path = '/',
    component = 'Layout',
    permission = NULL,
    icon = 'HomeOutlined',
    sort = 1,
    is_show = 1,
    status = 1,
    deleted = 0,
    update_time = NOW()
WHERE id = 1;

UPDATE sys_menu 
SET menu_name = '首页', 
    menu_code = NULL,
    parent_id = 0,
    menu_type = 2,
    path = '/dashboard',
    component = '@/views/dashboard/simple.vue',
    permission = NULL,
    icon = 'HomeOutlined',
    sort = 0,
    is_show = 1,
    status = 1,
    deleted = 0,
    update_time = NOW()
WHERE id = 2;

-- 7. 验证修复结果
SELECT id, menu_name, menu_code, parent_id, menu_type, path, is_show, status, deleted
FROM sys_menu
WHERE id IN (1, 2)
ORDER BY id;
