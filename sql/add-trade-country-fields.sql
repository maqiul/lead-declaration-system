-- 为申报单产品明细表添加产品中英文名称字段
ALTER TABLE `declaration_product`
ADD COLUMN `product_chinese_name` VARCHAR(255) COMMENT '产品中文名',
ADD COLUMN `product_english_name` VARCHAR(255) COMMENT '产品英文名';