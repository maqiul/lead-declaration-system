-- 业务审核记录表 (用于通用审核流程，如退回草稿申请)
CREATE TABLE `business_audit_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_id` bigint(20) NOT NULL COMMENT '业务记录控制ID (如申报单ID)',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型 (如 DECLARATION_RETURN)',
  `applicant_id` bigint(20) NOT NULL COMMENT '申请人常用ID',
  `apply_reason` text COMMENT '申请原因',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `auditor_id` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `audit_status` int(4) DEFAULT '0' COMMENT '审核状态: 0-待审核, 1-通过, 2-驳回',
  `audit_remark` text COMMENT '审核备注/驳回原因',
  `audit_time` datetime DEFAULT NULL COMMENT '审核完成时间',
  `pre_status` int(4) DEFAULT NULL COMMENT '申请前的原始业务状态',
  PRIMARY KEY (`id`),
  KEY `idx_business` (`business_id`,`business_type`),
  KEY `idx_applicant` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用业务审核记录表';
