# 菜单配置修复指南

## 问题分析
发现菜单配置存在以下问题：
1. 后端菜单数据与前端路由路径不匹配
2. 菜单显示逻辑需要优化
3. 缺少完整的菜单权限配置

## 修复步骤

### 1. 执行数据库菜单修复脚本
```sql
-- 执行 fix-menu-configuration.sql 脚本
-- 该脚本会：
-- - 删除旧的不匹配菜单项
-- - 重新插入正确的菜单配置
-- - 为管理员角色分配权限
-- - 验证修复结果
```

### 2. 前端菜单显示已修复
- 修正了 layout/index.vue 中的菜单数据处理逻辑
- 确保能正确显示多级菜单结构

### 3. 验证修复结果

#### 后端验证：
```sql
-- 检查菜单配置是否正确
SELECT 
    m.menu_name,
    m.menu_code,
    m.path,
    m.component,
    m.permission,
    p.menu_name as parent_name
FROM sys_menu m
LEFT JOIN sys_menu p ON m.parent_id = p.id
WHERE m.menu_code LIKE 'demo%' 
   OR m.menu_code LIKE 'business%' 
   OR m.menu_code LIKE 'declaration%'
ORDER BY m.menu_type, m.sort;
```

#### 前端验证：
1. 登录系统后检查左侧菜单是否正常显示
2. 点击各个菜单项验证路由跳转是否正常
3. 检查是否有404错误

## 完整的菜单结构

### 演示功能 (/demo)
- 发货清单 (demo/simple-shipping-demo.vue)
- 申报表单 (demo/declaration-form-demo.vue)  
- 历史记录 (demo/declaration-history.vue)

### 业务管理 (/business)
- 财务审核 (business/tax-refund/finance-review.vue)

### 出口申报 (/declaration)
- 申报管理 (declaration/manage/index.vue)
- 申报统计 (declaration/statistics/index.vue)

## 注意事项
1. 确保后端服务正常运行
2. 数据库连接正常
3. 管理员账号具有相应权限
4. 前端页面文件存在且路径正确

## 常见问题解决

### 菜单不显示
- 检查用户是否具有相应菜单权限
- 验证菜单状态是否为启用(1)
- 确认菜单类型正确(1-目录, 2-菜单)

### 点击菜单404
- 检查前端路由配置是否正确
- 验证页面组件文件是否存在
- 确认菜单path与路由路径匹配

### 权限不足
- 检查sys_role_menu表中的权限分配
- 验证用户角色是否正确
- 确认权限标识(permission字段)正确