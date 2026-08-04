-- ============================================================
-- 69-supplement-flow.sql
-- 资料补交流程升级为 Flowable 工作流（独立实例、不阻塞主流程）
-- 依赖：66-material-supplement-flow.sql 须先执行（补交表已建）
-- 参照：61-exemption-flow-template.sql（豁免流程节点库 + 模板写法）
-- ============================================================

-- 1. 补交记录表增加 Flowable 流程实例ID
ALTER TABLE `declaration_material_supplement`
    ADD COLUMN `process_instance_id` VARCHAR(64) NULL COMMENT 'Flowable流程实例ID（为空=纯状态机模式）' AFTER `status`;

-- 2. 新增补交审核节点到 flow_node（processType = 'supplement'）
--    注意：主申报流程已有 supplementAudit（财务补充审核）节点，此处用 materialSupplementAudit 避免冲突
INSERT INTO `flow_node` (`node_key`, `node_name`, `node_type`, `assignee`, `candidate_groups`, `target_status`, `form_section`, `is_system`, `process_type`, `description`) VALUES
('materialSupplementAudit', '补交审核', 'userTask', NULL, 'SUPPLEMENT_AUDITOR', NULL, NULL, 1, 'supplement', '审核资料补交申请（通过后增量资料转正，驳回则清除增量）')
ON DUPLICATE KEY UPDATE
  `node_name` = VALUES(`node_name`),
  `candidate_groups` = VALUES(`candidate_groups`),
  `process_type` = VALUES(`process_type`),
  `description` = VALUES(`description`);

-- 3. 新增补交流程模板
INSERT INTO `flow_template` (`name`, `code`, `description`, `process_type`, `is_default`, `status`, `create_time`, `update_time`) VALUES
('资料补交流程', 'supplement_normal', '资料补交审批（独立流程，不阻塞申报主流程）', 'supplement', 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `description` = VALUES(`description`),
  `status` = 1;

-- 4. 模板-节点编排：materialSupplementAudit
INSERT INTO `flow_template_node` (`template_id`, `node_id`, `enabled`, `sort_order`, `create_time`, `update_time`)
SELECT ft.id, fn.id, 1, 1, NOW(), NOW()
FROM `flow_template` ft, `flow_node` fn
WHERE ft.code = 'supplement_normal' AND fn.node_key = 'materialSupplementAudit'
ON DUPLICATE KEY UPDATE `enabled` = 1, `sort_order` = 1;

-- 5. 新增 SUPPLEMENT_AUDITOR 角色（如果不存在）
INSERT INTO `sys_role` (`role_name`, `role_code`, `description`, `data_scope`, `status`, `deleted`, `create_time`, `update_time`)
SELECT '补交审核员', 'SUPPLEMENT_AUDITOR', '资料补交审核角色', 1, 1, 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `role_code` = 'SUPPLEMENT_AUDITOR');

-- 6. 将补交相关权限菜单分配给 SUPPLEMENT_AUDITOR 角色
--    （资料审核 + 发起补交，按 permission 匹配，避免硬编码菜单ID）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`)
SELECT r.id, m.id, NOW()
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code = 'SUPPLEMENT_AUDITOR'
  AND m.permission IN ('business:declaration:audit:material', 'business:declaration:supplement:initiate');
