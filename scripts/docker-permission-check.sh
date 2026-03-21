#!/bin/bash
# Docker环境下权限配置检查和修复脚本

echo "=== Docker环境权限配置检查 ==="

# 配置变量
DOCKER_CONTAINER_NAME="mysql-container"  # 请根据实际情况修改容器名
MYSQL_USER="root"
MYSQL_PASSWORD="your_password"  # 请修改为实际密码
MYSQL_HOST="localhost"
MYSQL_PORT="3306"

# 1. 检查Docker容器状态
echo "1. 检查MySQL容器状态..."
if docker ps | grep -q "$DOCKER_CONTAINER_NAME"; then
    echo "✅ MySQL容器 $DOCKER_CONTAINER_NAME 正在运行"
else
    echo "❌ MySQL容器 $DOCKER_CONTAINER_NAME 未运行"
    echo "请启动容器: docker start $DOCKER_CONTAINER_NAME"
    exit 1
fi

# 2. 检查数据库连接
echo "2. 检查数据库连接..."
docker exec "$DOCKER_CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ 数据库连接正常"
else
    echo "❌ 数据库连接失败，请检查密码和网络配置"
    exit 1
fi

# 3. 检查权限相关表结构
echo "3. 检查权限相关表结构..."
TABLES=("sys_menu" "sys_role" "sys_user" "sys_role_menu" "sys_user_role")
for table in "${TABLES[@]}"; do
    RESULT=$(docker exec "$DOCKER_CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "DESCRIBE $table;" 2>&1)
    if [[ $RESULT == *"doesn't exist"* ]]; then
        echo "❌ 表 $table 不存在"
    else
        echo "✅ 表 $table 存在"
    fi
done

# 4. 检查管理员角色
echo "4. 检查管理员角色配置..."
ADMIN_COUNT=$(docker exec "$DOCKER_CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT COUNT(*) FROM sys_role WHERE role_code = 'ADMIN';" -s -N 2>/dev/null)
if [ -n "$ADMIN_COUNT" ] && [ "$ADMIN_COUNT" -gt 0 ]; then
    echo "✅ 管理员角色存在 (数量: $ADMIN_COUNT)"
else
    echo "❌ 管理员角色不存在或查询失败"
fi

# 5. 检查基础菜单配置
echo "5. 检查基础菜单配置..."
MENU_COUNT=$(docker exec "$DOCKER_CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT COUNT(*) FROM sys_menu WHERE status = 1;" -s -N 2>/dev/null)
if [ -n "$MENU_COUNT" ]; then
    echo "✅ 当前启用菜单数量: $MENU_COUNT"
else
    echo "❌ 无法获取菜单数量"
fi

# 6. 检查权限分配情况
echo "6. 检查权限分配情况..."
ROLE_MENU_COUNT=$(docker exec "$DOCKER_CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT COUNT(*) FROM sys_role_menu;" -s -N 2>/dev/null)
if [ -n "$ROLE_MENU_COUNT" ]; then
    echo "✅ 已分配权限数量: $ROLE_MENU_COUNT"
else
    echo "❌ 无法获取权限分配数量"
fi

# 7. 生成权限检查报告
echo "7. 生成权限检查报告..."
REPORT_FILE="docker_permission_check_report_$(date +%Y%m%d_%H%M%S).txt"

docker exec "$DOCKER_CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" << 'EOF' > "$REPORT_FILE"
SELECT 
    'Docker环境权限配置检查报告' as report_title,
    NOW() as check_time,
    '' as separator1,
    '角色权限统计' as section1,
    r.role_name,
    r.role_code,
    COUNT(rm.menu_id) as permission_count,
    CASE 
        WHEN COUNT(rm.menu_id) > 0 THEN '✓ 已配置权限'
        ELSE '✗ 未配置权限'
    END as status
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
GROUP BY r.id, r.role_name, r.role_code
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
    '关键权限检查' as section3,
    m.menu_name,
    m.menu_code,
    m.permission,
    CASE 
        WHEN rm.menu_id IS NOT NULL THEN '✓ 管理员有权限'
        ELSE '✗ 管理员无权限'
    END as admin_permission
FROM sys_menu m
LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id AND rm.role_id = 1
WHERE m.menu_code IN (
    'business_declaration_list',
    'business_tax_refund_apply',
    'system_config',
    'system_user'
)
ORDER BY m.menu_type, m.sort;
EOF

echo "✅ 权限检查报告已生成: $REPORT_FILE"

# 8. 提供Docker环境修复命令
echo "8. Docker环境修复命令:"
echo "   # 进入MySQL容器"
echo "   docker exec -it $DOCKER_CONTAINER_NAME bash"
echo ""
echo "   # 执行权限配置脚本"
echo "   mysql -uroot -p < /path/to/complete-permissions-config.sql"
echo ""
echo "   # 或者直接在宿主机执行"
echo "   docker exec -i $DOCKER_CONTAINER_NAME mysql -uroot -p$MYSQL_PASSWORD < sql/complete-permissions-config.sql"
echo ""
echo "   # 检查特定表"
echo "   docker exec $DOCKER_CONTAINER_NAME mysql -uroot -p$MYSQL_PASSWORD -e \"SELECT * FROM sys_menu LIMIT 5;\""
echo ""

# 9. 快速修复选项
echo "9. 快速修复选项:"
echo "   [1] 执行完整权限配置"
echo "   [2] 重新分配管理员权限"  
echo "   [3] 初始化基础数据"
echo "   [4] 退出"
echo ""
echo "请选择操作 (输入数字): "
read -r choice

case $choice in
    1)
        echo "执行完整权限配置..."
        if [ -f "sql/complete-permissions-config.sql" ]; then
            docker exec -i "$DOCKER_CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" < sql/complete-permissions-config.sql
            echo "✅ 权限配置完成"
        else
            echo "❌ 找不到权限配置文件: sql/complete-permissions-config.sql"
        fi
        ;;
    2)
        echo "重新分配管理员权限..."
        if [ -f "sql/fix-admin-permissions.sql" ]; then
            docker exec -i "$DOCKER_CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" < sql/fix-admin-permissions.sql
            echo "✅ 管理员权限重新分配完成"
        else
            echo "❌ 找不到管理员权限修复文件"
        fi
        ;;
    3)
        echo "初始化基础数据..."
        if [ -f "sql/complete-database-init.sql" ]; then
            docker exec -i "$DOCKER_CONTAINER_NAME" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" < sql/complete-database-init.sql
            echo "✅ 基础数据初始化完成"
        else
            echo "❌ 找不到基础数据初始化文件"
        fi
        ;;
    4)
        echo "退出检查程序"
        exit 0
        ;;
    *)
        echo "无效选择"
        ;;
esac

echo "=== Docker环境检查完成 ==="