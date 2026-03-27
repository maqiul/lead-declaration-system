-- ========================================
-- 财务补充表字段补充脚本 & 水单审核字段脚本
-- 生成时间: 2026-03-24
-- 说明: 补充缺失字段（幂等执行，已存在的字段会跳过）
-- ========================================

SET NAMES utf8mb4;

-- ========================================
-- 第一部分：financial_supplement 表字段检查
-- 说明: 以下字段在当前版本中已存在，此处仅作为备份参考
-- - bank_fee_rate decimal(10,4) - 外汇银行手续费率 ✓ 已存在
-- - foreign_exchange_bank varchar(64) - 外汇银行名称 ✓ 已存在  
-- - tax_refund_rate decimal(10,4) - 退税点 ✓ 已存在
-- - freight_amount decimal(15,2) - 货代金额 ✓ 已存在
-- - customs_amount decimal(15,2) - 报关代理金额 ✓ 已存在
-- - details_amount decimal(15,2) - 开票金额 ✓ 已存在
-- ========================================

-- 如果需要扩展 foreign_exchange_bank 长度到 100，可执行以下语句：
-- ALTER TABLE `financial_supplement` MODIFY COLUMN `foreign_exchange_bank` varchar(100) DEFAULT NULL COMMENT '外汇银行';

-- ========================================
-- 第二部分：水单表(declaration_remittance)添加审核相关字段
-- ========================================

-- 使用存储过程实现幂等添加字段
DROP PROCEDURE IF EXISTS add_remittance_audit_columns;

DELIMITER //

CREATE PROCEDURE add_remittance_audit_columns()
BEGIN
    -- 添加 status 字段
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'declaration_remittance' 
        AND COLUMN_NAME = 'status'
    ) THEN
        ALTER TABLE `declaration_remittance` 
        ADD COLUMN `status` tinyint DEFAULT 0 COMMENT '状态：0-待审核 1-已审核 2-已驳回' AFTER `photo_url`;
    END IF;
    
    -- 添加 audit_remark 字段
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'declaration_remittance' 
        AND COLUMN_NAME = 'audit_remark'
    ) THEN
        ALTER TABLE `declaration_remittance` 
        ADD COLUMN `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注' AFTER `status`;
    END IF;
    
    -- 添加 audit_by 字段
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'declaration_remittance' 
        AND COLUMN_NAME = 'audit_by'
    ) THEN
        ALTER TABLE `declaration_remittance` 
        ADD COLUMN `audit_by` bigint DEFAULT NULL COMMENT '审核人ID' AFTER `audit_remark`;
    END IF;
    
    -- 添加 audit_time 字段
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'declaration_remittance' 
        AND COLUMN_NAME = 'audit_time'
    ) THEN
        ALTER TABLE `declaration_remittance` 
        ADD COLUMN `audit_time` datetime DEFAULT NULL COMMENT '审核时间' AFTER `audit_by`;
    END IF;
    
    SELECT '水单审核字段添加完成!' AS message;
END //

DELIMITER ;

-- 执行存储过程
CALL add_remittance_audit_columns();

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_remittance_audit_columns;

-- 添加索引（如果不存在）
-- 注意：MySQL 不支持 IF NOT EXISTS 语法直接用于 CREATE INDEX，需要捕获错误
-- 以下为简化版本，可根据需要手动添加索引
-- CREATE INDEX idx_remittance_status ON declaration_remittance(status);

-- ========================================
-- 验证字段添加结果
-- ========================================
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'declaration_remittance'
AND COLUMN_NAME IN ('status', 'audit_remark', 'audit_by', 'audit_time')
ORDER BY ORDINAL_POSITION;
