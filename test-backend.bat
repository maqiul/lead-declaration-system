@echo off
echo 测试后端API连接...

echo.
echo 1. 测试健康检查接口:
curl -s http://localhost:8080/actuator/health
echo.

echo.
echo 2. 测试登录接口:
curl -X POST http://localhost:8080/user/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin\"}"
echo.

echo.
echo 3. 测试Swagger文档访问:
curl -I http://localhost:8080/doc.html
echo.

echo 测试完成！
pause