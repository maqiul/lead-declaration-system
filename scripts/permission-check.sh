#!/bin/bash
# 权限配置检查和修复脚本 (本地MySQL版本)
# 如果您使用Docker，请使用 docker-permission-check.sh 脚本

echo "=== 系统权限配置检查 ==="

# 1. 检查数据库连接
echo "1. 检查数据库连接..."
mysql -u root -p -e "SELECT 1;" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ 数据库连接失败，请检查MySQL服务和连接配置"
    exit 1
fi
echo "✅ 数据库连接正常"

# 2. 检查基础表结构
echo "2. 检查权限相关表结构..."
TABLES=("sys_menu" "sys_role" "sys_user" "sys_role_menu" "sys_user_role")
for table in "${TABLES[@]}"; do
    mysql -u root -p -e "DESCRIBE $table;" > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ 表 $table 存在"
    else
        echo "❌ 表 $table 不存在"
    fi
done

# 3. 检查管理员角色
echo "3. 检查管理员角色配置..."
ADMIN_COUNT=$(mysql -u root -p -e "SELECT COUNT(*) FROM sys_role WHERE role_code = 'ADMIN';" -s -N)
if [ "$ADMIN_COUNT" -gt 0 ]; then
    echo "✅ 管理员角色存在"
else
    echo "❌ 管理员角色不存在，需要创建"
fi

# 4. 检查基础菜单配置
echo "4. 检查基础菜单配置..."
MENU_COUNT=$(mysql -u root -p -e "SELECT COUNT(*) FROM sys_menu WHERE status = 1;" -s -N)
echo "✅ 当前启用菜单数量: $MENU_COUNT"

# 5. 检查权限分配情况
echo "5. 检查权限分配情况..."
ROLE_MENU_COUNT=$(mysql -u root -p -e "SELECT COUNT(*) FROM sys_role_menu;" -s -N)
echo "✅ 已分配权限数量: $ROLE_MENU_COUNT"

# 6. 生成权限检查报告
echo "6. 生成权限检查报告..."
mysql -u root -p << EOF > permission_check_report.txt
SELECT 
    '权限配置检查报告' as report_title,
    NOW() as check_time,
    '' as separator1,
    '角色权限统计' as section1,
    r.role_name,
    COUNT(rm.menu_id) as permission_count,
    CASE 
        WHEN COUNT(rm.menu_id) > 0 THEN '✓ 已配置权限'
        ELSE '✗ 未配置权限'
    END as status
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
GROUP BY r.id, r.role_name
ORDER BY r.id;

SELECT 
    '' as separator2,
    '菜单结构统计' as section2,
    CASE menu_type 
        WHEN 1 THEN '目录'
        WHEN 2 THEN '菜单'
        WHEN 3 THEN '按钮'
    END as menu_type_desc,
    COUNT(*) as count
FROM sys_menu 
WHERE status = 1
GROUP BY menu_type
ORDER BY menu_type;

SELECT 
    '' as separator3,
    '权限缺失检查' as section3,
    r.role_name,
    COUNT(m.id) - COUNT(rm.menu_id) as missing_permissions
FROM sys_role r
CROSS JOIN sys_menu m
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id AND m.id = rm.menu_id
WHERE m.status = 1 
  AND r.role_code IN ('ADMIN', 'USER', 'FINANCE')
GROUP BY r.id, r.role_name
HAVING missing_permissions > 0;
EOF

echo "✅ 权限检查报告已生成: permission_check_report.txt"

# 7. 提供修复建议
echo "7. 权限修复建议:"
echo "   - 如果发现权限缺失，执行: complete-permissions-config.sql"
echo "   - 如果需要重新分配管理员权限，执行: fix-admin-permissions.sql"
echo "   - 如果需要初始化基础数据，执行: complete-database-init.sql"

echo "=== 检查完成 ==="