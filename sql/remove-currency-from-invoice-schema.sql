-- ============================================================================
-- 去除 "货代发票 (FREIGHT_INVOICE)" 和 "报关代理发票 (CUSTOMS_AGENT_INVOICE)"
-- 资料项模板 form_schema 中的 currency（币种）字段。
--
-- 背景：该两类发票均按人民币结算，无需录入币种；同步清空历史资料项实例的
-- currency 字段，避免旧数据残留干扰展示。
--
-- 影响范围：
--   1. declaration_material_template —— 模板配置 form_schema（新申报单克隆基准）
--   2. declaration_material_item     —— 历史申报单资料项实例的 form_schema 副本
--                                        与 currency 列值
--
-- 说明：资料项实例在申报单创建时会从模板克隆 form_schema 到自己的列上，
--       之后模板更新不会反向同步，所以必须同时覆盖实例表。
--
-- 执行建议：生产库执行前先备份；可重复执行（幂等）。
-- ============================================================================

-- 1. 重写货代发票 / 报关代理发票模板的 form_schema（去掉 currency）
UPDATE `declaration_material_template`
SET `form_schema` = '[{"key":"amount","label":"发票金额","type":"number","required":true},{"key":"invoiceNo","label":"发票号","type":"text","required":true},{"key":"invoiceDate","label":"开票日期","type":"date","required":true}]',
    `update_time` = NOW()
WHERE `code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE');

-- 2. 覆盖历史申报单资料项实例的 form_schema 副本（去掉 currency）
--    页面实际读的是实例表这份 schema，必须同步，否则旧单页面仍然显示币种必填。
UPDATE `declaration_material_item`
SET `form_schema` = '[{"key":"amount","label":"发票金额","type":"number","required":true},{"key":"invoiceNo","label":"发票号","type":"text","required":true},{"key":"invoiceDate","label":"开票日期","type":"date","required":true}]',
    `update_time` = NOW()
WHERE `code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE')
   OR `template_id` IN (
        SELECT id FROM `declaration_material_template`
        WHERE `code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE')
   );

-- 3. 清空历史已提交资料项实例的币种字段（仅针对上述两类）
UPDATE `declaration_material_item`
SET `currency` = NULL,
    `update_time` = NOW()
WHERE (`code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE')
       OR `template_id` IN (
            SELECT id FROM `declaration_material_template`
            WHERE `code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE')
       ))
  AND `currency` IS NOT NULL;

-- 4. 验证（可选执行）
-- SELECT id, code, name, form_schema FROM declaration_material_template
--   WHERE code IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE');
-- SELECT COUNT(*) AS leftover_schema FROM declaration_material_item
--   WHERE code IN ('FREIGHT_INVOICE','CUSTOMS_AGENT_INVOICE')
--     AND form_schema LIKE '%currency%';
-- SELECT COUNT(*) AS leftover_currency FROM declaration_material_item
--   WHERE code IN ('FREIGHT_INVOICE','CUSTOMS_AGENT_INVOICE') AND currency IS NOT NULL;
