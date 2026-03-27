-- 为financial_supplement表添加计算明细JSON字段
ALTER TABLE financial_supplement ADD COLUMN calculation_detail TEXT NULL COMMENT '计算明细JSON';