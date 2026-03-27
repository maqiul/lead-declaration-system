-- 财务补充功能按钮权限配置
-- 为申报管理模块添加财务补充相关的按钮权限

-- 获取申报管理菜单ID
SET @declaration_manage_id = (SELECT id FROM sys_menu WHERE menu_code = 'declaration_manage' AND menu_type = 2);

-- 添加财务补充相关按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    @declaration_manage_id, 
    '财务补充', 'declaration_finance_supplement', 3, '', '', 'business:declaration:financeSupplement', '', 12, 1
WHERE @declaration_manage_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'declaration_finance_supplement');

-- 为财务单证菜单添加更多按钮权限
-- 获取财务单证菜单ID
SET @finance_menu_id = (SELECT id FROM sys_menu WHERE menu_code = 'finance' AND menu_type = 2);

-- 如果财务单证菜单存在，为其添加按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    @finance_menu_id, 
    '财务单证保存', 'finance_save', 3, '', '', 'business:declaration:finance:save', '', 1, 1
WHERE @finance_menu_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'finance_save');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    @finance_menu_id, 
    '财务单证查看', 'finance_view', 3, '', '', 'business:declaration:finance:view', '', 2, 1
WHERE @finance_menu_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'finance_view');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) 
SELECT 
    @finance_menu_id, 
    '开票明细导出', 'finance_export', 3, '', '', 'business:declaration:finance:export', '', 3, 1
WHERE @finance_menu_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'finance_export');

-- 验证权限配置
SELECT 
    '财务补充权限配置' as config_section,
    m.menu_name,
    m.menu_code,
    m.permission,
    m.sort,
    CASE 
        WHEN p.menu_name IS NOT NULL THEN CONCAT('父菜单: ', p.menu_name)
        ELSE '顶级菜单'
    END as parent_info
FROM sys_menu m
LEFT JOIN sys_menu p ON m.parent_id = p.id
WHERE m.permission LIKE '%financeSupplement%' 
   OR m.permission LIKE '%finance:%'
   OR m.menu_code LIKE '%finance%'
ORDER BY m.parent_id, m.sort;

-- 为管理员角色分配新增的财务相关权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 
    1,  -- 管理员角色ID
    m.id, 
    NOW()
FROM sys_menu m
WHERE m.permission LIKE '%financeSupplement%' 
   OR m.permission LIKE '%finance:%'
   OR m.menu_code LIKE '%finance%'
AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm 
    WHERE rm.role_id = 1 AND rm.menu_id = m.id
)
ON DUPLICATE KEY UPDATE create_time = NOW();