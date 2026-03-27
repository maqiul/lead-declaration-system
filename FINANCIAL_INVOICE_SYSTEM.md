# 财务票据管理系统使用说明

## 📋 系统概述

财务票据管理系统是一个独立的模块，用于管理货代发票、报关发票和开票明细，提供完整的票据生命周期管理功能。

## 🏗️ 系统架构

### 数据库表结构
- `financial_freight_invoice` - 货代发票表
- `financial_customs_invoice` - 报关发票表  
- `financial_invoice_detail` - 开票明细表
- `financial_invoice_audit_log` - 票据审核记录表

### 后端组件
- **实体类**: FinancialFreightInvoice, FinancialCustomsInvoice, FinancialInvoiceDetail
- **Mapper接口**: FinancialFreightInvoiceMapper, FinancialCustomsInvoiceMapper
- **Service接口**: FinancialFreightInvoiceService
- **控制器**: FinancialInvoiceController

### 前端组件
- **API接口**: financial/invoice.ts
- **页面组件**: 待开发

## 🔧 功能特性

### 1. 货代发票管理
- ✅ 发票信息录入（发票号、金额、供应商等）
- ✅ 发票文件上传
- ✅ 审核流程（待审核/已审核/已驳回）
- ✅ 按申报单关联查询
- ✅ 分页查询和筛选

### 2. 报关发票管理
- ✅ 报关行发票管理
- ✅ 发票信息维护
- ✅ 审核状态跟踪
- ✅ 与申报单关联

### 3. 开票明细管理
- ✅ 产品开票明细记录
- ✅ 发票抬头信息管理
- ✅ 税率和税额计算
- ✅ 开票状态跟踪

## 🚀 部署步骤

### 1. 数据库初始化
```sql
-- 执行表结构创建脚本
source sql/create_financial_invoice_tables.sql

-- 执行菜单配置脚本
source sql/financial_invoice_menu_config.sql
```

### 2. 后端部署
确保以下文件已创建：
- 实体类文件
- Mapper接口文件
- Service接口文件
- Controller文件

### 3. 前端部署
- API接口文件已创建
- 页面组件待开发

## 📊 API接口说明

### 货代发票接口
```
GET    /financial-invoice/freight/page     # 分页查询
GET    /financial-invoice/freight/{id}     # 查询详情
POST   /financial-invoice/freight          # 新增发票
PUT    /financial-invoice/freight          # 修改发票
DELETE /financial-invoice/freight/{id}     # 删除发票
POST   /financial-invoice/freight/{id}/audit # 审核发票
```

### 权限配置
- `financial:freight:list` - 查询权限
- `financial:freight:add` - 新增权限
- `financial:freight:edit` - 编辑权限
- `financial:freight:delete` - 删除权限
- `financial:freight:audit` - 审核权限

## 🎯 使用流程

### 财务人员操作流程
1. **录入发票信息** - 填写发票基本信息
2. **上传发票文件** - 附件上传功能
3. **提交审核** - 提交至审核流程
4. **审核处理** - 财务主管审核
5. **状态跟踪** - 查看审核结果

### 管理员操作流程
1. **查看所有票据** - 全局票据管理
2. **审核票据** - 审核各类发票
3. **统计分析** - 票据数据分析
4. **系统配置** - 权限和参数设置

## 🔒 安全控制

### 权限分级
- **财务人员**: 可录入、修改自己的票据
- **财务主管**: 可审核所有票据
- **管理员**: 全部操作权限

### 数据隔离
- 按组织机构ID进行数据隔离
- 支持多租户环境

## 📈 后续开发计划

### 待完成功能
1. 报关发票完整功能实现
2. 开票明细完整功能实现
3. 前端页面组件开发
4. 票据统计报表功能
5. 发票到期提醒功能
6. 电子发票对接功能

### 优化方向
1. 移动端适配
2. 批量操作功能
3. 导入导出功能
4. 消息通知机制
5. 工作流集成

## 🆘 常见问题

### 1. 权限不足
确保用户拥有相应的操作权限，可通过系统管理-角色管理进行配置。

### 2. 数据重复
系统通过发票号码进行唯一性约束，请确保发票号码不重复。

### 3. 文件上传失败
检查文件大小限制和服务器存储空间。

---
*文档版本: 1.0*
*最后更新: 2026-03-23*