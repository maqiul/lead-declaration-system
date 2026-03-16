@echo off
echo 测试登录功能...

echo.
echo 1. 测试健康检查:
curl -s http://localhost:8080/actuator/health
echo.

echo.
echo 2. 测试登录接口 (正确密码):
curl -X POST http://localhost:8080/user/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
echo.

echo.
echo 3. 测试登录接口 (错误密码):
curl -X POST http://localhost:8080/user/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"wrongpassword\"}"
echo.

echo.
echo 4. 测试CORS预检请求:
curl -H "Origin: http://localhost:3000" ^
  -H "Access-Control-Request-Method: POST" ^
  -H "Access-Control-Request-Headers: Content-Type" ^
  -X OPTIONS http://localhost:8080/user/login
echo.

echo 测试完成！
pause