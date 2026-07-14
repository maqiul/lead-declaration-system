# 权限命名标准化实施指南

## 1. 概述

本文档详细介绍了权限命名标准化的背景、规范、实施步骤和验证方法，旨在解决系统中权限命名混乱的问题。

## 2. 当前问题分析

### 2.1 存在的问题
- 权限命名格式不统一（有些是两段式，有些是三段式或四段式）
- 域（domain）命名不一致（如 `user:add` vs `system:bank-account:add`）
- 操作类型词汇不统一（如 `list` vs `query`）
- 缺乏统一的命名规范文档

### 2.2 影响
- 增加维护成本
- 容易造成权限配置错误
- 降低开发效率
- 影响系统的可扩展性

## 3. 标准化规范

### 3.1 命名格式
```
域:模块:功能:操作    # 适用于复杂的业务操作
域:模块:操作        # 适用于简单的系统操作
```

### 3.2 域（Domain）分类
- `system`: 系统管理功能（用户、角色、菜单、组织等）
- `business`: 业务功能（申报、合同、税务退费等）
- `workflow`: 工作流功能（流程定义、任务、监控等）

### 3.3 模块命名规范
- 使用小写字母
- 多词之间用连字符分隔
- 简洁明了，能准确表达模块功能
- 例如：`user`, `role`, `declaration`, `contract`, `tax-refund`

### 3.4 操作类型标准词汇

#### 3.4.1 查询类
- `list`: 列表查询
- `query`: 详细查询
- `view`: 查看详情
- `detail`: 查看详情（与view同义）

#### 3.4.2 操作类
- `add`: 新增
- `create`: 创建（与add同义）
- `update`: 更新
- `edit`: 编辑（与update同义）
- `delete`: 删除
- `remove`: 移除（与delete同义）

#### 3.4.3 业务类
- `submit`: 提交
- `approve`: 审批
- `audit`: 审核
- `export`: 导出
- `import`: 导入
- `download`: 下载
- `upload`: 上传
- `generate`: 生成

## 4. 实施步骤

### 4.1 准备阶段
1. 备份数据库
2. 确认当前权限使用情况
3. 通知相关开发人员

### 4.2 数据库层面
1. 执行权限标准化SQL脚本
2. 验证权限映射表的准确性

### 4.3 后端代码层面
1. 更新所有 `@RequiresPermissions` 注解
2. 更新相关的权限验证逻辑
3. 测试后端权限控制功能

### 4.4 前端代码层面
1. 更新所有权限指令和函数调用
2. 更新前端权限配置
3. 测试前端权限控制功能

## 5. 验证方法

### 5.1 数据库验证
```sql
-- 检查权限格式是否符合规范
SELECT 
    menu_name,
    permission,
    CASE 
        WHEN permission REGEXP '^(system|business|workflow):[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$' THEN '✓ 格式正确'
        WHEN permission REGEXP '^(system|business|workflow):[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$' THEN '✓ 格式正确'
        ELSE '✗ 格式错误'
    END as validation_result
FROM sys_menu 
WHERE permission IS NOT NULL AND permission != '';
```

### 5.2 功能验证
1. 测试不同角色的权限分配
2. 验证权限控制功能是否正常
3. 确认菜单显示和按钮显示是否正确

## 6. 常见问题及解决方案

### 6.1 权限变更后无法访问
- 检查角色权限分配是否正确
- 确认用户权限缓存是否已清除
- 验证后端注解是否已更新

### 6.2 前端权限控制失效
- 检查前端权限指令是否已更新
- 确认权限标识是否与后端一致
- 验证权限API返回的数据格式

## 7. 维护建议

### 7.1 新增权限规范
- 严格按照命名规范创建新权限
- 在权限映射表中记录新增权限
- 更新相关文档

### 7.2 定期检查
- 定期检查权限命名是否符合规范
- 审查不再使用的权限
- 更新权限使用统计

## 8. 参考资源

- 全量权限注册表（现行脚本）：`sql/migration/11-full-permission-registry.sql`
- SQL 脚本总索引：`sql/README.md`
- 权限统计分析：通过SQL查询获取

> 注：早期的 `permission-naming-standardization.sql` / `permission-reference-table.sql` 已不在仓库中，
> 权限相关变更现统一由 `sql/migration/11-full-permission-registry.sql` 维护。