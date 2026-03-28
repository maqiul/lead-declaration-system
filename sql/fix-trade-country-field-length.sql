-- 修复申报单表中贸易国字段长度问题
-- 原来的VARCHAR(10)太短，无法存储完整国家名称，如"United States"

-- 修改trade_country字段长度为VARCHAR(100)
ALTER TABLE `declaration_form` MODIFY COLUMN `trade_country` VARCHAR(100) COMMENT '贸易国';

-- 同时修改destination_country字段长度，保持一致性
ALTER TABLE `declaration_form` MODIFY COLUMN `destination_country` VARCHAR(100) COMMENT '目的国';

-- 验证修改结果
DESC `declaration_form`;

-- 检查现有的申报单记录中是否有数据被截断
SELECT 
    id,
    form_no,
    destination_country,
    trade_country,
    CHAR_LENGTH(destination_country) as dest_length,
    CHAR_LENGTH(trade_country) as trade_length
FROM `declaration_form` 
WHERE CHAR_LENGTH(destination_country) >= 100 OR CHAR_LENGTH(trade_country) >= 100;