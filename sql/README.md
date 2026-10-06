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
> 快照之后的增量脚本仍需按第二节顺序补执行。

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

**编号规则**：序号即推荐执行顺序，新增脚本取当前最大号 +1（当前链尾为 **80**，下一个用 **81**）。
一个序号只允许一个文件，禁止重号；历史遗留的 4 组重号已在 2026-10 统一理顺（见本节末尾映射表）。

日期列取自脚本内注释的执行日期，未标注者取 git 入库日期。

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
| 27 | `27-invoice-amount-submit-for-declarant.sql` | 2026-05-29 | 为普通申报角色补全开票金额提交权限 |
| 28 | `28-entity-config.sql` | 2026-06-12 | 多主体配置：新建 `entity_config`（公司主体 + 单证模板路径） |
| 29 | `29-backfill-declaration-entity-id.sql` | 2026-06-12 | 按 `shipper_company` 匹配，回填存量申报单 `entity_id` |
| 30 | `30-add-tax-refund-rate-and-min-service-fee.sql` | 2026-06-16 | HS 商品类型配置加退税率；银行账户配置加最低操作费 |
| 31 | `31-remittance-menu-split.sql` | 2026-06-16 | 水单管理拆分为草稿/待审核/已审核/未关联四个菜单 |
| 32 | `32-material-invoice-mode.sql` | 2026-07-01 | 资料模板加 `invoice_mode`（每个附件独立填写金额/发票号/日期） |
| 33 | `33-entity-config-extra-fields.sql` | 2026-07-01 | 主体配置加纳税人识别号、电话、开户银行 |
| 34 | `34-invoice-split-item.sql` | 2026-07-01 | 新建 `invoice_split_item`（开票 80%/20% 拆分产品明细） |
| 35 | `35-invoice-split-item-hs-code.sql` | 2026-07-01 | `invoice_split_item` 加 `hs_code` |
| 36 | `36-org-type-and-declaration-type.sql` | 2026-07-01 | 组织表加机构类型；申报单表加申报类型 |
| 37 | `37-fix-internal-org-data.sql` | 2026-07-01 | 内部组织申报流程存量数据修复 |
| 38 | `38-declaration-submit-others.sql` | 2026-07-07 | 新增「代提交申报单」权限（提交非本人创建的申报单） |
| 39 | `39-flow-template-config.sql` | 2026-07-14 | 流程模板配置：表结构 + 预置数据 + 菜单权限 |
| 40 | `40-flow-node-library.sql` | 2026-07-14 | 全局流程节点库 + 模板-节点编排表（替代 `flow_template_step`） |
| 41 | `41-flow-node-process-type.sql` | 2026-07-14 | 流程节点/模板增加 `process_type` 分类字段 |
| 42 | `42-sys-dict.sql` | 2026-07-14 | 系统字典表 `sys_dict` / `sys_dict_item` + 预置 process_type、form_section、node_type |
| 43 | `43-declaration-template-permission.sql` | 2026-07-14 | 申报模板选择权限；`declaration_form` 加 `template_code` |
| 44 | `44-carton-product-weight.sql` | 2026-07-14 | `declaration_carton_product` 加毛重/净重（按箱设置每个产品） |
| 45 | `45-flow-node-delegate.sql` | 2026-07-14 | `flow_node` 加 `delegate_expression`（serviceTask 委托表达式） |
| 46 | `46-material-invoice-category.sql` | 2026-07-14 | 资料模板加 `invoice_category`（区分扣款/进项发票） |
| 47 | `47-material-template-binding.sql` | 2026-07-14 | 资料模板绑定表（流程 + 运输方式双维度） |
| 48 | `48-declaration-rollback-permission.sql` | 2026-06-12 | 「退回上一步」申请与审核按钮权限（挂在旧菜单 202 下，**必须早于 54 执行**） |
| 49 | `49-binding-required-override.sql` | 2026-07-14 | 绑定规则加 `required`，按规则覆盖模板全局必填设置 |
| 50 | `50-payment-remittance.sql` | 2026-07-14 | 出款水单模块表结构（与申报单多对多关联） |
| 51 | `51-payment-remittance-menu.sql` | 2026-07-14 | 出款水单管理菜单与权限 |
| 52 | `52-form-section-remark.sql` | 2026-07-14 | `form_section` 字典项 `remark` 扩展 JSON（区块 UI 与流程映射） |
| 53 | `53-declaration-view-internal-permission.sql` | 2026-07-14 | 「查看内部申报」「查看外部申报」权限按钮，控制菜单可见性 |
| 54 | `54-declaration-menu-split.sql` | 2026-07-14 | 申报菜单拆分为 SELF/EXT 两套物理隔离菜单（含角色迁移） |
| 55 | `55-customer-config.sql` | 2026-07-17 | 常用客户配置表 + 菜单权限 |
| 56 | `56-customer-country-code-backfill.sql` | 2026-07-17 | 常用客户目的国/贸易国存量数据统一为英文全名 |
| 57 | `57-trade-term.sql` | 2026-07-21 | 贸易方式（Incoterms）配置表 + 预置 11 条 + 菜单权限 |
| 58 | `58-trade-term-transport-mode.sql` | 2026-07-21 | 贸易方式与运输方式多对多关联表 |
| 59 | `59-declaration-add-trade-term.sql` | 2026-07-21 | 申报单加贸易方式与到达港口字段 |
| 60 | `60-material-exemption.sql` | 2026-07-21 | 资料豁免审批记录表（必填文件不全时强制提交） |
| 61 | `61-exemption-flow-template.sql` | 2026-07-21 | 豁免流程节点库 + 模板（普通 1 步 / 发票 2 步） |
| 62 | `62-flow-node-reject-to-end.sql` | 2026-07-21 | `flow_node` 加 `reject_to_end`（驳回时直接结束流程） |
| 63 | `63-rename-exemption-invoice-audit.sql` | 2026-07-21 | `exemptionInvoiceAudit` 节点名称改为「豁免复核」 |
| 64 | `64-material-multi-stage.sql` | 2026-07-30 | 资料模板/资料项 `stage` 支持多环节（逗号分隔） |
| 65 | `65-declaration-data-scope-permission.sql` | 2026-08-04 | 数据权限隔离：「查看下级申报」+「发起资料补交」权限点 |
| 66 | `66-material-supplement-flow.sql` | 2026-08-04 | 独立资料补交流程（增量资料审核通过才转正） |
| 67 | `67-template-required-stages.sql` | 2026-08-04 | 资料模板必填按环节配置（加 `required_stages`） |
| 68 | `68-remittance-revoke-audit-permission.sql` | 2026-08-04 | 水单/出款水单反审核独立权限点 |
| 69 | `69-supplement-flow.sql` | 2026-08-04 | 资料补交流程升级为 Flowable 工作流（独立实例、不阻塞主流程） |
| 70 | `70-backfill-attachment-supplement-id.sql` | 2026-08-04 | 回填补交期内缺失 `supplement_id` 的附件 |
| 71 | `71-cleanup-stale-supplement-marks.sql` | 2026-08-04 | 清理指向已失效补交单的历史标记 |
| 72 | `72-supplement-file-snapshot.sql` | 2026-08-04 | 新建补交文件快照表（转正/驳回后保留增量留痕） |
| 73 | `73-carton-dims-and-exw-misc-fee.sql` | 2026-08-31 | 箱子加单箱长宽高（cm）；EXW 贸易方式加杂费字段 |
| 74 | `74-volume-4-decimals.sql` | 2026-08-31 | `volume` / `total_volume` 统一保留 4 位小数 |
| 75 | `75-district-info.sql` | 2026-09-03 | 出发口岸精确到区/县（区县数据导入 `city_info`，含区划码修正） |
| 76 | `76-party-b-config.sql` | 2026-09-05 | 乙方配置表 + 菜单权限（范式同「常用客户」） |
| 77 | `77-template-node-approver.sql` | 2026-07-14 | `flow_template_node` 加 `assignee` / `candidate_groups`（模板级审批人覆盖） |
| 78 | `78-supplement-initiate-grant.sql` | 2026-08-04 | 为普通申报角色补授「发起资料补交」权限（menu 81083） |
| 79 | `79-supplement-audit-menu.sql` | 2026-08-04 | 新增「补充资料审核」独立菜单（SELF 922 / EXT 923） |
| 80 | `80-carton-type-dict.sql` | 2026-10-06 | 箱子类型字典化 `carton_type`（纸箱/木箱/托盘）+ 修正历史 `CARTRONS` 拼写 |

> 注：序号代表**推荐执行顺序**而非严格日期。多个脚本同日产生，序号在同日内按依赖关系排定
> （例如 14 依赖 13）。01–03 为早期独立建表脚本，原仓库未纳入版本管理，日期不可考，
> 在全新部署的「方式 A 完整快照」中其表结构已包含。

### 重号修复映射（2026-10）

历史上存在 4 组重号，已按下表重命名（**仅改文件名，脚本内容未改动**，已执行过的库无需重跑）。
被挪号的脚本在其文件头都写了「编号说明」注释。

| 原文件名 | 现文件名 | 处理理由 |
|----------|----------|----------|
| `26-declaration-rollback-permission.sql` | `48-declaration-rollback-permission.sql` | 与 26 重号；其按钮挂在旧菜单 202 下，必须早于 54，故占用同段空号 48 |
| `46-template-node-approver.sql` | `77-template-node-approver.sql` | 与 46 重号；仅对 `flow_template_node` 增列，41–76 无脚本读写新列，可顺延链尾 |
| `70-supplement-initiate-grant.sql` | `78-supplement-initiate-grant.sql` | 与 70 重号；只做角色授权补发，依赖 65/69，无后续脚本依赖它 |
| `71-supplement-audit-menu.sql` | `79-supplement-audit-menu.sql` | 与 71 重号；新增菜单 922/923，依赖 54/69，无后续脚本依赖它 |

修复后迁移链为 **01–80 连续、无重号、无断号**（原先缺失的 48 号已启用）。

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
