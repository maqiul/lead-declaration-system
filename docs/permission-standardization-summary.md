# 权限命名标准化总结

## 核心规范

### 命名格式
```
域:模块:功能:操作    # 四段式（推荐用于复杂业务）
域:模块:操作        # 三段式（推荐用于简单操作）
```

### 域（Domain）分类
- `system`: 系统管理功能（用户、角色、菜单、组织等）
- `business`: 业务功能（申报、合同、税务退费等）
- `workflow`: 工作流功能（流程定义、任务、监控等）

### 标准词汇表
| 类型 | 词汇 | 说明 |
|------|------|------|
| 查询 | `list`, `query` | 列表查询、详细查询 |
| 操作 | `add`, `update`, `delete` | 新增、更新、删除 |
| 查看 | `view`, `detail` | 查看详情 |
| 业务 | `submit`, `approve`, `audit` | 提交、审批、审核 |
| 文件 | `export`, `import`, `download`, `upload` | 导出、导入、下载、上传 |

## 示例对照

| 旧权限 | 新权限 | 说明 |
|--------|--------|------|
| `user:add` | `system:user:add` | 添加域前缀 |
| `user:list` | `system:user:list` | 添加域前缀 |
| `business:contract:template:query` | `business:contract:template:query` | 已符合规范 |
| `system:bank-account:view` | `system:bank-account:view` | 已符合规范 |

## 实施要点

1. **数据库层面**：执行SQL脚本更新权限标识
2. **后端层面**：更新所有 `@RequiresPermissions` 注解
3. **前端层面**：更新权限指令和函数调用
4. **验证测试**：确保权限控制功能正常

## 关键文件

- `sql/migration/11-full-permission-registry.sql` - 全量权限注册表脚本（替代早期已移除的 permission-naming-standardization.sql / permission-reference-table.sql）
- `sql/README.md` - SQL 脚本总索引
- `docs/permission-standardization-guide.md` - 实施指南
- `docs/permission-standardization-checklist.md` - 检查清单