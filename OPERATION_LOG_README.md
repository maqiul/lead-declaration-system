# 操作日志系统使用说明

## 📋 功能介绍

操作日志系统是一个完整的用户行为跟踪和审计功能，能够记录用户在系统中的各种操作行为。

## 🚀 核心功能

### 1. 自动记录功能
- 自动捕获所有带有`@OperationLog`注解的接口调用
- 记录用户信息、操作类型、业务类型、请求参数等
- 自动获取客户端IP、浏览器、操作系统等信息
- 记录操作耗时和执行状态

### 2. 手动记录功能
可以通过在Controller方法上添加注解来启用日志记录：

```java
@OperationLog(
    operationType = "CREATE", 
    businessType = "申报管理",
    saveRequestData = true,
    saveResponseData = true
)
@PostMapping("/create")
public Result<?> createDeclaration(@RequestBody DeclarationForm form) {
    // 业务逻辑
    return Result.success();
}
```

### 3. 日志查询和管理
- 支持分页查询操作日志
- 支持按用户、操作类型、业务类型等条件筛选
- 支持时间段查询
- 支持日志详情查看和删除

### 4. 自动清理机制
- 定时任务每天凌晨2点自动清理过期日志
- 默认保留30天的日志记录
- 可通过配置文件调整保留天数

## 📁 文件结构

```
backend/
├── entity/
│   └── OperationLog.java          # 操作日志实体类
├── dao/
│   └── OperationLogMapper.java    # Mapper接口
├── service/
│   ├── OperationLogService.java   # Service接口
│   └── impl/
│       └── OperationLogServiceImpl.java  # Service实现
├── controller/
│   └── OperationLogController.java       # 控制器
├── interceptor/
│   └── OperationLogInterceptor.java      # 拦截器
├── annotation/
│   └── OperationLog.java          # 日志注解
├── util/
│   └── ServletUtils.java          # Servlet工具类
├── task/
│   └── OperationLogCleanTask.java # 清理任务
├── config/
│   └── WebConfig.java             # Web配置
└── resources/
    └── application.yml            # 配置文件

frontend/
└── src/
    └── api/
        └── system/
            └── operation-log.ts   # 前端API接口
```

## ⚙️ 配置说明

在`application.yml`中添加以下配置：

```yaml
# 系统日志配置
system:
  log:
    # 日志保留天数
    keep-days: 30
    # 是否开启操作日志
    enabled: true
```

## 🛠️ 数据库表结构

执行`sql/create_operation_log_table.sql`创建数据表：

```sql
CREATE TABLE `sys_operation_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` bigint DEFAULT NULL COMMENT '用户ID',
    `username` varchar(50) DEFAULT NULL COMMENT '用户名',
    `operation_type` varchar(50) NOT NULL COMMENT '操作类型',
    `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型',
    `method` varchar(10) DEFAULT NULL COMMENT '请求方法',
    `request_url` varchar(255) DEFAULT NULL COMMENT '请求URL',
    `request_params` text COMMENT '请求参数',
    `response_result` text COMMENT '响应结果',
    `ip_address` varchar(50) DEFAULT NULL COMMENT '操作IP地址',
    `location` varchar(255) DEFAULT NULL COMMENT '操作地点',
    `browser` varchar(50) DEFAULT NULL COMMENT '浏览器类型',
    `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
    `status` tinyint(1) DEFAULT '0' COMMENT '操作状态（0成功 1失败）',
    `error_msg` text COMMENT '错误消息',
    `cost_time` bigint DEFAULT NULL COMMENT '操作耗时（毫秒）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

## 🔧 操作类型定义

常用的业务操作类型：

- `LOGIN`: 用户登录
- `LOGOUT`: 用户登出
- `CREATE`: 创建操作
- `UPDATE`: 更新操作
- `DELETE`: 删除操作
- `QUERY`: 查询操作
- `EXPORT`: 导出操作
- `IMPORT`: 导入操作
- `AUDIT`: 审核操作
- `GENERATE`: 生成操作

## 📊 API接口

### 后端接口

1. `GET /operation-log/page` - 分页查询操作日志
2. `GET /operation-log/{id}` - 查询日志详情
3. `DELETE /operation-log/{id}` - 删除单条日志
4. `DELETE /operation-log/batch` - 批量删除日志
5. `DELETE /operation-log/clean` - 清理过期日志
6. `GET /operation-log/stats` - 获取统计信息

### 前端API

```typescript
import {
  getOperationLogs,
  getOperationLogById,
  deleteOperationLog,
  deleteOperationLogs,
  cleanExpiredLogs,
  getOperationLogStats
} from '@/api/system/operation-log'
```

## 🎯 使用场景

1. **安全审计** - 追踪敏感操作，防范安全风险
2. **问题排查** - 通过操作日志快速定位问题原因
3. **行为分析** - 分析用户使用习惯，优化产品体验
4. **合规要求** - 满足行业监管和合规审计要求
5. **性能监控** - 监控接口性能，识别慢查询

## 🔒 注意事项

1. 敏感信息（如密码）不应记录在日志中
2. 大文件上传等操作建议不记录请求参数
3. 定期清理过期日志，避免占用过多存储空间
4. 生产环境建议调整日志级别，避免记录过多调试信息

## 📈 扩展建议

1. 添加日志导出功能（Excel/PDF格式）
2. 实现日志实时推送（WebSocket）
3. 添加更丰富的统计分析功能
4. 集成ELK等日志分析平台
5. 实现异常操作告警机制