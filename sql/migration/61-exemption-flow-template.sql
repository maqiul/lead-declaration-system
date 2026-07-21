-- ============================================================
-- 资料豁免流程 - 节点库 + 流程模板
-- 普通豁免: exemptionAudit → end (1步审核)
-- 发票豁免: exemptionAudit → exemptionInvoiceAudit → end (2步审核)
-- ============================================================

-- 1. 新增豁免流程节点到 flow_node（processType = 'exemption'）
INSERT INTO `flow_node` (`node_key`, `node_name`, `node_type`, `assignee`, `candidate_groups`, `target_status`, `form_section`, `is_system`, `process_type`, `description`) VALUES
('exemptionAudit',        '豁免审核',       'userTask', NULL, 'EXEMPTION_AUDITOR', NULL, NULL, 1, 'exemption', '审核资料缺失豁免申请（普通文件1步）'),
('exemptionInvoiceAudit', '豁免复核',       'userTask', NULL, 'EXEMPTION_AUDITOR', NULL, NULL, 1, 'exemption', '发票类文件豁免复核（金额/信息确认）')
ON DUPLICATE KEY UPDATE
  `node_name` = VALUES(`node_name`),
  `candidate_groups` = VALUES(`candidate_groups`),
  `process_type` = VALUES(`process_type`),
  `description` = VALUES(`description`);

-- 2. 新增豁免流程模板（2个）
INSERT INTO `flow_template` (`name`, `code`, `description`, `process_type`, `is_default`, `status`, `create_time`, `update_time`) VALUES
('普通资料豁免流程', 'exemption_normal', '普通文件缺失豁免审批（1步审核）', 'exemption', 0, 1, NOW(), NOW()),
('发票资料豁免流程', 'exemption_invoice', '发票类文件缺失豁免审批（2步审核）', 'exemption', 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `description` = VALUES(`description`),
  `status` = 1;

-- 3. 模板-节点编排
-- 普通豁免模板: exemptionAudit
INSERT INTO `flow_template_node` (`template_id`, `node_id`, `enabled`, `sort_order`, `create_time`, `update_time`)
SELECT ft.id, fn.id, 1, 1, NOW(), NOW()
FROM `flow_template` ft, `flow_node` fn
WHERE ft.code = 'exemption_normal' AND fn.node_key = 'exemptionAudit'
ON DUPLICATE KEY UPDATE `enabled` = 1, `sort_order` = 1;

-- 发票豁免模板: exemptionAudit → exemptionInvoiceAudit
INSERT INTO `flow_template_node` (`template_id`, `node_id`, `enabled`, `sort_order`, `create_time`, `update_time`)
SELECT ft.id, fn.id, 1, 1, NOW(), NOW()
FROM `flow_template` ft, `flow_node` fn
WHERE ft.code = 'exemption_invoice' AND fn.node_key = 'exemptionAudit'
ON DUPLICATE KEY UPDATE `enabled` = 1, `sort_order` = 1;

INSERT INTO `flow_template_node` (`template_id`, `node_id`, `enabled`, `sort_order`, `create_time`, `update_time`)
SELECT ft.id, fn.id, 1, 2, NOW(), NOW()
FROM `flow_template` ft, `flow_node` fn
WHERE ft.code = 'exemption_invoice' AND fn.node_key = 'exemptionInvoiceAudit'
ON DUPLICATE KEY UPDATE `enabled` = 1, `sort_order` = 2;

-- 4. 新增 EXEMPTION_AUDITOR 角色（如果不存在）
INSERT INTO `sys_role` (`role_name`, `role_code`, `description`, `data_scope`, `status`, `deleted`, `create_time`, `update_time`)
SELECT '豁免审核员', 'EXEMPTION_AUDITOR', '资料豁免审核角色', 1, 1, 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `role_code` = 'EXEMPTION_AUDITOR');

-- 5. 将豁免审核权限菜单分配给 EXEMPTION_AUDITOR 角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`)
SELECT r.id, m.id, NOW()
FROM `sys_role` r, `sys_menu` m
WHERE r.role_code = 'EXEMPTION_AUDITOR'
  AND m.permission = 'business:declaration:exemption:audit';
