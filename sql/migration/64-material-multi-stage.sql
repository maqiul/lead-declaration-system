-- ============================================================
-- 64: 资料模板/资料项 stage 支持多环节（逗号分隔）
-- 一个资料可同时属于多个环节，如 'BASIC,MATERIAL_SUBMIT'
-- 原列 VARCHAR(32) 不足以容纳多值，扩容至 VARCHAR(100)
-- ============================================================

ALTER TABLE `declaration_material_template`
    MODIFY COLUMN `stage` VARCHAR(100) NOT NULL DEFAULT 'MATERIAL_SUBMIT'
    COMMENT '所属环节（支持多环节逗号分隔）：BASIC-基础资料, MATERIAL_SUBMIT-资料上传, SUPPLEMENT-补充资料, INVOICE-业务发票';

ALTER TABLE `declaration_material_item`
    MODIFY COLUMN `stage` VARCHAR(100) NOT NULL DEFAULT 'MATERIAL_SUBMIT'
    COMMENT '所属环节（支持多环节逗号分隔，从模板克隆回填）：BASIC-基础资料, MATERIAL_SUBMIT-资料上传, SUPPLEMENT-补充资料, INVOICE-业务发票';
