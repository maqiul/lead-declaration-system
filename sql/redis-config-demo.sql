-- Redis配置存储演示脚本
-- 这些命令可以直接在Redis客户端中执行

-- 1. 存储单个配置到Redis
SET system:config:system.name "线索申报系统"
SET system:config:system.version "1.0.0"
SET system:config:system.company "XXX科技有限公司"
SET system:config:ui.theme "#1890ff"
SET system:config:ui.language "zh-CN"

-- 2. 获取单个配置
GET system:config:system.name
GET system:config:system.version
GET system:config:ui.theme

-- 3. 批量存储配置 (使用MSET命令)
MSET system:config:demo.config1 "测试配置1" system:config:demo.config2 "测试配置2" system:config:demo.config3 "测试配置3"

-- 4. 批量获取配置 (使用MGET命令)
MGET system:config:demo.config1 system:config:demo.config2 system:config:demo.config3

-- 5. 查看所有系统配置键
KEYS system:config:*

-- 6. 删除特定配置
DEL system:config:demo.config1

-- 7. 检查配置是否存在
EXISTS system:config:system.name

-- 8. 设置配置过期时间 (例如3600秒后过期)
EXPIRE system:config:demo.config2 3600

-- 9. 查看配置剩余过期时间
TTL system:config:demo.config2

-- 10. 清空所有系统配置
-- KEYS system:config:* | xargs DEL
-- 注意：生产环境中谨慎使用此命令

-- 验证存储的数据
ECHO "=== 系统配置验证 ==="
GET system:config:system.name
GET system:config:system.version
GET system:config:system.company
GET system:config:ui.theme
GET system:config:ui.language