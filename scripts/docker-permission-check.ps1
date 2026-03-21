# Docker环境下权限配置检查和修复脚本 (PowerShell版本)

Write-Host "=== Docker环境权限配置检查 ===" -ForegroundColor Green

# 配置变量
$DOCKER_CONTAINER_NAME = "mysql-container"  # 请根据实际情况修改容器名
$MYSQL_USER = "root"
$MYSQL_PASSWORD = "your_password"  # 请修改为实际密码

# 1. 检查Docker容器状态
Write-Host "1. 检查MySQL容器状态..." -ForegroundColor Yellow
$containerStatus = docker ps --format "{{.Names}}" | Where-Object { $_ -eq $DOCKER_CONTAINER_NAME }

if ($containerStatus) {
    Write-Host "✅ MySQL容器 $DOCKER_CONTAINER_NAME 正在运行" -ForegroundColor Green
} else {
    Write-Host "❌ MySQL容器 $DOCKER_CONTAINER_NAME 未运行" -ForegroundColor Red
    Write-Host "请启动容器: docker start $DOCKER_CONTAINER_NAME" -ForegroundColor Yellow
    exit 1
}

# 2. 检查数据库连接
Write-Host "2. 检查数据库连接..." -ForegroundColor Yellow
try {
    $connectionTest = docker exec $DOCKER_CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -e "SELECT 1;" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 数据库连接正常" -ForegroundColor Green
    } else {
        Write-Host "❌ 数据库连接失败，请检查密码和网络配置" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ 数据库连接测试失败: $_" -ForegroundColor Red
    exit 1
}

# 3. 检查权限相关表结构
Write-Host "3. 检查权限相关表结构..." -ForegroundColor Yellow
$tables = @("sys_menu", "sys_role", "sys_user", "sys_role_menu", "sys_user_role")

foreach ($table in $tables) {
    try {
        $result = docker exec $DOCKER_CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -e "DESCRIBE $table;" 2>&1
        if ($result -match "doesn't exist") {
            Write-Host "❌ 表 $table 不存在" -ForegroundColor Red
        } else {
            Write-Host "✅ 表 $table 存在" -ForegroundColor Green
        }
    } catch {
        Write-Host "❌ 检查表 $table 时出错: $_" -ForegroundColor Red
    }
}

# 4. 检查管理员角色
Write-Host "4. 检查管理员角色配置..." -ForegroundColor Yellow
try {
    $adminCount = docker exec $DOCKER_CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -e "SELECT COUNT(*) FROM sys_role WHERE role_code = 'ADMIN';" -s -N 2>$null
    if ($adminCount -and [int]$adminCount -gt 0) {
        Write-Host "✅ 管理员角色存在 (数量: $adminCount)" -ForegroundColor Green
    } else {
        Write-Host "❌ 管理员角色不存在或查询失败" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ 检查管理员角色时出错: $_" -ForegroundColor Red
}

# 5. 检查基础菜单配置
Write-Host "5. 检查基础菜单配置..." -ForegroundColor Yellow
try {
    $menuCount = docker exec $DOCKER_CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -e "SELECT COUNT(*) FROM sys_menu WHERE status = 1;" -s -N 2>$null
    if ($menuCount) {
        Write-Host "✅ 当前启用菜单数量: $menuCount" -ForegroundColor Green
    } else {
        Write-Host "❌ 无法获取菜单数量" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ 检查菜单配置时出错: $_" -ForegroundColor Red
}

# 6. 检查权限分配情况
Write-Host "6. 检查权限分配情况..." -ForegroundColor Yellow
try {
    $roleMenuCount = docker exec $DOCKER_CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD -e "SELECT COUNT(*) FROM sys_role_menu;" -s -N 2>$null
    if ($roleMenuCount) {
        Write-Host "✅ 已分配权限数量: $roleMenuCount" -ForegroundColor Green
    } else {
        Write-Host "❌ 无法获取权限分配数量" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ 检查权限分配时出错: $_" -ForegroundColor Red
}

# 7. 生成权限检查报告
Write-Host "7. 生成权限检查报告..." -ForegroundColor Yellow
$reportFile = "docker_permission_check_report_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"

$query = @"
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
"@

$query | docker exec -i $DOCKER_CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD > $reportFile

if (Test-Path $reportFile) {
    Write-Host "✅ 权限检查报告已生成: $reportFile" -ForegroundColor Green
} else {
    Write-Host "❌ 生成报告失败" -ForegroundColor Red
}

# 8. 提供Docker环境修复命令
Write-Host "8. Docker环境修复命令:" -ForegroundColor Yellow
Write-Host "   # 进入MySQL容器" -ForegroundColor Cyan
Write-Host "   docker exec -it $DOCKER_CONTAINER_NAME bash" -ForegroundColor White
Write-Host ""
Write-Host "   # 执行权限配置脚本" -ForegroundColor Cyan
Write-Host "   mysql -uroot -p < /path/to/complete-permissions-config.sql" -ForegroundColor White
Write-Host ""
Write-Host "   # 或者直接在宿主机执行" -ForegroundColor Cyan
Write-Host "   docker exec -i $DOCKER_CONTAINER_NAME mysql -uroot -p$MYSQL_PASSWORD < sql\complete-permissions-config.sql" -ForegroundColor White
Write-Host ""
Write-Host "   # 检查特定表" -ForegroundColor Cyan
Write-Host "   docker exec $DOCKER_CONTAINER_NAME mysql -uroot -p$MYSQL_PASSWORD -e `"SELECT * FROM sys_menu LIMIT 5;`"" -ForegroundColor White

# 9. 快速修复选项
Write-Host "9. 快速修复选项:" -ForegroundColor Yellow
Write-Host "   [1] 执行完整权限配置" -ForegroundColor White
Write-Host "   [2] 重新分配管理员权限" -ForegroundColor White
Write-Host "   [3] 初始化基础数据" -ForegroundColor White
Write-Host "   [4] 退出" -ForegroundColor White
Write-Host ""
$choice = Read-Host "请选择操作 (输入数字)"

switch ($choice) {
    "1" {
        Write-Host "执行完整权限配置..." -ForegroundColor Yellow
        if (Test-Path "sql\fixed-permissions-config.sql") {
            Get-Content "sql\fixed-permissions-config.sql" | docker exec -i $DOCKER_CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD
            Write-Host "✅ 权限配置完成" -ForegroundColor Green
        } elseif (Test-Path "sql\complete-permissions-config.sql") {
            Get-Content "sql\complete-permissions-config.sql" | docker exec -i $DOCKER_CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD
            Write-Host "✅ 权限配置完成（使用原始版本）" -ForegroundColor Yellow
        } else {
            Write-Host "❌ 找不到权限配置文件" -ForegroundColor Red
        }
    }
    "2" {
        Write-Host "重新分配管理员权限..." -ForegroundColor Yellow
        if (Test-Path "sql\fix-admin-permissions.sql") {
            Get-Content "sql\fix-admin-permissions.sql" | docker exec -i $DOCKER_CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD
            Write-Host "✅ 管理员权限重新分配完成" -ForegroundColor Green
        } else {
            Write-Host "❌ 找不到管理员权限修复文件" -ForegroundColor Red
        }
    }
    "3" {
        Write-Host "初始化基础数据..." -ForegroundColor Yellow
        if (Test-Path "sql\complete-database-init.sql") {
            Get-Content "sql\complete-database-init.sql" | docker exec -i $DOCKER_CONTAINER_NAME mysql -u$MYSQL_USER -p$MYSQL_PASSWORD
            Write-Host "✅ 基础数据初始化完成" -ForegroundColor Green
        } else {
            Write-Host "❌ 找不到基础数据初始化文件" -ForegroundColor Red
        }
    }
    "4" {
        Write-Host "退出检查程序" -ForegroundColor Yellow
        exit 0
    }
    default {
        Write-Host "无效选择" -ForegroundColor Red
    }
}

Write-Host "=== Docker环境检查完成 ===" -ForegroundColor Green