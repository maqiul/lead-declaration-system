-- 32-资料模板发票模式配置字段
-- invoice_mode=1 时，该资料项的每个附件显示独立的金额/发票号/日期字段（发票式渲染）

-- 1. 模板表增加字段
ALTER TABLE `declaration_material_template`
    ADD COLUMN `invoice_mode` TINYINT NOT NULL DEFAULT 0 COMMENT '发票模式: 0-普通附件 1-附件级金额/发票号/日期' AFTER `stage`;

-- 2. 资料项表增加字段
ALTER TABLE `declaration_material_item`
    ADD COLUMN `invoice_mode` TINYINT NOT NULL DEFAULT 0 COMMENT '发票模式: 0-普通附件 1-附件级金额/发票号/日期' AFTER `stage`;

-- 3. 回填已有发票类数据
UPDATE `declaration_material_template` SET `invoice_mode` = 1
WHERE `code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE') OR `stage` = 'INVOICE';

UPDATE `declaration_material_item` SET `invoice_mode` = 1
WHERE `code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE') OR `stage` = 'INVOICE';
