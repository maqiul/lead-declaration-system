-- 税务退费申请表
CREATE TABLE IF NOT EXISTS `tax_refund_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_no` varchar(50) NOT NULL COMMENT '申请编号',
  `initiator_id` bigint NOT NULL COMMENT '发起人ID',
  `initiator_name` varchar(50) NOT NULL COMMENT '发起人姓名',
  `department_id` bigint DEFAULT NULL COMMENT '发起部门ID',
  `department_name` varchar(100) DEFAULT NULL COMMENT '发起部门名称',
  `application_type` varchar(50) NOT NULL COMMENT '申请类型',
  `amount` decimal(15,2) NOT NULL COMMENT '申请金额',
  `description` text COMMENT '申请说明',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '当前状态：0-草稿 1-部门提交 2-财务初审 3-文件生成 4-退回补充 5-发票提交 6-财务复审 7-已完成 8-已拒绝',
  `first_review_opinion` text COMMENT '财务初审意见',
  `first_reviewer_id` bigint DEFAULT NULL COMMENT '财务初审人ID',
  `first_reviewer_name` varchar(50) DEFAULT NULL COMMENT '财务初审人姓名',
  `first_review_time` datetime DEFAULT NULL COMMENT '财务初审时间',
  `generated_file_path` varchar(255) DEFAULT NULL COMMENT '生成的文件路径',
  `invoice_no` varchar(50) DEFAULT NULL COMMENT '发票号码',
  `invoice_amount` decimal(15,2) DEFAULT NULL COMMENT '发票金额',
  `tax_rate` decimal(5,2) DEFAULT NULL COMMENT '税率',
  `refund_amount` decimal(15,2) DEFAULT NULL COMMENT '退税金额',
  `invoice_image_path` varchar(255) DEFAULT NULL COMMENT '发票图片路径',
  `final_review_opinion` text COMMENT '财务复审意见',
  `final_reviewer_id` bigint DEFAULT NULL COMMENT '财务复审人ID',
  `final_reviewer_name` varchar(50) DEFAULT NULL COMMENT '财务复审人姓名',
  `final_review_time` datetime DEFAULT NULL COMMENT '财务复审时间',
  `archive_file_path` varchar(255) DEFAULT NULL COMMENT '归档文件路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_initiator_id` (`initiator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='税务退费申请表';

-- 税务退费申请附件表
CREATE TABLE IF NOT EXISTS `tax_refund_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint NOT NULL COMMENT '申请ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_url` varchar(500) NOT NULL COMMENT '文件URL',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传人ID',
  `uploader_name` varchar(50) DEFAULT NULL COMMENT '上传人姓名',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='税务退费申请附件表';

-- 税务退费操作历史表
CREATE TABLE IF NOT EXISTS `tax_refund_operation_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint NOT NULL COMMENT '申请ID',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) NOT NULL COMMENT '操作人姓名',
  `action` varchar(50) NOT NULL COMMENT '操作类型：submit-提交 approve-审核通过 reject-审核拒绝 return-退回补充 complete-流程完成',
  `remark` text COMMENT '操作备注',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='税务退费操作历史表';

-- 插入初始数据
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `sort`, `status`) VALUES
(0, '税务退费', 'tax_refund', 1, '/business/tax-refund', '', 'business:tax-refund:view', 10, 1),
(100, '退费申请', 'tax_refund_apply', 2, '/business/tax-refund/apply', 'business/tax-refund/application', 'business:tax-refund:apply', 1, 1),
(100, '我的申请', 'tax_refund_my', 2, '/business/tax-refund/my', 'business/tax-refund/my-list', 'business:tax-refund:my', 2, 1),
(100, '待审核', 'tax_refund_pending', 2, '/business/tax-refund/pending', 'business/tax-refund/pending-list', 'business:tax-refund:pending', 3, 1);

-- 为财务角色添加权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 2, id FROM `sys_menu` WHERE `menu_code` LIKE 'tax_refund%';

-- 插入流程定义
INSERT INTO `process_definition` (`process_key`, `process_name`, `description`, `category`, `version`, `status`, `bpmn_xml`) VALUES
('tax_refund_process', '税务退费审批流程', '处理税务退费申请的完整审批流程', 'FINANCE', 1, 1, '<bpmn内容>');

-- 创建索引优化查询性能
CREATE INDEX idx_tax_refund_app_dept_status ON tax_refund_application(department_id, status);
CREATE INDEX idx_tax_refund_app_type_status ON tax_refund_application(application_type, status);
CREATE INDEX idx_tax_refund_attach_type ON tax_refund_attachment(file_type);