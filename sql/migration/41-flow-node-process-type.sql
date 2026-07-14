-- ============================================================
-- 流程节点/模板增加 process_type 分类字段
-- ============================================================

-- 1. flow_node 加 process_type
ALTER TABLE `flow_node`
  ADD COLUMN `process_type` VARCHAR(30) NOT NULL DEFAULT 'declaration' COMMENT '所属流程类型: declaration/remittance/taxRefund'
  AFTER `description`;

-- 2. flow_template 加 process_type
ALTER TABLE `flow_template`
  ADD COLUMN `process_type` VARCHAR(30) NOT NULL DEFAULT 'declaration' COMMENT '流程类型: declaration/remittance/taxRefund'
  AFTER `description`;

-- 3. 现有节点全部归为 declaration
UPDATE `flow_node` SET `process_type` = 'declaration' WHERE `process_type` = 'declaration';
