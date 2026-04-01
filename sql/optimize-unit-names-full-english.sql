-- 优化计量单位英文名称为完整单词形式
-- 用于更友好的 UI 显示和国际化场景

-- 数量单位
UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'piece', 
    `unit_name_en` = 'pieces' 
WHERE `unit_code` = '01';

UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'set', 
    `unit_name_en` = 'sets' 
WHERE `unit_code` = '02';

UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'suit', 
    `unit_name_en` = 'suits' 
WHERE `unit_code` = '03';

UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'item', 
    `unit_name_en` = 'items' 
WHERE `unit_code` = '04';

UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'head', 
    `unit_name_en` = 'heads' 
WHERE `unit_code` = '05';

UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'sheet', 
    `unit_name_en` = 'sheets' 
WHERE `unit_code` = '06';

-- 重量单位
UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'kilo', 
    `unit_name_en` = 'kilos' 
WHERE `unit_code` = '11';

UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'gram', 
    `unit_name_en` = 'grams' 
WHERE `unit_code` = '12';

UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'ton', 
    `unit_name_en` = 'tons' 
WHERE `unit_code` = '13';

-- 体积单位
UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'cubic meter', 
    `unit_name_en` = 'cubic meters' 
WHERE `unit_code` = '14';

-- 面积单位
UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'square meter', 
    `unit_name_en` = 'square meters' 
WHERE `unit_code` = '15';

-- 长度单位
UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'meter', 
    `unit_name_en` = 'meters' 
WHERE `unit_code` = '16';

UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'centimeter', 
    `unit_name_en` = 'centimeters' 
WHERE `unit_code` = '17';

UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'millimeter', 
    `unit_name_en` = 'millimeters' 
WHERE `unit_code` = '18';

-- 容量单位
UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'liter', 
    `unit_name_en` = 'liters' 
WHERE `unit_code` = '19';

UPDATE `measurement_units` SET 
    `unit_name_en_singular` = 'milliliter', 
    `unit_name_en` = 'milliliters' 
WHERE `unit_code` = '20';
