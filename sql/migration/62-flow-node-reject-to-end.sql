-- ============================================================
-- flow_node 新增 reject_to_end 字段
-- 标记审核节点驳回时是否直接结束流程（而非回退到上一个提交节点）
-- 豁免流程的审核节点设为 1（驳回即结束）
-- ============================================================

ALTER TABLE `flow_node`
  ADD COLUMN `reject_to_end` TINYINT NOT NULL DEFAULT 0
  COMMENT '驳回时是否直接结束流程 0-否(回退) 1-是(结束)' AFTER `delegate_expression`;

-- 豁免流程的审核节点：驳回直接结束
UPDATE `flow_node` SET `reject_to_end` = 1
WHERE `node_key` IN ('exemptionAudit', 'exemptionInvoiceAudit');
