# 申报单功能正式目录迁移说明

## 迁移概述
将申报单相关功能从demo目录迁移到正式的业务目录结构中。

## 目录结构调整

### 原目录结构
```
src/views/demo/
├── declaration-form-demo.vue     # 申报表单
├── declaration-history.vue       # 历史记录
└── ... (其他演示文件)
```

### 新目录结构
```
src/views/declaration/
├── form/
│   └── index.vue                 # 申报表单（原declaration-form-demo.vue）
└── history/
    └── index.vue                 # 历史记录（原declaration-history.vue）
```

## 文件迁移详情

### 1. 申报表单文件迁移
- **源文件**: `src/views/demo/declaration-form-demo.vue`
- **目标文件**: `src/views/declaration/form/index.vue`
- **路由路径**: `/declaration/form`
- **组件名称**: `DeclarationForm`

### 2. 历史记录文件迁移
- **源文件**: `src/views/demo/declaration-history.vue`
- **目标文件**: `src/views/declaration/history/index.vue`
- **路由路径**: `/declaration/history`
- **组件名称**: `DeclarationHistoryFormal`

## 菜单配置更新

### SQL脚本位置
`d:\lead-declaration-system\sql\formal-declaration-menu.sql`

### 主要更新内容
1. 更新菜单路径指向正式目录
2. 更新组件路径配置
3. 保持权限标识不变
4. 添加缺失的菜单项
5. 确保管理员权限正确分配

## 路由配置更新

### 更新文件
`src/router/index.ts`

### 新增路由配置
```javascript
{
  path: '/declaration',
  component: Layout,
  name: 'Declaration',
  meta: { title: '出口申报', icon: 'FileProtectOutlined' },
  children: [
    {
      path: 'form',
      name: 'DeclarationForm',
      component: () => import('@/views/declaration/form/index.vue'),
      meta: { title: '申报表单', icon: 'FormOutlined' }
    },
    {
      path: 'history',
      name: 'DeclarationHistoryFormal',
      component: () => import('@/views/declaration/history/index.vue'),
      meta: { title: '历史记录', icon: 'HistoryOutlined' }
    }
  ]
}
```

## 执行步骤

### 1. 数据库菜单更新
```sql
-- 执行菜单配置脚本
source d:/lead-declaration-system/sql/formal-declaration-menu.sql
```

### 2. 验证迁移结果
- 检查新目录文件是否存在
- 验证数据库菜单配置
- 测试页面访问功能

### 3. 清理工作（可选）
- 删除demo目录中的旧文件
- 更新相关引用链接

## 注意事项

1. **兼容性**: 保留demo目录中的文件以确保向后兼容
2. **权限**: 确保管理员角色拥有新菜单的访问权限
3. **测试**: 迁移后需要全面测试功能完整性
4. **备份**: 执行前建议备份数据库和重要文件

## 验证清单

- [ ] 文件迁移完成
- [ ] 数据库菜单配置更新
- [ ] 路由配置生效
- [ ] 页面访问正常
- [ ] 权限控制正确
- [ ] 功能完整性验证