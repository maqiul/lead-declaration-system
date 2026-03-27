CREATE TABLE IF NOT EXISTS `financial_invoice` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` bigint(20) NOT NULL COMMENT '关联申报单ID',
  `form_no` varchar(64) NOT NULL COMMENT '关联申报单号',
  
  `freight_amount` decimal(15,2) DEFAULT NULL COMMENT '货代发票金额',
  `freight_invoice_no` varchar(64) DEFAULT NULL COMMENT '货代发票号',
  `freight_file_name` varchar(255) DEFAULT NULL COMMENT '货代发票文件名',
  `freight_file_url` varchar(500) DEFAULT NULL COMMENT '货代发票URL',
  
  `customs_amount` decimal(15,2) DEFAULT NULL COMMENT '报关代理发票金额',
  `customs_invoice_no` varchar(64) DEFAULT NULL COMMENT '报关单发票号',
  `customs_file_name` varchar(255) DEFAULT NULL COMMENT '报关代理发票文件名',
  `customs_file_url` varchar(500) DEFAULT NULL COMMENT '报关代理发票URL',
  
  `details_amount` decimal(15,2) DEFAULT NULL COMMENT '开票明细金额',
  `details_invoice_no` varchar(64) DEFAULT NULL COMMENT '开票号',
  `details_file_name` varchar(255) DEFAULT NULL COMMENT '开票明细文件名',
  `details_file_url` varchar(500) DEFAULT NULL COMMENT '开票明细URL',
  
  `currency` varchar(10) DEFAULT 'CNY' COMMENT '币种',
  
  `status` int(11) DEFAULT '0' COMMENT '状态 0-待上传, 1-已提交',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务开票补充表';
