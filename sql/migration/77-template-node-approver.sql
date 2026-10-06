-- ============================================================
-- 模板级审批人覆盖: flow_template_node 增加 assignee / candidate_groups
-- 允许每个流程模板独立配置各节点的办理人和候选组
-- 为空时使用 flow_node 全局默认值
-- 编号说明：原 46-template-node-approver.sql，与 46-material-invoice-category.sql 重号，顺延至 77。
--          仅对 flow_template_node 增列（建表在 40），41–76 无脚本读写新列，故置于链尾不影响重放
-- ============================================================

ALTER TABLE `flow_template_node`
  ADD COLUMN `assignee` VARCHAR(100) DEFAULT NULL COMMENT '办理人覆盖（为空则使用节点库默认值）' AFTER `sort_order`,
  ADD COLUMN `candidate_groups` VARCHAR(255) DEFAULT NULL COMMENT '候选组覆盖（为空则使用节点库默认值）' AFTER `assignee`;
