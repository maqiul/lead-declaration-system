-- 退税申请表添加审核人信息字段
-- 用于记录初审和复审的审核人ID、名称和时间

ALTER TABLE tax_refund_application 
ADD COLUMN first_reviewer_id BIGINT COMMENT '初审人ID',
ADD COLUMN first_reviewer_name VARCHAR(50) COMMENT '初审人名称',
ADD COLUMN first_review_time DATETIME COMMENT '初审时间',
ADD COLUMN final_reviewer_id BIGINT COMMENT '复审人ID',
ADD COLUMN final_reviewer_name VARCHAR(50) COMMENT '复审人名称',
ADD COLUMN final_review_time DATETIME COMMENT '复审时间';
