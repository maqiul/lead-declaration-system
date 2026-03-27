-- 为申报单主表添加贸易国字段
ALTER TABLE `declaration_form`
ADD COLUMN `trade_country` VARCHAR(10) COMMENT '贸易国';