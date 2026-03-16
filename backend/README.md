# 线索申报系统后端

基于 Spring Boot + MyBatis-Plus + Sa-Token + FastJSON2 + Swagger + Hutool 构建的企业级后台管理系统。

## 技术栈

### 后端技术栈
- **Spring Boot 3.1.5**: 核心框架
- **MyBatis-Plus 3.5.4**: ORM框架，简化数据库操作
- **Sa-Token 1.37.0**: 权限认证框架
- **FastJSON2 2.0.43**: JSON序列化框架
- **Knife4j 4.3.0**: OpenAPI 3文档生成工具
- **Hutool 5.8.22**: Java工具类库
- **Flowable 6.8.0**: 工作流引擎
- **MySQL 8.0**: 数据库
- **Redis**: 缓存和会话存储

## 项目结构

```
src/main/java/com/declaration
├── common          # 通用类
│   ├── Result.java     # 统一响应结果
│   └── PageParam.java  # 分页参数
├── config          # 配置类
│   ├── FastJson2Config.java      # FastJSON2配置
│   ├── GlobalExceptionHandler.java # 全局异常处理
│   ├── MybatisPlusConfig.java    # MyBatis-Plus配置
│   ├── MyMetaObjectHandler.java  # 自动填充配置
│   ├── SaTokenConfig.java        # Sa-Token配置
│   └── SwaggerConfig.java        # Swagger配置
├── controller      # 控制器层
│   ├── TestController.java       # 测试控制器
│   └── UserController.java       # 用户控制器
├── dao             # 数据访问层
│   └── UserDao.java              # 用户DAO
├── entity          # 实体类
│   ├── BaseEntity.java           # 基础实体类
│   └── User.java                 # 用户实体类
├── service         # 服务层
│   ├── impl                      # 服务实现
│   │   └── UserServiceImpl.java  # 用户服务实现
│   └── UserService.java          # 用户服务接口
└── LeadDeclarationApplication.java # 启动类
```

## 功能特性

### 🔐 权限认证
- 基于 Sa-Token 的 Token 认证机制
- 支持多端登录、权限控制
- 自动拦截未登录请求
- **细粒度权限控制**：支持到按钮级别的权限控制
- **角色权限管理**：灵活的角色分配和权限配置

### 🏢 组织管理
- **多层级组织架构**：支持无限层级的组织结构
- **数据权限控制**：基于组织的数据访问权限
- **用户组织关联**：支持用户归属多个组织

### 📊 ORM框架
- MyBatis-Plus 提供强大的 CRUD 操作
- 支持逻辑删除、自动填充
- 内置分页插件

### 前端技术栈
- **Vue 3.3.8**: 渐进式JavaScript框架
- **TypeScript 5.2**: JavaScript超集
- **Vite 5.0**: 新一代构建工具
- **Ant Design Vue 4.0.7**: Vue组件库
- **Vue Router 4.2.5**: Vue路由管理
- **Pinia 2.1.7**: Vue状态管理
- **Axios 1.6.0**: HTTP客户端

### 🔄 工作流引擎
- **Flowable集成**：完整的BPMN 2.0工作流引擎
- **流程设计器**：支持可视化流程设计
- **审批流程**：完整的审批流程管理
- **任务管理**：待办、已办、转办、委托等功能
- **历史追踪**：完整的流程执行历史记录

### 🎯 统一规范
- 统一响应格式 `Result<T>`
- 全局异常处理机制
- 参数校验和错误提示
- **注解式权限控制**：`@RequiresPermissions` 和 `@RequiresRoles`
- **动态菜单路由**：根据权限动态生成前端路由

## 快速开始

### 后端启动

#### 1. 环境准备
- JDK 17+
- MySQL 8.0+
- Redis 5.0+

### 2. 数据库配置
```sql
-- 执行基础数据库初始化脚本
source src/main/resources/db/init.sql

-- 执行权限管理模块脚本
source src/main/resources/db/permission.sql

-- 执行工作流模块脚本
source src/main/resources/db/workflow.sql
```

### 3. 修改配置
编辑 `application.yml` 文件：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lead_declaration
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
```

#### 4. 启动后端项目
```bash
cd backend
mvn spring-boot:run
```

#### 5. 访问测试
- 应用地址：http://localhost:8080
- API文档：http://localhost:8080/doc.html
- 测试接口：http://localhost:8080/test/hello

### 前端启动

#### 1. 安装依赖
```bash
cd frontend
npm install
```

#### 2. 启动开发服务器
```bash
npm run dev
```

#### 3. 访问前端
- 前端地址：http://localhost:3000
- 默认账户：admin/admin

## API接口示例

### 用户登录
```bash
POST /user/login
{
  "username": "admin",
  "password": "admin"
}
```

### 获取用户信息
```bash
GET /user/info
Header: satoken=your_token
```

### 分页查询用户
```bash
GET /user/page?current=1&size=10
```

### 获取用户路由菜单
```bash
GET /user/routes
Header: satoken=your_token
```

### 获取用户权限标识
```bash
GET /user/permissions
Header: satoken=your_token
```

### 组织管理接口
```bash
# 获取组织树
GET /org/tree

# 分页查询组织
GET /org/page?current=1&size=10

# 新增组织
POST /org
```

### 角色管理接口
```bash
# 查询角色列表
GET /role/list

# 分页查询角色
GET /role/page?current=1&size=10

# 分配角色给用户
POST /role/assign
{
  "userId": 1,
  "roleIds": [1, 2]
}
```

### 工作流接口
```bash
# 部署流程定义
POST /workflow/deploy

# 启动流程实例
POST /workflow/instance/start?processDefinitionKey=leave_process&businessKey=LEAVE_001

# 获取我的待办任务
GET /workflow/tasks/assigned

# 完成任务
POST /workflow/task/complete/TASK_ID

# 驳回任务
POST /workflow/task/reject/TASK_ID?targetActivityId=UserTask_1&reason=资料不全
```

## 默认账户
- 用户名：admin
- 密码：admin (MD5: 21232f297a57a5a743894a0e4a801fc3)

## 权限控制说明

### 注解式权限控制
```java
// 需要 user:add 权限
@RequiresPermissions("user:add")
@PostMapping
public Result<Boolean> addUser(@RequestBody User user) {
    // ...
}

// 需要 ADMIN 或 USER 角色
@RequiresRoles({"ADMIN", "USER"})
@GetMapping("/list")
public Result<List<User>> getUserList() {
    // ...
}

// 多个权限，OR逻辑（满足其一即可）
@RequiresPermissions(value = {"user:add", "user:update"}, logical = Logical.OR)
@PutMapping
public Result<Boolean> updateUser(@RequestBody User user) {
    // ...
}
```

### 权限标识规范
- **菜单权限**：`模块:功能` 如 `user:list`
- **按钮权限**：`模块:功能:操作` 如 `user:add`
- **常用权限**：
  - `query` - 查询
  - `add` - 新增  
  - `update` - 修改
  - `delete` - 删除
  - `export` - 导出
  - `import` - 导入

### 数据权限级别
1. **全部数据**：可查看系统所有数据
2. **本级数据**：仅可查看本组织数据
3. **本级及下级**：可查看本组织及下属组织数据
4. **自定义数据**：按自定义规则控制数据访问

## 开发规范

### 响应格式
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1647158400000
}
```

### 异常处理
- 使用 `@Valid` 进行参数校验
- 通过 `GlobalExceptionHandler` 统一处理异常
- 返回友好的错误信息

### 代码约定
- 实体类继承 `BaseEntity`
- 服务层实现 `IService<T>`
- 控制器统一返回 `Result<T>`
- 使用 Lombok 简化代码

## 扩展建议

1. **日志管理**：集成 ELK 或其他日志分析平台
2. **监控告警**：添加 Spring Boot Actuator 监控
3. **缓存优化**：合理使用 Redis 缓存热点数据
4. **安全加固**：添加 XSS、SQL注入防护
5. **性能优化**：数据库索引优化、连接池调优

## 注意事项

- 生产环境请修改默认密码
- 建议开启 HTTPS
- 定期备份数据库
- 监控系统运行状态