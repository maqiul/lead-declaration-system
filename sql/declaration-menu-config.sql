-- 出口申报单菜单配置
-- 为申报单功能添加完整的菜单项和权限配置

-- 1. 插入申报单根菜单（目录类型）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(0, '出口申报', 'declaration', 1, '/declaration', 'Layout', '', 'FileTextOutlined', 4, 1);

-- 获取刚插入的申报单根菜单ID
SET @declaration_parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'declaration' AND menu_type = 1);

-- 2. 插入申报单子菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@declaration_parent_id, '申报表单', 'declaration_form', 2, 'form', 'demo/declaration-form-demo', 'declaration:form:list', 'FormOutlined', 1, 1),
(@declaration_parent_id, '历史记录', 'declaration_history', 2, 'history', 'demo/declaration-history', 'declaration:history:list', 'HistoryOutlined', 2, 1);

-- 3. 插入申报单表单操作权限（按钮类型）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
((SELECT id FROM sys_menu WHERE menu_code = 'declaration_form'), '表单查询', 'declaration_form_query', 3, '', '', 'declaration:form:query', '', 1, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'declaration_form'), '表单新增', 'declaration_form_add', 3, '', '', 'declaration:form:add', '', 2, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'declaration_form'), '表单修改', 'declaration_form_update', 3, '', '', 'declaration:form:update', '', 3, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'declaration_form'), '表单删除', 'declaration_form_delete', 3, '', '', 'declaration:form:delete', '', 4, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'declaration_form'), '表单导出', 'declaration_form_export', 3, '', '', 'declaration:form:export', '', 5, 1);

-- 4. 插入历史记录操作权限（按钮类型）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
((SELECT id FROM sys_menu WHERE menu_code = 'declaration_history'), '历史查询', 'declaration_history_query', 3, '', '', 'declaration:history:query', '', 1, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'declaration_history'), '历史详情', 'declaration_history_detail', 3, '', '', 'declaration:history:detail', '', 2, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'declaration_history'), '历史导出', 'declaration_history_export', 3, '', '', 'declaration:history:export', '', 3, 1);

-- 5. 为管理员角色分配申报单相关权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE menu_code IN ('declaration', 'declaration_form', 'declaration_history')
   OR menu_code LIKE 'declaration_%'
   OR parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code IN ('declaration_form', 'declaration_history')
   );

-- 6. 验证菜单配置结果
SELECT 
    m.id,
    m.menu_name,
    m.menu_code,
    m.menu_type,
    CASE m.menu_type 
        WHEN 1 THEN '目录'
        WHEN 2 THEN '菜单'
        WHEN 3 THEN '按钮'
    END as menu_type_desc,
    m.path,
    m.component,
    m.permission,
    m.sort,
    p.menu_name as parent_name
FROM sys_menu m
LEFT JOIN sys_menu p ON m.parent_id = p.id
WHERE m.menu_code LIKE 'declaration%' 
   OR m.parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code = 'declaration'
   )
ORDER BY m.menu_type, m.sort;

-- 7. 验证管理员权限分配
SELECT 
    r.role_name,
    COUNT(rm.menu_id) as declaration_menu_count,
    GROUP_CONCAT(m.menu_name ORDER BY m.menu_type, m.sort) as menus
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
LEFT JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.id = 1 
  AND (m.menu_code LIKE 'declaration%' 
       OR m.parent_id IN (
           SELECT id FROM sys_menu WHERE menu_code = 'declaration'
       ))
GROUP BY r.id, r.role_name;