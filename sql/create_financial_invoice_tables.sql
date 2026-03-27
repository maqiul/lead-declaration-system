-- 财务票据管理表结构

-- 1. 货代发票表
DROP TABLE IF EXISTS `financial_freight_invoice`;
CREATE TABLE `financial_freight_invoice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `declaration_form_id` BIGINT NOT NULL COMMENT '申报单ID',
  `declaration_form_code` VARCHAR(50) NOT NULL COMMENT '申报单编号',
  `invoice_no` VARCHAR(100) NOT NULL COMMENT '发票号码',
  `invoice_date` DATE NOT NULL COMMENT '开票日期',
  `amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '发票金额',
  `currency` VARCHAR(10) DEFAULT 'CNY' COMMENT '币种',
  `supplier_name` VARCHAR(200) NOT NULL COMMENT '供应商名称',
  `supplier_tax_no` VARCHAR(50) COMMENT '供应商税号',
  `invoice_file_path` VARCHAR(500) COMMENT '发票文件路径',
  `invoice_file_name` VARCHAR(200) COMMENT '发票文件名',
  `status` TINYINT DEFAULT 0 COMMENT '状态 0:待审核 1:已审核 2:已驳回',
  `audit_user_id` BIGINT COMMENT '审核人ID',
  `audit_user_name` VARCHAR(50) COMMENT '审核人姓名',
  `audit_time` DATETIME COMMENT '审核时间',
  `audit_remark` TEXT COMMENT '审核备注',
  `remark` TEXT COMMENT '备注',
  `org_id` BIGINT NOT NULL COMMENT '组织机构ID',
  `create_by` BIGINT COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_no` (`invoice_no`),
  KEY `idx_declaration_form_id` (`declaration_form_id`),
  KEY `idx_status` (`status`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='货代发票表';

-- 2. 报关发票表
DROP TABLE IF EXISTS `financial_customs_invoice`;
CREATE TABLE `financial_customs_invoice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `declaration_form_id` BIGINT NOT NULL COMMENT '申报单ID',
  `declaration_form_code` VARCHAR(50) NOT NULL COMMENT '申报单编号',
  `invoice_no` VARCHAR(100) NOT NULL COMMENT '发票号码',
  `invoice_date` DATE NOT NULL COMMENT '开票日期',
  `amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '发票金额',
  `currency` VARCHAR(10) DEFAULT 'CNY' COMMENT '币种',
  `customs_broker` VARCHAR(200) NOT NULL COMMENT '报关行名称',
  `broker_tax_no` VARCHAR(50) COMMENT '报关行税号',
  `invoice_file_path` VARCHAR(500) COMMENT '发票文件路径',
  `invoice_file_name` VARCHAR(200) COMMENT '发票文件名',
  `status` TINYINT DEFAULT 0 COMMENT '状态 0:待审核 1:已审核 2:已驳回',
  `audit_user_id` BIGINT COMMENT '审核人ID',
  `audit_user_name` VARCHAR(50) COMMENT '审核人姓名',
  `audit_time` DATETIME COMMENT '审核时间',
  `audit_remark` TEXT COMMENT '审核备注',
  `remark` TEXT COMMENT '备注',
  `org_id` BIGINT NOT NULL COMMENT '组织机构ID',
  `create_by` BIGINT COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_no` (`invoice_no`),
  KEY `idx_declaration_form_id` (`declaration_form_id`),
  KEY `idx_status` (`status`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报关发票表';

-- 3. 开票明细表
DROP TABLE IF EXISTS `financial_invoice_detail`;
CREATE TABLE `financial_invoice_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `declaration_form_id` BIGINT NOT NULL COMMENT '申报单ID',
  `declaration_form_code` VARCHAR(50) NOT NULL COMMENT '申报单编号',
  `product_id` BIGINT NOT NULL COMMENT '产品ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '产品名称',
  `hs_code` VARCHAR(20) COMMENT 'HS编码',
  `quantity` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '数量',
  `unit_price` DECIMAL(15,4) NOT NULL DEFAULT 0.0000 COMMENT '单价',
  `total_amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
  `currency` VARCHAR(10) DEFAULT 'CNY' COMMENT '币种',
  `tax_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '税率(%)',
  `tax_amount` DECIMAL(15,2) DEFAULT 0.00 COMMENT '税额',
  `invoice_type` TINYINT NOT NULL COMMENT '发票类型 1:增值税专用发票 2:增值税普通发票 3:其他',
  `invoice_title` VARCHAR(200) COMMENT '发票抬头',
  `tax_number` VARCHAR(50) COMMENT '税号',
  `address_phone` VARCHAR(200) COMMENT '地址电话',
  `bank_account` VARCHAR(100) COMMENT '开户行及账号',
  `status` TINYINT DEFAULT 0 COMMENT '状态 0:待开票 1:已开票 2:已作废',
  `invoice_no` VARCHAR(100) COMMENT '发票号码',
  `invoice_date` DATE COMMENT '开票日期',
  `remark` TEXT COMMENT '备注',
  `org_id` BIGINT NOT NULL COMMENT '组织机构ID',
  `create_by` BIGINT COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_declaration_form_id` (`declaration_form_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_status` (`status`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开票明细表';

-- 4. 票据审核记录表
DROP TABLE IF EXISTS `financial_invoice_audit_log`;
CREATE TABLE `financial_invoice_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `invoice_type` TINYINT NOT NULL COMMENT '票据类型 1:货代发票 2:报关发票 3:开票明细',
  `invoice_id` BIGINT NOT NULL COMMENT '票据ID',
  `declaration_form_id` BIGINT NOT NULL COMMENT '申报单ID',
  `action` TINYINT NOT NULL COMMENT '操作类型 1:提交 2:审核通过 3:审核驳回 4:修改 5:删除',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50) NOT NULL COMMENT '操作人姓名',
  `operate_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark` TEXT COMMENT '操作备注',
  `org_id` BIGINT NOT NULL COMMENT '组织机构ID',
  PRIMARY KEY (`id`),
  KEY `idx_invoice_type_id` (`invoice_type`, `invoice_id`),
  KEY `idx_declaration_form_id` (`declaration_form_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_operate_time` (`operate_time`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='票据审核记录表';