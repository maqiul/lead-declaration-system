-- 73: 箱子单箱尺寸（长宽高，cm）+ EXW 贸易方式杂费字段
-- 1) 箱子表增加单箱长宽高，前端按 长*宽*高*数量 自动计算该行总体积(CBM)
ALTER TABLE declaration_carton
    ADD COLUMN length_cm DECIMAL(10, 2) DEFAULT NULL COMMENT '单箱长度(cm)' AFTER quantity,
    ADD COLUMN width_cm  DECIMAL(10, 2) DEFAULT NULL COMMENT '单箱宽度(cm)' AFTER length_cm,
    ADD COLUMN height_cm DECIMAL(10, 2) DEFAULT NULL COMMENT '单箱高度(cm)' AFTER width_cm;

-- 2) 申报表增加杂费字段（贸易方式为 EXW 时前端展示录入）
ALTER TABLE declaration_form
    ADD COLUMN misc_fee DECIMAL(14, 2) DEFAULT NULL COMMENT '杂费(EXW贸易方式时录入)' AFTER trade_term;
