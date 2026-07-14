-- ============================================================
-- 50-出款水单模块（独立管理，支持与申报单多对多关联）
-- ============================================================

-- 1. 出款水单主表
CREATE TABLE IF NOT EXISTS `payment_remittance` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `form_id` BIGINT DEFAULT NULL COMMENT '关联申报单ID（保留兼容，新流程使用关联表）',
  `payment_no` VARCHAR(50) NOT NULL COMMENT '出款水单编号',
  `payee_name` VARCHAR(200) DEFAULT NULL COMMENT '收款人',
  `payment_date` DATE DEFAULT NULL COMMENT '出款日期',
  `payment_amount` DECIMAL(18,4) DEFAULT NULL COMMENT '出款金额',
  `currency` VARCHAR(10) DEFAULT 'USD' COMMENT '币种',
  `bank_account_id` BIGINT DEFAULT NULL COMMENT '银行账户ID',
  `bank_account_name` VARCHAR(200) DEFAULT NULL COMMENT '银行名称',
  `photo_url` VARCHAR(500) DEFAULT NULL COMMENT '凭证照片URL',
  `remarks` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `status` INT NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿 1-待审核 2-已审核 3-已驳回',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `audit_by` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `audit_by_name` VARCHAR(100) DEFAULT NULL COMMENT '审核人姓名',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `process_instance_id` VARCHAR(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `submit_time` DATETIME DEFAULT NULL COMMENT '提交时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` BIGINT DEFAULT NULL,
  `update_by` BIGINT DEFAULT NULL,
  INDEX `idx_payment_no` (`payment_no`),
  INDEX `idx_status` (`status`),
  INDEX `idx_form_id` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出款水单表';

-- 2. 出款水单与申报单关联表（多对多）
CREATE TABLE IF NOT EXISTS `payment_remittance_form_relation` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `remittance_id` BIGINT NOT NULL COMMENT '出款水单ID',
  `form_id` BIGINT NOT NULL COMMENT '申报单ID',
  `relation_type` INT NOT NULL DEFAULT 1 COMMENT '关联类型: 1-主关联 2-副关联',
  `relation_amount` DECIMAL(18,4) DEFAULT NULL COMMENT '关联金额',
  `remarks` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `create_by` BIGINT DEFAULT NULL,
  UNIQUE KEY `uk_remittance_form` (`remittance_id`, `form_id`),
  INDEX `idx_remittance_id` (`remittance_id`),
  INDEX `idx_form_id` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出款水单与申报单关联表';
