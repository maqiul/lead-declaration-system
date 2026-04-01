-- measurement_units 表添加单数英文名称字段
-- 用于区分英文单数和复数形式

-- 1. 添加单数英文名称字段
ALTER TABLE `measurement_units` 
ADD COLUMN `unit_name_en_singular` varchar(50) COMMENT '英文单位单数名称' AFTER `unit_name_en`;

-- 2. 更新现有数据，填充单数形式（大部分情况下单数和复数相同，特殊情况的单独处理）
UPDATE `measurement_units` SET `unit_name_en_singular` = `unit_name_en`;

-- 3. 特殊情况：修正单复数不同的单位
-- 目前数据中都是大写缩写形式，单复数同形，无需特殊处理
-- 如果后续需要区分，例如：piece/pieces, box/boxes 等，可以在这里补充

-- 示例：如果有以下单位，可以这样区分
-- UPDATE `measurement_units` SET `unit_name_en_singular` = 'piece' WHERE `unit_code` = 'XX' AND `unit_name_en` = 'pieces';
-- UPDATE `measurement_units` SET `unit_name_en_singular` = 'box' WHERE `unit_code` = 'XX' AND `unit_name_en` = 'boxes';
