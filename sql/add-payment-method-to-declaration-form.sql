-- 为 declaration_form 表添加支付方式字段
ALTER TABLE declaration_form ADD COLUMN payment_method VARCHAR(50) DEFAULT NULL COMMENT '支付方式';

-- 更新现有数据（可选）
-- UPDATE declaration_form SET payment_method = 'T/T' WHERE payment_method IS NULL;
