# 线索申报系统 - 项目工作记录

> **创建日期**: 2026-04-22  
> **最后更新**: 2026-07-07  
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

## 📊 2026-07-07 工作内容

### 1. 扣款项获取逻辑简化

#### 后端 (`FinancialSupplementServiceImpl.java`)
- `getAllInvoiceDeductions()` 简化为直接判断 `invoiceCategory === 'DEDUCTION'`
- 移除 stage 排除、invoiceMode 回退、硬编码 code 等复杂判断
- 删除 `isInvoiceTypeItem()` 方法

---

### 2. 开票文件含税金额修正

#### 后端 (`FinancialSupplementController.java`)
- Word/Excel 表格中"含税金额"列改用 `amountWithTaxRefund`（退税加成后金额），替代原来的 `cnyAmount`（原始水单 CNY）
- Word 通知摘要和 Excel 计算摘要均改为 80% 内联计算

---

### 3. 80% 开票基数（仅文件生成）

#### 规则
- **前端显示**: 总金额 = 退税加成合计 - 扣款合计 - 手续费合计（不含 80%）
- **文件生成（Word/Excel）**: 开票基数 = `amountWithTaxRefund × 0.8`，文件开票金额 = 开票基数 - 扣款 - 手续费

#### 实现
- 后端 `getCalculationDetail()` 返回完整金额（无 80% 因子）
- 文件生成代码内联计算 80%：
  ```java
  BigDecimal invoiceBaseAmt = amountWithTaxRefundAmt.multiply(new BigDecimal("0.8")).setScale(2, RoundingMode.HALF_UP);
  BigDecimal fileInvoiceAmt = invoiceBaseAmt.subtract(totalDeductionAmt).subtract(totalFeeAmt).setScale(2, RoundingMode.HALF_UP);
  ```
- 文件摘要显示：退税加成合计 → 开票基数(80%) → 扣款合计 → 开票金额

---

### 4. MaterialManager 分段渲染

#### 前端 (`MaterialManager.vue`)
- 新增 props: `sectionRange`（'all'|'pre'|'post'）、`stopBefore`（边界 section 名）、`sectionOrderMap`
- `visibleSections` 根据 sectionRange 过滤 pre/post 片段

#### 前端 (`FormComposition.vue`)
- 新增 `sectionOrderMap` computed：从流程节点推导 formSection → sortOrder 映射
- 模板分段渲染：MaterialManager(pre) → InvoiceAmountSection → MaterialManager(post)
- InvoiceAmountSection 插入在补充资料和发票资料之间，由流程节点 sortOrder 驱动环节顺序

---

### 5. 开票金额前端展示 (`InvoiceAmountSection.vue` / `index.vue`)

#### 计算公式展示
- 收入部分：退税加成合计 = Σ(商品 cnyAmount × (1 + 退税率%))
- 支出部分：扣款项 + 银行手续费 + 内部操作费
- 开票金额 = 退税加成合计 - 支出合计（无 80%）

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
   - 执行 `sql/migration/09-product-add-amount-locked.sql`
   - 清理历史发票 `form_id` 为 NULL 的数据

2. **编译**:
   - 后端重新编译
   - 前端重新构建

---

**维护人**: AI助手  
**最后更新**: 2026-07-07
