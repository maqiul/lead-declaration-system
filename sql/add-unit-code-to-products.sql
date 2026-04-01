-- 为申报产品表添加单位代码字段
-- 用于关联计量单位配置表

-- 1. 添加 unit_code 字段到 declaration_product 表
ALTER TABLE `declaration_product` 
ADD COLUMN `unit_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位代码（关联 measurement_units 表）' AFTER `unit`;

-- 2. 将现有数据的 unit 字段值复制到 unit_code（如果 unit 存的是英文缩写）
-- 使用 BINARY 和 CONVERT 解决字符集冲突问题
UPDATE `declaration_product` 
SET `unit_code` = (
    SELECT `unit_code` 
    FROM `measurement_units` 
    WHERE BINARY CONVERT(`unit_name_en` USING utf8mb4) = BINARY CONVERT(`declaration_product`.`unit` USING utf8mb4)
    LIMIT 1
)
WHERE `unit` IS NOT NULL AND `unit` != '';

-- 3. 验证数据迁移结果
SELECT 
    p.id,
    p.product_name,
    p.unit AS '旧单位字段',
    p.unit_code AS '新单位代码字段',
    mu.unit_name AS '单位中文',
    mu.unit_name_en AS '单位英文'
FROM `declaration_product` p
LEFT JOIN `measurement_units` mu ON p.unit_code = mu.unit_code
WHERE p.unit_code IS NOT NULL
LIMIT 10;

-- 4. 统计迁移结果
SELECT 
    COUNT(*) AS '总记录数',
    COUNT(unit_code) AS '已迁移记录数',
    COUNT(unit_code) / COUNT(*) * 100 AS '迁移完成率'
FROM `declaration_product`;
