-- 为申报单产品明细表添加中文名和英文名字段
ALTER TABLE `declaration_product`
ADD COLUMN `product_chinese_name` VARCHAR(255) COMMENT '产品中文名',
ADD COLUMN `product_english_name` VARCHAR(255) COMMENT '产品英文名';

-- 更新现有数据，如果需要的话可以基于现有的产品名称字段进行处理
-- 例如，如果现有产品名称是中英文混合的，可以根据一定规则拆分
-- UPDATE `declaration_product` 
-- SET product_chinese_name = SUBSTRING_INDEX(product_name, '/', 1),
--     product_english_name = SUBSTRING_INDEX(product_name, '/', -1)
-- WHERE product_name LIKE '%/%';