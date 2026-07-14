-- ============================================================
-- 流程模板配置功能：表结构 + 预置数据 + 菜单权限
-- ============================================================

-- 1. 流程模板主表
CREATE TABLE IF NOT EXISTS `flow_template` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL COMMENT '模板名称（如：标准流程、简化流程）',
  `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
  `description` VARCHAR(500) COMMENT '模板说明',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认模板 0-否 1-是',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` BIGINT DEFAULT NULL,
  `update_by` BIGINT DEFAULT NULL,
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标志 0-正常 1-删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板';

-- 2. 模板步骤配置表
CREATE TABLE IF NOT EXISTS `flow_template_step` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `template_id` BIGINT NOT NULL COMMENT '所属模板ID',
  `step_key` VARCHAR(50) NOT NULL COMMENT 'BPMN任务定义Key',
  `step_name` VARCHAR(100) COMMENT '步骤中文名',
  `enabled` TINYINT DEFAULT 1 COMMENT '1=启用 0=跳过',
  `target_status` INT DEFAULT NULL COMMENT '进入该步骤时 declaration_form.status 应设为的值',
  `form_section` VARCHAR(50) DEFAULT NULL COMMENT '对应前端表单区块组件标识',
  `sort_order` INT DEFAULT 0 COMMENT '排序（越小越靠前）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_tpl_step` (`template_id`, `step_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板步骤配置';

-- 3. 预置"标准流程"模板（ID=1，全部9步启用）
INSERT INTO `flow_template` (`id`, `name`, `code`, `description`, `is_default`, `status`)
VALUES (1, '标准流程', 'STANDARD', '完整申报流程，包含所有审核环节', 1, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = 1;

INSERT INTO `flow_template_step` (`template_id`, `step_key`, `step_name`, `enabled`, `target_status`, `form_section`, `sort_order`) VALUES
(1, 'deptAudit',            '初审',         1, 1, 'basic',         1),
(1, 'materialSubmit',       '资料提交',     1, 2, 'material',      2),
(1, 'materialAudit',        '资料审核',     1, 3, 'material',      3),
(1, 'supplementSubmit',     '补充资料提交', 1, 4, 'supplement',    4),
(1, 'supplementAudit',      '补充资料审核', 1, 5, 'supplement',    5),
(1, 'invoiceAmountSubmit',  '申请开票金额', 1, 6, 'invoiceAmount', 6),
(1, 'invoiceAmountAudit',   '开票金额审核', 1, 7, 'invoiceAmount', 7),
(1, 'invoiceSubmit',        '业务发票提交', 1, 8, 'invoice',       8),
(1, 'invoiceAudit',         '业务发票审核', 1, 9, 'invoice',       9)
ON DUPLICATE KEY UPDATE `step_name` = VALUES(`step_name`), `target_status` = VALUES(`target_status`), `form_section` = VALUES(`form_section`), `sort_order` = VALUES(`sort_order`);

-- 4. 菜单数据（挂在系统管理 parent_id=2）
-- 页面菜单 ID=2050
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
VALUES
  (2050, '流程模板', 'flow-template', 100, 2, 'flow-template', 'system/flow-template/index.vue',
   'system:flow-template:view', 'BranchesOutlined', 20, 0, 0, 1, 1, 0,
   NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `component` = VALUES(`component`),
  `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 按钮权限 2051-2054
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
VALUES
  (2051, '模板查询', 'flow-template-query',  2050, 3, NULL, NULL, 'system:flow-template:query',  NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
  (2052, '模板新增', 'flow-template-add',    2050, 3, NULL, NULL, 'system:flow-template:add',    NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
  (2053, '模板编辑', 'flow-template-update', 2050, 3, NULL, NULL, 'system:flow-template:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
  (2054, '模板删除', 'flow-template-delete', 2050, 3, NULL, NULL, 'system:flow-template:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 5. 分配给超级管理员
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2050);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2051);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2052);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2053);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2054);
