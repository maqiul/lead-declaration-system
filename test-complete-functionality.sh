#!/bin/bash
# 申报系统完整功能测试脚本

echo "=== 出口申报系统功能测试 ==="

# 1. 检查后端服务是否运行
echo "1. 检查后端服务..."
curl -s http://localhost:8080/actuator/health > /dev/null
if [ $? -eq 0 ]; then
    echo "✓ 后端服务运行正常"
else
    echo "✗ 后端服务未运行，请启动后端服务"
    exit 1
fi

# 2. 检查前端服务是否运行
echo "2. 检查前端服务..."
curl -s http://localhost:3000 > /dev/null
if [ $? -eq 0 ]; then
    echo "✓ 前端服务运行正常"
else
    echo "✗ 前端服务未运行，请启动前端服务"
    exit 1
fi

# 3. 测试数据库连接
echo "3. 测试数据库连接..."
# 这里可以添加具体的数据库连接测试

# 4. 测试API接口
echo "4. 测试申报单保存接口..."
curl -X POST http://localhost:8080/api/declaration/form/save \
  -H "Content-Type: application/json" \
  -d '{
    "shipperCompany": "TEST COMPANY",
    "shipperAddress": "TEST ADDRESS",
    "consigneeCompany": "CONSIGNEE COMPANY",
    "consigneeAddress": "CONSIGNEE ADDRESS",
    "invoiceNo": "TEST-2026-001",
    "declarationDate": "2026-03-13",
    "transportMode": "TRUCK",
    "departureCity": "SHANGHAI, CHINA",
    "destinationRegion": "USA",
    "currency": "USD",
    "totalQuantity": 1000,
    "totalAmount": 1000.00,
    "totalCartons": 10,
    "totalGrossWeight": 50.00,
    "totalNetWeight": 45.00,
    "totalVolume": 1.000,
    "status": 0
  }' | jq '.'

# 5. 测试查询接口
echo "5. 测试申报单查询接口..."
curl -s "http://localhost:8080/api/declaration/form/list?pageNum=1&pageSize=10" | jq '.'

# 6. 测试HS商品接口
echo "6. 测试HS商品接口..."
curl -s "http://localhost:8080/api/system/product/enabled" | jq '.'

echo "=== 测试完成 ==="
echo "请在浏览器中访问 http://localhost:3000 进行功能验证"