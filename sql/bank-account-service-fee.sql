-- 银行账户配置表添加手续费率字段
ALTER TABLE bank_account_config ADD COLUMN service_fee_rate DECIMAL(10,6) DEFAULT 0 COMMENT '手续费率';
