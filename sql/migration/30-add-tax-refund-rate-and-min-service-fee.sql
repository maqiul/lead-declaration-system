-- HS商品类型配置表：新增退税率字段
ALTER TABLE product_type_config ADD COLUMN tax_refund_rate DECIMAL(5,2) DEFAULT NULL COMMENT '退税率（如 13.00 表示 13%）' AFTER unit_name;

-- 银行账户配置表：新增最低操作费字段
ALTER TABLE bank_account_config ADD COLUMN min_service_fee DECIMAL(12,2) DEFAULT NULL COMMENT '最低操作费金额（计算出的手续费低于此值时，按此值收取）' AFTER service_fee_rate;

-- 水单表：新增内部手续费字段（人民币，审核时自动计算）
ALTER TABLE declaration_remittance ADD COLUMN internal_bank_fee DECIMAL(12,2) DEFAULT NULL COMMENT '内部手续费（人民币）= 汇率×收汇金额×银行手续费率，审核时自动计算' AFTER bank_fee;
