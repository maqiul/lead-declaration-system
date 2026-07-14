-- ============================================================
-- 全局流程节点库 + 模板-节点编排表
-- ============================================================

-- 1. 流程节点库（全局共享）
CREATE TABLE IF NOT EXISTS `flow_node` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `node_key` VARCHAR(50) NOT NULL COMMENT 'BPMN taskDefinitionKey（如 deptAudit, materialSubmit）',
  `node_name` VARCHAR(100) NOT NULL COMMENT '节点中文名',
  `node_type` VARCHAR(20) NOT NULL DEFAULT 'userTask' COMMENT '节点类型: userTask / serviceTask',
  `assignee` VARCHAR(100) DEFAULT NULL COMMENT '办理人表达式（如 ${starterId}）',
  `candidate_groups` VARCHAR(255) DEFAULT NULL COMMENT '候选组（如 MATERIAL_AUDITOR），多个逗号分隔',
  `target_status` INT DEFAULT NULL COMMENT '到达此节点时 declaration_form.status 值',
  `form_section` VARCHAR(50) DEFAULT NULL COMMENT '对应前端表单区块: basic/material/supplement/invoiceAmount/invoice',
  `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统内置节点 0-否 1-是（不可删除）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '节点说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_node_key` (`node_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点库';

-- 2. 预置系统节点（9个业务节点 + 4个系统服务节点）
INSERT INTO `flow_node` (`node_key`, `node_name`, `node_type`, `assignee`, `candidate_groups`, `target_status`, `form_section`, `is_system`, `description`) VALUES
-- 业务 userTask 节点
('deptAudit',            '初审',         'userTask', '${starterId}',  NULL,                1, 'basic',         1, '部门初审，审核申报单基本信息'),
('materialSubmit',       '资料提交',     'userTask', '${starterId}',  NULL,                2, 'material',      1, '申报人提交资料'),
('materialAudit',        '资料审核',     'userTask', NULL,            'MATERIAL_AUDITOR',  3, 'material',      1, '审核员审核资料'),
('supplementSubmit',     '补充资料提交', 'userTask', '${starterId}',  NULL,                4, 'supplement',    1, '申报人提交补充资料'),
('supplementAudit',      '补充资料审核', 'userTask', NULL,            'MATERIAL_AUDITOR',  5, 'supplement',    1, '审核员审核补充资料'),
('invoiceAmountSubmit',  '申请开票金额', 'userTask', '${starterId}',  NULL,                6, 'invoiceAmount', 1, '申报人提交开票金额申请'),
('invoiceAmountAudit',   '开票金额审核', 'userTask', NULL,            'FINANCE_AUDITOR',   7, 'invoiceAmount', 1, '财务审核开票金额'),
('invoiceSubmit',        '业务发票提交', 'userTask', '${starterId}',  NULL,                8, 'invoice',       1, '申报人提交业务发票'),
('invoiceAudit',         '业务发票审核', 'userTask', NULL,            'FINANCE_AUDITOR',   9, 'invoice',       1, '财务审核业务发票'),
-- 系统 serviceTask 节点（不参与模板编排，BPMN 生成时自动插入）
('genPreEntryTask',      '生成预录入单', 'serviceTask', NULL,         NULL,                NULL, NULL, 1, '自动生成预录入单（系统服务节点）'),
('genCustomsDoc',        '生成海关报关单','serviceTask', NULL,         NULL,                2,    NULL, 1, '自动生成海关报关单（系统服务节点）'),
('rejectHandler',        '驳回处理',     'serviceTask', NULL,         NULL,                0,    NULL, 1, '驳回后回退到草稿状态（系统服务节点）'),
('endEvent',             '流程结束',     'serviceTask', NULL,         NULL,                10,   NULL, 1, '流程结束节点')
ON DUPLICATE KEY UPDATE
  `node_name` = VALUES(`node_name`),
  `node_type` = VALUES(`node_type`),
  `target_status` = VALUES(`target_status`),
  `form_section` = VALUES(`form_section`),
  `is_system` = VALUES(`is_system`);

-- 3. 模板-节点编排表（替代 flow_template_step）
CREATE TABLE IF NOT EXISTS `flow_template_node` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `template_id` BIGINT NOT NULL COMMENT '所属模板ID',
  `node_id` BIGINT NOT NULL COMMENT '引用 flow_node.id',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=跳过',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_tpl_node` (`template_id`, `node_id`),
  INDEX `idx_template_id` (`template_id`),
  INDEX `idx_node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板-节点编排';

-- 4. 从 flow_template_step 迁移数据到 flow_template_node
INSERT INTO `flow_template_node` (`template_id`, `node_id`, `enabled`, `sort_order`, `create_time`, `update_time`)
SELECT
  fts.template_id,
  fn.id AS node_id,
  fts.enabled,
  fts.sort_order,
  fts.create_time,
  fts.update_time
FROM `flow_template_step` fts
INNER JOIN `flow_node` fn ON fn.node_key = fts.step_key
ON DUPLICATE KEY UPDATE
  `enabled` = VALUES(`enabled`),
  `sort_order` = VALUES(`sort_order`);

-- 5. 菜单：流程节点库（挂在系统管理 parent_id=2）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
VALUES
  (2060, '流程节点', 'flow-node', 3, 2, 'flow-node', 'system/flow-node/index.vue',
   'system:flow-node:view', 'ApartmentOutlined', 21, 0, 0, 1, 1, 0,
   NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `component` = VALUES(`component`),
  `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 按钮权限 2061-2064
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
VALUES
  (2061, '节点查询', 'flow-node-query',  2060, 3, NULL, NULL, 'system:flow-node:query',  NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
  (2062, '节点新增', 'flow-node-add',    2060, 3, NULL, NULL, 'system:flow-node:add',    NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
  (2063, '节点编辑', 'flow-node-update', 2060, 3, NULL, NULL, 'system:flow-node:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
  (2064, '节点删除', 'flow-node-delete', 2060, 3, NULL, NULL, 'system:flow-node:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 6. 分配给超级管理员
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2060);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2061);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2062);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2063);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2064);
