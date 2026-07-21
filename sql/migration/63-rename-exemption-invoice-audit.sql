-- ============================================================
-- 将 exemptionInvoiceAudit 节点名称从"豁免发票审核"改为"豁免复核"
-- ============================================================

UPDATE `flow_node`
SET `node_name` = '豁免复核',
    `description` = '发票类文件豁免复核（金额/信息确认）'
WHERE `node_key` = 'exemptionInvoiceAudit';
