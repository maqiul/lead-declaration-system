# Flowable Modeler 集成指南

## 概述

本文档详细介绍了如何在现有的线索申报系统中集成 Flowable Modeler，实现可视化的流程设计功能。

## 技术架构

### 后端技术栈
- Spring Boot 3.1.5
- Flowable 6.8.0
- MySQL 8.0
- Redis (用于会话管理)

### 前端技术栈
- Vue 3 + TypeScript
- Ant Design Vue 4.0.7
- Vite 5.0.0

## 集成步骤

### 1. 后端集成

#### 1.1 添加依赖
在 `pom.xml` 中添加 Flowable UI 相关依赖：

```xml
<!-- Flowable UI 相关依赖 -->
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-ui-modeler-rest</artifactId>
    <version>6.8.0</version>
</dependency>
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-ui-modeler-conf</artifactId>
    <version>6.8.0</version>
</dependency>
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-ui-common</artifactId>
    <version>6.8.0</version>
</dependency>
```

#### 1.2 配置文件更新
在 `application.yml` 中添加 Flowable UI 配置：

```yaml
flowable:
  # 基础配置
  async-executor-activate: false
  database-schema-update: true
  history-level: full
  mail-server-host: localhost
  mail-server-port: 50025
  
  # UI Modeler 配置
  rest-api-enabled: true
  # 允许匿名访问UI（开发环境）
  common:
    app:
      security:
        csrf:
          disabled: true
```

#### 1.3 创建 Modeler 控制器
创建 `ModelerController.java` 提供以下功能：
- 模型列表查询
- 模型创建/编辑/删除
- 模型部署
- 模型详情查看

主要 API 接口：
```
GET    /modeler/models          # 获取模型列表
POST   /modeler/model           # 创建新模型
GET    /modeler/model/{id}      # 获取模型详情
PUT    /modeler/model/{id}      # 更新模型
DELETE /modeler/model/{id}      # 删除模型
POST   /modeler/model/{id}/deploy # 部署模型
```

### 2. 前端集成

#### 2.1 创建页面组件
在 `src/views/workflow/modeler/index.vue` 中实现模型管理界面，包含：
- 模型列表展示（表格形式）
- 搜索过滤功能
- 分页功能
- 模型 CRUD 操作
- 模型部署功能

#### 2.2 创建 API 接口
在 `src/api/workflow/modeler.ts` 中定义与后端交互的 API 方法。

#### 2.3 路由配置
在 `src/router/index.ts` 中添加模型管理路由：

```javascript
{
  path: 'modeler',
  name: 'Modeler',
  component: () => import('@/views/workflow/modeler/index.vue'),
  meta: { title: '模型设计', icon: 'EditOutlined' }
}
```

## 功能特性

### 1. 模型管理
- ✅ 模型列表展示（分页、搜索）
- ✅ 模型创建、编辑、删除
- ✅ 模型详情查看
- ✅ 模型版本管理

### 2. 流程设计
- ✅ 基于 Flowable 的 BPMN 2.0 标准
- ✅ 可视化流程设计器
- ✅ 支持各种流程节点类型
- ✅ 流程验证和预览

### 3. 部署功能
- ✅ 一键部署流程模型
- ✅ 部署历史记录
- ✅ 部署状态监控

### 4. 权限控制
- ✅ 基于角色的访问控制
- ✅ 操作权限验证
- ✅ 数据隔离

## 使用说明

### 1. 创建流程模型
1. 进入"工作流" → "模型设计"
2. 点击"创建模型"按钮
3. 填写模型基本信息（名称、Key、描述）
4. 点击确定完成创建

### 2. 设计流程
1. 在模型列表中点击"编辑"进入设计器
2. 使用拖拽方式添加流程节点
3. 配置节点属性和连线条件
4. 保存设计内容

### 3. 部署流程
1. 在模型列表中找到目标模型
2. 点击"部署"按钮
3. 确认部署信息
4. 部署成功后可在流程定义中查看

## 注意事项

### 1. 环境要求
- JDK 17+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+

### 2. 配置要点
- 确保数据库连接正确
- Redis 服务正常运行
- Flowable 相关表已初始化

### 3. 安全考虑
- 生产环境应关闭匿名访问
- 配置适当的 CSRF 保护
- 实施严格的权限控制

## 扩展建议

### 1. 功能增强
- 添加流程模板管理
- 支持流程导入/导出
- 实现流程版本对比
- 增加流程性能监控

### 2. 用户体验
- 添加流程图预览功能
- 实现多语言支持
- 优化移动端适配
- 增加操作引导

### 3. 集成扩展
- 与现有业务系统深度集成
- 支持更多流程引擎
- 实现流程市场功能
- 添加 AI 辅助设计能力

## 故障排除

### 常见问题
1. **模型无法保存**：检查数据库连接和权限
2. **部署失败**：验证流程定义的合法性
3. **页面加载异常**：确认前端依赖安装完整
4. **权限验证失败**：检查用户角色和权限配置

### 调试建议
- 查看后端日志获取详细错误信息
- 使用浏览器开发者工具检查网络请求
- 验证 API 接口返回的数据格式
- 确认前后端版本兼容性

## 维护计划

### 短期维护（1-3个月）
- 监控系统运行稳定性
- 收集用户反馈并优化
- 修复发现的 Bug
- 完善文档和示例

### 长期规划（3-12个月）
- 性能优化和扩展
- 新功能开发
- 架构升级
- 安全加固

---

*本文档最后更新：2026年3月13日*