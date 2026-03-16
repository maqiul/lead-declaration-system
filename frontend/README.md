# 线索申报系统前端

## 项目介绍

这是一个基于Vue 3 + TypeScript + Ant Design Vue的企业级管理系统前端，包含完整的RBAC权限控制和工作流引擎集成。

## 技术栈

- **框架**: Vue 3 + TypeScript
- **UI库**: Ant Design Vue 4.x
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **构建工具**: Vite 5
- **图标**: @ant-design/icons-vue

## 功能模块

### 🔐 权限管理系统
- 用户管理（增删改查、重置密码）
- 角色管理（权限分配、数据权限）
- 组织管理（树形架构）
- 菜单管理（动态路由）

### ⚙️ 工作流引擎
- 流程定义管理
- 流程实例监控
- 任务处理中心
- BPMN模型设计器

### 📊 系统监控
- 仪表板数据统计
- 实时图表展示
- 系统状态监控

## 开发环境

### 环境要求
- Node.js >= 16.0.0
- npm >= 8.0.0

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run dev
```

默认访问地址: http://localhost:3001

## 后端连接配置

### API代理配置
前端通过Vite代理连接后端服务：

```javascript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

### 后端服务地址
- **API基础路径**: http://localhost:8080
- **Swagger文档**: http://localhost:8080/doc.html
- **健康检查**: http://localhost:8080/actuator/health

### 启动后端服务
在项目根目录运行：
```bash
# Windows
backend\manage.bat

# Linux/Mac
backend/manage.sh
```

或者手动启动：
```bash
cd backend
mvn spring-boot:run
```

## 权限控制

### 指令权限
```vue
<!-- 按钮权限控制 -->
<a-button v-permission="['user:add']">新增用户</a-button>

<!-- 角色权限控制 -->
<div v-role="['admin', 'manager']">管理员可见内容</div>
```

### 函数权限
```typescript
import { hasPermission, hasRole } from '@/utils/permission'

// 检查权限
if (hasPermission('user:delete')) {
  // 执行删除操作
}

// 检查角色
if (hasRole('admin')) {
  // 管理员操作
}
```

## API接口

### 用户管理
- `POST /api/user/login` - 用户登录
- `GET /api/user/info` - 获取用户信息
- `GET /api/system/user/list` - 用户列表
- `POST /api/system/user` - 新增用户

### 角色管理
- `GET /api/system/role/list` - 角色列表
- `POST /api/system/role` - 新增角色
- `PUT /api/system/role/{id}/menus` - 角色权限配置

### 工作流
- `GET /api/workflow/definition/list` - 流程定义列表
- `POST /api/workflow/definition/deploy` - 部署流程
- `GET /api/workflow/task/list` - 任务列表

## 目录结构

```
src/
├── api/                 # API接口定义
│   ├── system.ts       # 系统管理接口
│   ├── user.ts         # 用户认证接口
│   └── workflow/       # 工作流接口
├── components/          # 公共组件
├── directives/          # 自定义指令
│   └── permission.ts   # 权限指令
├── layout/             # 页面布局
├── router/             # 路由配置
├── store/              # 状态管理
│   └── user.ts        # 用户状态
├── styles/             # 样式文件
├── utils/              # 工具函数
│   ├── permission.ts   # 权限工具
│   └── request.ts      # HTTP请求封装
├── views/              # 页面组件
│   ├── dashboard/      # 仪表板
│   ├── login/          # 登录页面
│   ├── system/         # 系统管理
│   └── workflow/       # 工作流
└── App.vue            # 根组件
```

## 构建部署

### 生产环境构建
```bash
npm run build
```

### 预览生产构建
```bash
npm run preview
```

### 代码检查
```bash
npm run lint
```

## 测试

项目包含完整的API测试页面，在系统管理 -> API测试中可以：
- 测试后端接口连通性
- 调试API请求和响应
- 快速验证接口功能

## 注意事项

1. 确保后端服务正常运行后再启动前端
2. 开发环境下API请求会自动代理到后端
3. 生产环境需要配置正确的API基础路径
4. 权限控制基于后端返回的用户权限数据

## 常见问题

### 1. API请求失败
- 检查后端服务是否启动
- 确认后端端口是否为8080
- 查看浏览器控制台网络请求详情

### 2. 权限控制不生效
- 确认用户已正确登录
- 检查后端返回的权限数据
- 验证权限标识是否正确

### 3. 页面白屏
- 检查控制台是否有JavaScript错误
- 确认依赖包是否安装完整
- 清除浏览器缓存后重试