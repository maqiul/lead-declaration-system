-- 银行账户配置表
CREATE TABLE `bank_account_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_name` varchar(100) NOT NULL COMMENT '账户名称',
  `bank_name` varchar(100) NOT NULL COMMENT '银行名称',
  `bank_code` varchar(20) COMMENT '银行代码',
  `account_number` varchar(50) NOT NULL COMMENT '银行账号',
  `swift_code` varchar(20) COMMENT 'SWIFT代码',
  `iban` varchar(50) COMMENT 'IBAN号码',
  `account_holder` varchar(100) NOT NULL COMMENT '账户持有人',
  `currency` varchar(10) NOT NULL DEFAULT 'USD' COMMENT '账户币种',
  `branch_name` varchar(100) COMMENT '支行名称',
  `branch_address` varchar(200) COMMENT '支行地址',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认账户 0-否 1-是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `remarks` varchar(500) COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_bank_name` (`bank_name`),
  KEY `idx_account_holder` (`account_holder`),
  KEY `idx_currency` (`currency`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='银行账户配置表';

-- 插入默认银行账户配置
INSERT INTO `bank_account_config` (`account_name`, `bank_name`, `bank_code`, `account_number`, `swift_code`, `account_holder`, `currency`, `is_default`, `sort`, `remarks`) VALUES
('美元基本账户', '中国银行', 'BOC', '1234567890123456', 'BKCHCNBJ', '宁波智翼科技有限公司', 'USD', 1, 1, '公司主要美元收款账户'),
('欧元账户', '工商银行', 'ICBC', '9876543210987654', 'ICBKCNBJ', '宁波智翼科技有限公司', 'EUR', 0, 2, '欧洲业务收款账户'),
('人民币账户', '建设银行', 'CCB', '1122334455667788', 'PCBCCNBJ', '宁波智翼科技有限公司', 'CNY', 0, 3, '国内业务收款账户');