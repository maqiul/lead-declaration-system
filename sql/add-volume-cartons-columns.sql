-- 补丁脚本：为 declaration_product 表添加缺失的 volume 和 cartons 列
-- 适用于已初始化但缺少这两个字段的数据库

-- 添加 cartons 列（如果不存在）
SET @dbname = DATABASE();
SET @tablename = 'declaration_product';
SET @columnname = 'cartons';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
    AND TABLE_NAME = @tablename
    AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  'ALTER TABLE `declaration_product` ADD COLUMN `cartons` int NOT NULL DEFAULT 1 COMMENT ''箱数'' AFTER `net_weight`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加 volume 列（如果不存在）
SET @columnname = 'volume';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
    AND TABLE_NAME = @tablename
    AND COLUMN_NAME = @columnname
  ) > 0,
  'SELECT 1',
  'ALTER TABLE `declaration_product` ADD COLUMN `volume` decimal(10,3) NOT NULL DEFAULT 0.000 COMMENT ''体积(CBM)'' AFTER `cartons`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 验证结果
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'declaration_product'
ORDER BY ORDINAL_POSITION;
