-- =============================================
-- 检查并修复500错误
-- 执行日期: 2026-04-21
-- =============================================

-- 1. 检查sys_role_menu表的字段名
SHOW COLUMNS FROM sys_role_menu;

-- 2. 检查ID 1060和6011是否已存在
SELECT id, menu_name, menu_code, parent_id 
FROM sys_menu 
WHERE id IN (1060, 6011);

-- 3. 检查首页菜单状态
SELECT id, menu_name, menu_code, parent_id, menu_type, is_show, status, deleted
FROM sys_menu 
WHERE id IN (1, 2);

-- 4. 检查申报单管理父菜单ID
SELECT id, menu_name, menu_code, parent_id, menu_type
FROM sys_menu 
WHERE menu_code = 'declaration' OR menu_name LIKE '%申报%'
ORDER BY id
LIMIT 10;

-- 5. 检查水单管理父菜单ID
SELECT id, menu_name, menu_code, parent_id, menu_type
FROM sys_menu 
WHERE menu_code = 'remittance' OR menu_name LIKE '%水单%'
ORDER BY id
LIMIT 10;
