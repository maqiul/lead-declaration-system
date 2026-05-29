-- ============================================================
-- 资料模板环节分类改造
-- 给 declaration_material_template 增加所属环节字段
-- 环节枚举：
--   MATERIAL_SUBMIT     - 资料上传
--   INVOICE             - 业务发票
--   FINANCE_SUPPLEMENT  - 财务补充
-- ============================================================

-- 1. 模板表增加 stage 字段（默认归到资料上传环节，兼容存量）
ALTER TABLE `declaration_material_template`
  ADD COLUMN `stage` VARCHAR(32) NOT NULL DEFAULT 'MATERIAL_SUBMIT'
  COMMENT '所属环节：MATERIAL_SUBMIT-资料上传, INVOICE-业务发票, FINANCE_SUPPLEMENT-财务补充'
  AFTER `enabled`;

-- 2. 按环节 + 启用状态 的复合索引（前端按环节筛选时使用）
ALTER TABLE `declaration_material_template`
  ADD KEY `idx_stage_enabled` (`stage`, `enabled`);

-- 3. 实例表同步增加 stage 字段（从模板克隆时回填，兼容存量默认资料上传）
ALTER TABLE `declaration_material_item`
  ADD COLUMN `stage` VARCHAR(32) NOT NULL DEFAULT 'MATERIAL_SUBMIT'
  COMMENT '所属环节：MATERIAL_SUBMIT-资料上传, INVOICE-业务发票, FINANCE_SUPPLEMENT-财务补充'
  AFTER `template_id`;

-- 4. 实例表按 form_id + stage 的复合索引（申报单详情页按环节查询时使用）
ALTER TABLE `declaration_material_item`
  ADD KEY `idx_form_stage` (`form_id`, `stage`);
