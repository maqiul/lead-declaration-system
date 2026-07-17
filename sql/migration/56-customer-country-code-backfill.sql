-- ============================================================
-- 常用客户：目的国 / 贸易国 存量数据统一为英文全名（english_name）
--   背景：系统规范存储格式为英文全名（申报记录、导出均用英文名），
--         country_code 仅作为申报表单下拉的 value。
--   部分历史数据（如申报页快速新增）可能把 country_code 直接落库，
--         此脚本将这些 code 回填为对应的英文全名。
--   幂等：仅转换“当前值命中 country_code”的记录；已是英文名的不动。
-- ============================================================

SET NAMES utf8mb4;

-- 1. 目的国：code -> english_name
UPDATE `customer_config` c
JOIN `country_info` ci ON c.`destination_country` = ci.`country_code` AND ci.`del_flag` = 0
SET c.`destination_country` = ci.`english_name`
WHERE c.`destination_country` IS NOT NULL
  AND c.`destination_country` <> '';

-- 2. 贸易国：code -> english_name
UPDATE `customer_config` c
JOIN `country_info` ci ON c.`trade_country` = ci.`country_code` AND ci.`del_flag` = 0
SET c.`trade_country` = ci.`english_name`
WHERE c.`trade_country` IS NOT NULL
  AND c.`trade_country` <> '';

-- 3. 验证：列出仍无法匹配为有效 english_name 的记录（需人工核对）
SELECT id, customer_name, destination_country, trade_country
FROM `customer_config`
WHERE (destination_country IS NOT NULL AND destination_country <> ''
        AND destination_country NOT IN (SELECT english_name FROM country_info WHERE del_flag = 0))
   OR (trade_country IS NOT NULL AND trade_country <> ''
        AND trade_country NOT IN (SELECT english_name FROM country_info WHERE del_flag = 0));
