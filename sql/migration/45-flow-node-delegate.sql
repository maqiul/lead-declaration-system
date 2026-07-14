-- ============================================================
-- 流程节点库: 新增 delegate_expression 字段
-- 允许 serviceTask 节点的委托表达式从数据库配置
-- ============================================================

-- 1. flow_node 加 delegate_expression 列
ALTER TABLE `flow_node`
  ADD COLUMN `delegate_expression` VARCHAR(100) DEFAULT NULL
  COMMENT 'serviceTask 的委托表达式（如 ${declarationServiceTask}）'
  AFTER `process_type`;

-- 2. 回填现有 serviceTask 节点的委托表达式
UPDATE `flow_node` SET `delegate_expression` = '${declarationServiceTask}'
  WHERE `node_key` IN ('genPreEntryTask', 'genCustomsDoc');

-- 3. 为所有已有模板补充 genPreEntryTask 和 genCustomsDoc 节点编排（默认启用）
-- 获取 genPreEntryTask 和 genCustomsDoc 的 node_id
INSERT INTO `flow_template_node` (`template_id`, `node_id`, `enabled`, `sort_order`)
SELECT ft.id, fn.id, 1, 0
FROM `flow_template` ft
CROSS JOIN `flow_node` fn
WHERE fn.node_key IN ('genPreEntryTask', 'genCustomsDoc')
  AND ft.status = 1
  AND ft.del_flag = 0
ON DUPLICATE KEY UPDATE `enabled` = 1;
