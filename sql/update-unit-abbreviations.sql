-- 更新现有计量单位数据为简写全大写格式
-- 执行此脚本前请备份数据

UPDATE `measurement_units` SET 
  `unit_name_en` = 'PCS' WHERE `unit_code` = '01';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'SETS' WHERE `unit_code` = '02';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'SUITS' WHERE `unit_code` = '03';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'ITEMS' WHERE `unit_code` = '04';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'ONLY' WHERE `unit_code` = '05';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'SHEETS' WHERE `unit_code` = '06';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'BRANCHES' WHERE `unit_code` = '07';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'ROOTS' WHERE `unit_code` = '08';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'STRIPS' WHERE `unit_code` = '09';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'BLOCKS' WHERE `unit_code` = '10';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'KILOS' WHERE `unit_code` = '11';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'GRAMS' WHERE `unit_code` = '12';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'TONS' WHERE `unit_code` = '13';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'CBM' WHERE `unit_code` = '14';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'SQM' WHERE `unit_code` = '15';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'METERS' WHERE `unit_code` = '16';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'CM' WHERE `unit_code` = '17';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'MM' WHERE `unit_code` = '18';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'LITERS' WHERE `unit_code` = '19';
UPDATE `measurement_units` SET 
  `unit_name_en` = 'ML' WHERE `unit_code` = '20';

-- 验证更新结果
SELECT unit_code, unit_name, unit_name_en, unit_type 
FROM measurement_units 
ORDER BY unit_code;