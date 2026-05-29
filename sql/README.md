# SQL 脚本索引

本目录集中管理 `lead-declaration-system` 的全部数据库脚本。脚本均为**手工执行**（应用未配置 `spring.sql.init`，启动时不会自动运行），请按下方说明用 MySQL 客户端执行。

> 数据库：`lead_declaration`，字符集 `utf8mb4 / utf8mb4_general_ci`。

## 目录结构

| 目录 | 用途 |
|------|------|
| `init/` | 全新部署使用的基线初始化脚本（二选一，见下文） |
| `migration/` | 增量迁移脚本，文件名按推荐执行顺序用 `NN-` 序号前缀 |
| `diagnostic/` | 手工排查 / 修复 / 清理脚本，**不属于迁移链**，按需单独执行 |

---

## 一、全新部署

`init/` 下提供两套互斥的初始化方式，**任选其一**：

### 方式 A：完整快照（最快，推荐）

直接导入完整 mysqldump（包含全部业务表、Flowable `ACT_*` 表及基础数据）：

```bash
mysql -uroot -p < sql/init/00-full-database-dump.sql
```

> 该脚本内含 `DROP DATABASE IF EXISTS lead_declaration`，会**重建整个库**，仅用于全新环境。

### 方式 B：结构化初始化 + 迁移

适合希望按模块、按顺序了解 schema 演进的场景：

```bash
mysql -uroot -p < sql/init/01-schema-and-base-data.sql   # 基础用户/权限/组织/工作流结构
mysql -uroot -p < sql/init/02-menu-seed.sql              # 初始菜单种子数据
# 然后按顺序执行 migration/ 下全部脚本（见第二节）
```

> 方式 B 的基线不含 Flowable 的 `ACT_*` 表，这些表由 Flowable 在应用启动时自动建表
> （`application.yml` 中 `flowable.database-schema-update: true`），无需手工建。

| 文件 | 日期 | 说明 |
|------|------|------|
| `init/00-full-database-dump.sql` | 2026-03-21 | 完整库快照（9000+ 行，含 ACT_* 表与数据） |
| `init/01-schema-and-base-data.sql` | 2026-03-16 | 结构化初始化：用户/权限/组织/工作流 |
| `init/02-menu-seed.sql` | 2026-03-16 | 初始菜单数据 |

---

## 二、增量迁移（`migration/`）

在已有数据库上**按序号从小到大**执行。脚本大多带 `IF NOT EXISTS` / `INSERT IGNORE` / `ON DUPLICATE KEY`，基本可重复执行，但仍建议逐个核对后执行。

| 序号 | 文件 | 日期 | 说明 |
|------|------|------|------|
| 01 | `01-create-declaration-remittance-table.sql` | 早期 | 创建水单信息表 `declaration_remittance` |
| 02 | `02-create-declaration-attachment-table.sql` | 早期 | 创建申报单附件表 `declaration_attachment`（一对多） |
| 03 | `03-declaration-form-add-export-file-url.sql` | 早期 | `declaration_form` 增加 `export_file_url`（导出文件路径） |
| 04 | `04-create-financial-invoice-table.sql` | 2026-03-27 | 创建财务发票表 `financial_invoice` |
| 05 | `05-create-financial-supplement-table.sql` | 2026-03-27 | 创建财务补充表 `financial_supplement` |
| 06 | `06-create-business-audit-record-table.sql` | 2026-03-31 | 创建通用业务审核记录表 `business_audit_record`（如退回草稿） |
| 07 | `07-declaration-permission-optimization.sql` | 2026-04-24 | 申报管理权限优化，拆分审批权限 |
| 08 | `08-remittance-relation-permission.sql` | 2026-04-24 | 水单关联申报单的菜单权限 |
| 09 | `09-product-add-amount-locked.sql` | 2026-04-24 | `declaration_product` 增加 `amount_locked`（金额锁定标记） |
| 10 | `10-finance-invoice-module.sql` | 2026-04-24 | 财务发票台账模块：表结构 + 菜单 + 权限 |
| 11 | `11-full-permission-registry.sql` | 2026-05-05 | 全量重建 `sys_menu`（180+ 条）与超级管理员授权 |
| 12 | `12-material-submit-audit.sql` | 2026-05-05 | 申报资料提交/审核：模板表 + 实例表 + 默认模板 + 菜单权限 |
| 13 | `13-material-item-extend-fields.sql` | 2026-05-05 | 资料项扩展结构化字段（发票金额/号/开票日期/`form_schema`） |
| 14 | `14-material-item-backfill-schema.sql` | 2026-05-05 | 回填存量资料项 `form_schema`（依赖 13） |
| 15 | `15-material-item-auditor-fields.sql` | 2026-05-05 | 资料项行级 `create_by` / `update_by` 审计字段 |
| 16 | `16-material-multi-file.sql` | 2026-05-05 | 资料项多文件支持：新建附件子表 + 历史数据迁移 |
| 17 | `17-material-customize-permission.sql` | 2026-05-05 | 「自定义资料项」操作权限 |
| 18 | `18-remove-currency-from-invoice-schema.sql` | 2026-05-05 | 去除货代/报关代理发票模板的币种字段并清洗历史数据 |
| 19 | `19-declaration-resume-flow-permission.sql` | 2026-05-05 | 「恢复流程」按钮权限（老 BPMN 迁移到新流程节点） |
| 20 | `20-declaration-menu-split.sql` | 2026-05-11 | 「申报管理」拆分为：录入/资料提交/发票提交/归档查询 |
| 21 | `21-material-template-stage.sql` | 2026-05-11 | 资料模板增加 `stage` 环节字段 |
| 22 | `22-supplement-stage.sql` | 2026-05-11 | 补充资料环节（SUPPLEMENT）模板数据 |
| 23 | `23-add-supplement-menu.sql` | 2026-05-11 | 新增「补充资料」「开票金额」两个独立菜单 |
| 24 | `24-flow-supplement-invoice-amount.sql` | 2026-05-11 | 补充资料 + 申请开票金额流程；申报单状态迁移 |
| 25 | `25-migrate-legacy-declaration-flow.sql` | 2026-05-29 | 老申报单业务状态修正（资料审过→补充资料）；Flowable 需配合接口批量恢复 |
| 26 | `26-supplement-invoice-amount-permissions.sql` | 2026-05-29 | 注册补充资料/开票金额按钮权限（修复提交按钮被 v-permission 隐藏） |
| 27 | `27-invoice-amount-submit-for-declarant.sql` | 2026-05-29 | 为资料/补充资料申报角色补全开票金额提交权限 |

> 注：序号代表**推荐执行顺序**而非严格日期。多个脚本同日产生，序号在同日内按依赖关系排定
> （例如 14 依赖 13）。01–03 为早期独立建表脚本，原仓库未纳入版本管理，日期不可考，
> 在全新部署的「方式 A 完整快照」中其表结构已包含。

---

## 三、诊断 / 修复脚本（`diagnostic/`）

仅在排查问题或特定环境修复时手工执行，**不要纳入常规迁移流程**。

| 文件 | 说明 |
|------|------|
| `check-permission-menu-errors.sql` | 排查权限/菜单相关 500 错误：查看表字段、菜单 ID 是否存在等（含 SELECT 诊断语句） |
| `fix-home-menu.sql` | 修复首页菜单不显示问题（诊断 + 修复 `sys_menu`） |
| `flowable-cleanup-orphan-executions.sql` | Flowable 运行时脏数据诊断与救援（仅限开发/测试环境，生产请走 Flowable API 级联删除） |

---

## 四、老流程迁移到新版（重要）

新版 `declarationProcess` 在「资料提交/审核」之后增加了**补充资料、申请开票金额**等环节。旧版 BPMN 常在 status=2 后流程实例即结束，造成：

- 列表上仍是「待资料提交/待资料审核」
- 点击提交/审核报错：「没有待提交的资料任务」

**推荐步骤：**

1. 执行 `migration/24-flow-supplement-invoice-amount.sql`（若尚未执行）
2. 执行 `migration/25-migrate-legacy-declaration-flow.sql`（修正 status=3/8 等业务状态）
3. 调用接口恢复 Flowable（先预览再执行）：
   ```bash
   # 预览（默认 dryRun=true）
   POST /api/v1/declarations/migrate-flow/batch?dryRun=true&statuses=2&statuses=3

   # 正式迁移
   POST /api/v1/declarations/migrate-flow/batch?dryRun=false&statuses=2&statuses=3
   ```
4. 或在申报管理列表「更多 → 恢复流程」逐条处理

**智能映射规则（接口侧）：**

| 业务 status | 典型场景 | 迁移到节点 |
|-------------|----------|------------|
| 2 | 待资料提交 | materialSubmit |
| 3 + 资料审核已通过 | 老流程未走补充资料 | supplementSubmit（status 改为 4） |
| 3 + 待审 | 已提交待审 | materialAudit |
| 8 + 无补充资料上传 | 老流程直接进发票 | supplementSubmit（status 改为 4） |
