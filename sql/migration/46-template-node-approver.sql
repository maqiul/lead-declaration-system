-- ============================================================
-- 模板级审批人覆盖: flow_template_node 增加 assignee / candidate_groups
-- 允许每个流程模板独立配置各节点的办理人和候选组
-- 为空时使用 flow_node 全局默认值
-- ============================================================

ALTER TABLE `flow_template_node`
  ADD COLUMN `assignee` VARCHAR(100) DEFAULT NULL COMMENT '办理人覆盖（为空则使用节点库默认值）' AFTER `sort_order`,
  ADD COLUMN `candidate_groups` VARCHAR(255) DEFAULT NULL COMMENT '候选组覆盖（为空则使用节点库默认值）' AFTER `assignee`;
