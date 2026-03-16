# 线索申报系统后端

## 项目简介
基于Spring Boot 3.1.5 + MyBatis-Plus + Sa-Token构建的线索申报系统后端服务。

## 技术栈
- Spring Boot 3.1.5
- MyBatis-Plus 3.5.4
- Sa-Token 1.37.0 (权限认证)
- FastJSON2 2.0.43
- Hutool 5.8.22
- MySQL 8.0.33
- Lombok

## 修复说明
本次修复解决了以下问题：
1. ✅ 更新了Jakarta EE相关的包导入（javax -> jakarta）
2. ✅ 移除了有问题的Swagger/Knife4j依赖
3. ✅ 简化了Sa-Token Redis配置，使用内存存储模式
4. ✅ 修复了Controller中的方法签名问题
5. ✅ 清理了不必要的复杂依赖

## 快速开始

### 环境要求
- Java 17+
- MySQL 8.0+
- Maven 3.6+ (可选，IDE内置即可)

### 数据库配置
1. 创建MySQL数据库：`lead_declaration`
2. 执行初始化脚本：`src/main/resources/db/init.sql`

### 配置文件
修改 `src/main/resources/application.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lead_declaration?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
    username: your_username
    password: your_password
```

### 运行项目
```bash
# 方式1：使用IDE运行
运行 LeadDeclarationApplication.java

# 方式2：使用Maven命令
mvn spring-boot:run

# 方式3：打包运行
mvn clean package
java -jar target/lead-declaration-system-1.0.0.jar
```

## API接口
项目启动后访问：
- API基础路径：`http://localhost:8080`
- 健康检查：`GET /actuator/health`

## 主要功能模块
- 用户管理（登录、注册、权限控制）
- 组织机构管理
- 角色权限管理
- 菜单路由管理
- 工作流引擎集成

## 注意事项
1. 当前版本移除了Redis依赖，使用Sa-Token的内存存储模式
2. 如需恢复Redis支持，请添加相应的Redis依赖配置
3. Swagger文档功能暂时移除，后续可重新集成

## 后续优化建议
- [ ] 重新集成Redis支持
- [ ] 恢复API文档功能
- [ ] 添加更多业务功能
- [ ] 完善单元测试
- [ ] 添加监控和日志配置