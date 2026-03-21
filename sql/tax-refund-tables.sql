-- 退税功能相关表结构

-- 1. 退税申请主表
CREATE TABLE `tax_refund_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_no` varchar(32) NOT NULL COMMENT '申请编号',
  `declaration_form_id` bigint NOT NULL COMMENT '关联申报单ID',
  `initiator_id` bigint NOT NULL COMMENT '申请人ID',
  `initiator_name` varchar(50) NOT NULL COMMENT '申请人姓名',
  `department_id` bigint DEFAULT NULL COMMENT '申请部门ID',
  `department_name` varchar(100) DEFAULT NULL COMMENT '申请部门名称',
  `application_type` varchar(20) NOT NULL COMMENT '申请类型(EXPORT_REFUND:出口退税,VAT_REFUND:增值税退税,OTHER_REFUND:其他退税)',
  `amount` decimal(15,2) NOT NULL COMMENT '申请金额',
  `invoice_no` varchar(50) NOT NULL COMMENT '发票号码',
  `invoice_amount` decimal(15,2) NOT NULL COMMENT '发票金额',
  `tax_rate` decimal(5,2) NOT NULL COMMENT '税率(%)',
  `expected_refund_amount` decimal(15,2) GENERATED ALWAYS AS ((`invoice_amount` * `tax_rate`) / 100) STORED COMMENT '预计退税金额',
  `description` text COMMENT '申请说明',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0:草稿,1:已提交,2:财务初审,3:文件生成,4:退回补充,5:发票提交,6:财务复审,7:已完成,8:已拒绝)',
  `current_approver_id` bigint DEFAULT NULL COMMENT '当前审批人ID',
  `current_approver_name` varchar(50) DEFAULT NULL COMMENT '当前审批人姓名',
  `reject_reason` text COMMENT '拒绝原因',
  `return_reason` text COMMENT '退回补充原因',
  `first_review_opinion` text COMMENT '初审意见',
  `final_review_opinion` text COMMENT '复审意见',
  `file_path` varchar(255) COMMENT '生成的退税文件路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识(0:未删除,1:已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_declaration_form_id` (`declaration_form_id`),
  KEY `idx_initiator_id` (`initiator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退税申请表';

-- 2. 退税申请附件表
CREATE TABLE `tax_refund_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint NOT NULL COMMENT '退税申请ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) NOT NULL COMMENT '文件类型',
  `attachment_type` varchar(20) NOT NULL COMMENT '附件类型(INVOICE:发票,CONTRACT:合同,OTHER:其他)',
  `description` varchar(500) COMMENT '附件描述',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `uploader_id` bigint NOT NULL COMMENT '上传人ID',
  `uploader_name` varchar(50) NOT NULL COMMENT '上传人姓名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退税申请附件表';

-- 3. 退税审核记录表
CREATE TABLE `tax_refund_review_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint NOT NULL COMMENT '退税申请ID',
  `review_type` varchar(20) NOT NULL COMMENT '审核类型(FIRST_REVIEW:初审,FINAL_REVIEW:复审)',
  `reviewer_id` bigint NOT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(50) NOT NULL COMMENT '审核人姓名',
  `review_result` varchar(20) NOT NULL COMMENT '审核结果(APPROVED:通过,REJECTED:拒绝,RETURNED:退回补充)',
  `review_opinion` text COMMENT '审核意见',
  `review_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `next_approver_id` bigint DEFAULT NULL COMMENT '下一审批人ID',
  `next_approver_name` varchar(50) DEFAULT NULL COMMENT '下一审批人姓名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_reviewer_id` (`reviewer_id`),
  KEY `idx_review_time` (`review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退税审核记录表';

-- 4. 退税流程节点表(用于记录审批流程状态)
CREATE TABLE `tax_refund_process_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint NOT NULL COMMENT '退税申请ID',
  `node_type` varchar(30) NOT NULL COMMENT '节点类型(SUBMIT:提交,FIRST_REVIEW:初审,FILE_GENERATE:文件生成,INVOICE_SUBMIT:发票提交,FINAL_REVIEW:复审,COMPLETE:完成,REJECT:拒绝)',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) NOT NULL COMMENT '操作人姓名',
  `operation_result` varchar(20) COMMENT '操作结果',
  `remark` text COMMENT '备注',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退税流程节点表';

-- 5. 初始化基础数据
-- 插入示例退税申请编号生成规则配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `config_name`, `config_type`, `remark`, `create_time`) 
VALUES 
('tax.refund.application.no.prefix', 'TR', '退税申请编号前缀', 'SYSTEM', '退税申请编号生成前缀', NOW()),
('tax.refund.application.no.pattern', 'yyyyMMdd', '退税申请编号日期格式', 'SYSTEM', '退税申请编号日期部分格式', NOW());

-- 6. 初始化菜单权限(如果还没有的话)
-- 退税申请菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`, `create_time`) 
SELECT 
  (SELECT id FROM sys_menu WHERE menu_code = 'system' AND menu_type = 1), 
  '税务退费', 'system_tax_refund', 1, '/tax-refund', 'Layout', 'system:tax-refund:view', 'DollarOutlined', 7, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund');

-- 获取退税菜单ID
SET @tax_refund_menu_id = (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund' AND menu_type = 1);

-- 退税子菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`, `create_time`) VALUES
(@tax_refund_menu_id, '退税申请', 'system_tax_refund_apply', 2, 'apply', 'tax-refund/apply/index', 'system:tax-refund:apply', 'PlusOutlined', 1, 1, NOW()),
(@tax_refund_menu_id, '申请列表', 'system_tax_refund_list', 2, 'list', 'tax-refund/list/index', 'system:tax-refund:list', 'UnorderedListOutlined', 2, 1, NOW()),
(@tax_refund_menu_id, '财务审核', 'system_tax_refund_finance', 2, 'finance-review', 'tax-refund/finance-review/index', 'system:tax-refund:finance', 'AuditOutlined', 3, 1, NOW());

-- 按钮权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`, `create_time`) 
SELECT 
  (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
  '申请查询', 'system_tax_refund_query', 3, '', '', 'system:tax-refund:query', '', 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_query');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`, `create_time`) 
SELECT 
  (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
  '申请新增', 'system_tax_refund_add', 3, '', '', 'system:tax-refund:add', '', 2, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_add');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`, `create_time`) 
SELECT 
  (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
  '申请修改', 'system_tax_refund_edit', 3, '', '', 'system:tax-refund:edit', '', 3, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_edit');

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`, `create_time`) 
SELECT 
  (SELECT id FROM sys_menu WHERE menu_code = 'system_tax_refund_list'), 
  '申请删除', 'system_tax_refund_delete', 3, '', '', 'system:tax-refund:delete', '', 4, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'system_tax_refund_delete');

-- 为管理员角色分配权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 1, id FROM `sys_menu` WHERE `menu_code` LIKE 'system_tax_refund%'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 7. 验证表创建结果
SELECT 
  TABLE_NAME as '表名',
  TABLE_COMMENT as '表说明',
  CREATE_TIME as '创建时间'
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME LIKE 'tax_refund_%'
ORDER BY TABLE_NAME;