-- 验证税务退费权限配置脚本

-- 检查税务退费相关菜单权限
SELECT 
    r.role_name,
    m.menu_name,
    m.menu_code,
    m.permission,
    m.menu_type
FROM sys_role_menu rm
JOIN sys_menu m ON rm.menu_id = m.id
JOIN sys_role r ON rm.role_id = r.id
WHERE m.menu_code LIKE 'system_tax_refund%'
    AND r.role_name IN ('管理员', '财务')
ORDER BY r.role_name, m.sort;

-- 检查具体的审核权限是否存在
SELECT 
    '审核权限检查' as check_type,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM sys_menu 
            WHERE menu_code = 'system_tax_refund_approve' 
            AND permission = 'system:tax-refund:approve'
        ) THEN '✓ 审核权限存在'
        ELSE '✗ 审核权限缺失'
    END as approve_permission,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM sys_menu 
            WHERE menu_code = 'system_tax_refund_first_review' 
            AND permission = 'system:tax-refund:first-review'
        ) THEN '✓ 财务初审权限存在'
        ELSE '✗ 财务初审权限缺失'
    END as first_review_permission,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM sys_menu 
            WHERE menu_code = 'system_tax_refund_final_review' 
            AND permission = 'system:tax-refund:final-review'
        ) THEN '✓ 财务复审权限存在'
        ELSE '✗ 财务复审权限缺失'
    END as final_review_permission;

-- 检查财务角色是否拥有审核权限
SELECT 
    r.role_name,
    COUNT(CASE WHEN m.permission = 'system:tax-refund:approve' THEN 1 END) as has_approve,
    COUNT(CASE WHEN m.permission = 'system:tax-refund:first-review' THEN 1 END) as has_first_review,
    COUNT(CASE WHEN m.permission = 'system:tax-refund:final-review' THEN 1 END) as has_final_review
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
LEFT JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.role_name = '财务'
GROUP BY r.role_name;