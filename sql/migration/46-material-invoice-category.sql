-- ============================================================
-- 资料模板: 新增 invoice_category 字段
-- 区分发票类资料项是扣款还是进项
-- ============================================================

-- 1. declaration_material_template 加列
ALTER TABLE `declaration_material_template`
  ADD COLUMN `invoice_category` VARCHAR(20) DEFAULT NULL
  COMMENT '发票分类: DEDUCTION-扣款 INPUT-进项 (仅 invoice_mode=1 时有效)'
  AFTER `invoice_mode`;

-- 2. declaration_material_item 加列
ALTER TABLE `declaration_material_item`
  ADD COLUMN `invoice_category` VARCHAR(20) DEFAULT NULL
  COMMENT '发票分类: DEDUCTION-扣款 INPUT-进项 (从模板克隆时同步)'
  AFTER `invoice_mode`;

-- 3. 回填现有数据: 已知扣款类发票编码设为 DEDUCTION
UPDATE `declaration_material_template`
  SET `invoice_category` = 'DEDUCTION'
  WHERE `invoice_mode` = 1
    AND `code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE');

-- 4. 同步到已有资料项
UPDATE `declaration_material_item` mi
  INNER JOIN `declaration_material_template` mt ON mi.template_id = mt.id
  SET mi.invoice_category = mt.invoice_category
  WHERE mt.invoice_category IS NOT NULL;
