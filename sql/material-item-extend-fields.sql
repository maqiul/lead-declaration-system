-- ============================================================
-- 申报资料项 - 扩展结构化字段（发票金额/发票号/开票日期/扩展 JSON）
-- 用于已经执行过 material-submit-audit.sql 的环境做增量升级
-- ============================================================

-- 1. 模板表增加 form_schema 字段
ALTER TABLE `declaration_material_template`
    ADD COLUMN `form_schema` TEXT DEFAULT NULL COMMENT '结构化字段配置 JSON（如发票金额/发票号等）' AFTER `remark`;

-- 2. 实例表增加 form_schema + 4 个固定列 + extra_data
ALTER TABLE `declaration_material_item`
    ADD COLUMN `form_schema`  TEXT          DEFAULT NULL COMMENT '结构化字段配置（从模板克隆可单据内覆盖）' AFTER `remark`,
    ADD COLUMN `amount`       DECIMAL(18,4) DEFAULT NULL COMMENT '金额（发票类使用）'                    AFTER `upload_time`,
    ADD COLUMN `currency`     VARCHAR(10)   DEFAULT NULL COMMENT '币种'                                  AFTER `amount`,
    ADD COLUMN `invoice_no`   VARCHAR(100)  DEFAULT NULL COMMENT '发票号'                               AFTER `currency`,
    ADD COLUMN `invoice_date` DATE          DEFAULT NULL COMMENT '开票日期'                             AFTER `invoice_no`,
    ADD COLUMN `extra_data`   TEXT          DEFAULT NULL COMMENT '其他扩展字段 JSON'                    AFTER `invoice_date`;

-- 3. 为默认发票类模板回填 form_schema
UPDATE `declaration_material_template`
SET `form_schema` = '[{"key":"amount","label":"发票金额","type":"number","required":true},{"key":"invoiceNo","label":"发票号","type":"text","required":true},{"key":"invoiceDate","label":"开票日期","type":"date","required":true}]'
WHERE `code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE')
  AND (`form_schema` IS NULL OR `form_schema` = '');
