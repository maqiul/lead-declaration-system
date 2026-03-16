@echo off
setlocal enabledelayedexpansion

:: 后端服务管理脚本 (Windows版本)
set PROJECT_DIR=d:\lead-declaration-system\backend
set BACKEND_PORT=8080

echo === 线索申报系统后端服务管理 ===
echo.

:MENU
echo 请选择操作:
echo 1. 检查后端服务状态
echo 2. 启动后端服务
echo 3. 停止后端服务
echo 4. 重启后端服务
echo 5. 打开Swagger文档
echo 6. 退出
echo.

set /p choice=请输入选项 (1-6): 

if "%choice%"=="1" goto CHECK_STATUS
if "%choice%"=="2" goto START_BACKEND
if "%choice%"=="3" goto STOP_BACKEND
if "%choice%"=="4" goto RESTART_BACKEND
if "%choice%"=="5" goto OPEN_SWAGGER
if "%choice%"=="6" goto EXIT
goto INVALID_CHOICE

:CHECK_STATUS
echo 检查后端服务状态...
powershell -Command "& {try {Invoke-WebRequest -Uri 'http://localhost:%BACKEND_PORT%/actuator/health' -TimeoutSec 5 | Select-Object StatusCode; exit 0} catch {exit 1}}"
if %ERRORLEVEL%==0 (
    echo ✅ 后端服务正在运行
    echo Swagger文档地址: http://localhost:%BACKEND_PORT%/doc.html
) else (
    echo ❌ 后端服务未运行
)
echo.
pause
goto MENU

:START_BACKEND
echo 启动后端服务...
cd /d %PROJECT_DIR%

:: 检查是否已有进程在运行
tasklist | findstr "java.exe" >nul
if %ERRORLEVEL%==0 (
    echo 后端服务可能已在运行
    goto MENU
)

:: 启动服务
start "Backend Service" cmd /c "mvn spring-boot:run"
echo 后端服务启动中...

:: 等待服务启动
timeout /t 15 /nobreak >nul

:: 检查启动状态
powershell -Command "& {try {Invoke-WebRequest -Uri 'http://localhost:%BACKEND_PORT%/actuator/health' -TimeoutSec 5; exit 0} catch {exit 1}}"
if %ERRORLEVEL%==0 (
    echo ✅ 后端服务启动成功
    echo Swagger文档地址: http://localhost:%BACKEND_PORT%/doc.html
    echo API基础路径: http://localhost:%BACKEND_PORT%
) else (
    echo ❌ 后端服务启动可能失败，请检查日志
)
echo.
pause
goto MENU

:STOP_BACKEND
echo 停止后端服务...
taskkill /f /im java.exe 2>nul
if %ERRORLEVEL%==0 (
    echo ✅ 后端服务已停止
) else (
    echo ❌ 没有找到运行中的Java进程
)
echo.
pause
goto MENU

:RESTART_BACKEND
echo 重启后端服务...
call :STOP_BACKEND
timeout /t 3 /nobreak >nul
call :START_BACKEND
goto MENU

:OPEN_SWAGGER
echo 检查后端服务状态...
powershell -Command "& {try {Invoke-WebRequest -Uri 'http://localhost:%BACKEND_PORT%/actuator/health' -TimeoutSec 5; exit 0} catch {exit 1}}"
if %ERRORLEVEL%==0 (
    echo 打开Swagger文档...
    start http://localhost:%BACKEND_PORT%/doc.html
) else (
    echo 后端服务未运行，请先启动服务
)
echo.
pause
goto MENU

:INVALID_CHOICE
echo 无效选项，请重新选择
echo.
goto MENU

:EXIT
echo 退出管理工具
exit /b 0