# 线索申报系统 - 项目工作记录

> **创建日期**: 2026-04-22  
> **最后更新**: 2026-04-24  
> **项目路径**: F:\lead-declaration-system

---

## 📋 项目概述

**项目名称**: 线索申报系统 (Lead Declaration System)  
**技术栈**: 
- 前端: Vue 3 + TypeScript + Ant Design Vue
- 后端: Spring Boot + MyBatis-Plus + Sa-Token
- 工作流: Flowable 7.2.0
- 数据库: MySQL 8.0 (Docker)
- 缓存: Redis (Database 2 for Sa-Token)

**权限框架**: Sa-Token (RBAC模型)

---

## 📊 2026-04-24 工作内容

### 1. 发票台账强制关联申报单

#### 后端
- `DeclarationInvoice.java`: 新增 `formNo`（非DB字段）、`invoiceName` 字段
- `FinanceInvoiceController.java`: 创建/更新时强制校验 `form_id` 必填
- 查询接口增加 `formId` 筛选参数，批量填充申报单号

#### 前端
- 发票台账录入弹窗新增申报单选择器（支持搜索）
- 列表新增申报单号列、申报单筛选器
- 移除跳转申报页逻辑

#### 数据库
- `declaration_invoice.form_id` 改为 `NOT NULL`
- 新增 `invoice_name` 字段

---

### 2. 发票文件上传功能

#### 后端（FinanceInvoiceController）
- `POST /v1/finance/invoices/{id}/file` - 上传发票文件
- `GET /v1/finance/invoices/{id}/file` - 下载发票文件

#### 前端（发票台账页）
- 录入弹窗新增"发票文件"上传组件
- 保存时一起上传文件

---

### 3. 申报详情页发票上传

#### 申报列表页
- "更多"菜单新增**"上传发票"**按钮
- 点击跳转到申报详情页（`mode=invoiceUpload`）

#### 申报详情页
- 新增 `isInvoiceUploadMode` 模式（页面只读，仅发票区域可操作）
- 业务发票模块 `status>=1` 才显示（草稿不显示）
- 移除发票类型选择，默认为进项
- 新增发票名称字段

#### 后端
- `uploadBusinessInvoice` 使用 `attachmentService.uploadFile()` 统一文件存储

---

### 4. 产品金额锁定功能

#### 需求
- 用户手动修改金额后，数量/单价变化不再自动计算

#### 实现
- **数据库**: `declaration_product.amount_locked` (tinyint, 0/1)
- **前端**: 
  - `amountUserModified` - 标记用户手动修改
  - `handleAmountChange()` - 用户修改时标记
  - `handleQuantityOrPriceChange()` - 未锁定时自动计算
  - 保存时转换: `true → 1`, `false → 0`
- **后端**: `DeclarationProduct.java` 新增 `amountLocked` 字段

---

### 5. 退回草稿审核功能

#### 申报列表页
- **退回草稿申请**按钮: `status >= 1 && status !== 9`
- **退回审核**按钮: `status === 9`（新增）
- 新增状态 `9` - 退回待审（橙色标签）

#### 权限
- 申请: `business:declaration:return:apply`
- 审核: `business:declaration:return:audit`

---

### 6. 其他优化

| 修改项 | 说明 |
|--------|------|
| 发票模块显示 | 草稿状态不显示，`status>=1` 才显示 |
| 提货单按钮 | 只有提货单模式（`isPickupMode`）才显示 |
| 发票权限 | 统一使用 `finance:invoice:create/update/delete/view` |

---

## 📊 状态定义

| 状态值 | 状态名称 | 说明 |
|--------|---------|------|
| 0 | 草稿 | 初始状态 |
| 1 | 待审核 | 已提交 |
| 2 | 已完成 | 审核通过 |
| 9 | 退回待审 | 退回申请待审核 |

---

## 📚 核心权限

### 申报单
| 权限标识 | 说明 |
|---------|------|
| `business:declaration:view` | 查看申报单 |
| `business:declaration:create` | 创建/保存草稿 |
| `business:declaration:update` | 更新申报单/上传发票 |
| `business:declaration:submit` | 提交申报单 |
| `business:declaration:return:apply` | 申请退回草稿 |
| `business:declaration:return:audit` | 审核退回申请 |

### 发票台账
| 权限标识 | 说明 |
|---------|------|
| `finance:invoice:view` | 查看发票列表 |
| `finance:invoice:create` | 录入发票 |
| `finance:invoice:update` | 编辑发票 |
| `finance:invoice:delete` | 删除发票 |

---

## 📁 重要文件

| 文件 | 说明 |
|------|------|
| `发票台账与申报单关联优化-工作记录.md` | 2026-04-24 详细工作记录 |
| `QWEN.md` | 本文件 - 项目概要 |
| `README.md` | 项目说明文档 |
| `sql/` | 数据库脚本目录 |
| `backend/` | 后端代码 |
| `frontend/` | 前端代码 |

---

## ⚠️ 待处理

1. **数据库迁移**:
   - 执行 `sql/add-amount-locked-to-product.sql`
   - 清理历史发票 `form_id` 为 NULL 的数据

2. **编译**:
   - 后端重新编译
   - 前端重新构建

---

**维护人**: AI助手  
**最后更新**: 2026-04-24
