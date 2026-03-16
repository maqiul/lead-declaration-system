#!/bin/bash

# 后端服务管理脚本

PROJECT_DIR="/d/lead-declaration-system/backend"
BACKEND_PORT=8080

echo "=== 线索申报系统后端服务管理 ==="

# 检查后端服务状态
check_backend_status() {
    echo "检查后端服务状态..."
    if curl -s http://localhost:$BACKEND_PORT/actuator/health > /dev/null 2>&1; then
        echo "✅ 后端服务正在运行"
        curl -s http://localhost:$BACKEND_PORT/actuator/health | jq '.'
        return 0
    else
        echo "❌ 后端服务未运行"
        return 1
    fi
}

# 启动后端服务
start_backend() {
    echo "启动后端服务..."
    cd $PROJECT_DIR
    
    # 检查是否已有进程在运行
    if pgrep -f "spring-boot:run" > /dev/null; then
        echo "后端服务已在运行"
        return 0
    fi
    
    # 启动服务
    mvn spring-boot:run &
    BACKEND_PID=$!
    echo "后端服务启动中，PID: $BACKEND_PID"
    
    # 等待服务启动
    echo "等待服务启动..."
    sleep 10
    
    # 检查启动状态
    if check_backend_status; then
        echo "✅ 后端服务启动成功"
        echo "Swagger文档地址: http://localhost:$BACKEND_PORT/doc.html"
        echo "API基础路径: http://localhost:$BACKEND_PORT"
    else
        echo "❌ 后端服务启动失败"
        echo "请检查日志输出和配置"
    fi
}

# 停止后端服务
stop_backend() {
    echo "停止后端服务..."
    pkill -f "spring-boot:run"
    sleep 2
    if ! pgrep -f "spring-boot:run" > /dev/null; then
        echo "✅ 后端服务已停止"
    else
        echo "❌ 后端服务停止失败"
    fi
}

# 重启后端服务
restart_backend() {
    echo "重启后端服务..."
    stop_backend
    sleep 3
    start_backend
}

# 显示菜单
show_menu() {
    echo ""
    echo "请选择操作:"
    echo "1. 检查后端服务状态"
    echo "2. 启动后端服务"
    echo "3. 停止后端服务"
    echo "4. 重启后端服务"
    echo "5. 打开Swagger文档"
    echo "6. 退出"
    echo ""
}

# 主循环
while true; do
    show_menu
    read -p "请输入选项 (1-6): " choice
    
    case $choice in
        1)
            check_backend_status
            ;;
        2)
            start_backend
            ;;
        3)
            stop_backend
            ;;
        4)
            restart_backend
            ;;
        5)
            if check_backend_status; then
                echo "打开Swagger文档..."
                start http://localhost:$BACKEND_PORT/doc.html 2>/dev/null || \
                xdg-open http://localhost:$BACKEND_PORT/doc.html 2>/dev/null || \
                echo "请手动打开浏览器访问: http://localhost:$BACKEND_PORT/doc.html"
            else
                echo "后端服务未运行，请先启动服务"
            fi
            ;;
        6)
            echo "退出管理工具"
            exit 0
            ;;
        *)
            echo "无效选项，请重新选择"
            ;;
    esac
    
    echo ""
    read -p "按回车键继续..."
done