-- 财务补充表银行字段结构调整
-- 为foreignExchangeBank添加对应的ID字段

SET NAMES utf8mb4;

-- 添加外键字段存储银行ID
ALTER TABLE `financial_supplement` 
ADD COLUMN `foreign_exchange_bank_id` BIGINT DEFAULT NULL COMMENT '外汇银行ID' AFTER `tax_refund_rate`;

-- 如果原来的foreign_exchange_bank字段存储的是ID，需要迁移数据
-- 这里假设原来存储的就是银行ID，将其迁移到新的字段中
UPDATE `financial_supplement` 
SET `foreign_exchange_bank_id` = CAST(`foreign_exchange_bank` AS UNSIGNED)
WHERE `foreign_exchange_bank` REGEXP '^[0-9]+$' 
AND `foreign_exchange_bank` IS NOT NULL 
AND `foreign_exchange_bank` != '';

-- 为新字段添加索引
ALTER TABLE `financial_supplement` 
ADD INDEX `idx_foreign_exchange_bank_id` (`foreign_exchange_bank_id`);

-- 验证修改结果
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'financial_supplement'
AND COLUMN_NAME IN ('foreign_exchange_bank', 'foreign_exchange_bank_id')
ORDER BY ORDINAL_POSITION;