-- ============================================================
-- 新增补充资料流程 + 申请开票金额流程
-- 状态迁移: 4→8, 5→9, 6→10, 9→11
-- ============================================================

-- 1. 新增字段
ALTER TABLE `declaration_form` ADD COLUMN `requested_invoice_amount` DECIMAL(18,4) DEFAULT NULL COMMENT '申请开票金额' AFTER `total_amount`;

-- 2. 迁移现有数据 (按顺序避免冲突)
-- 9(退回待审) → 11
UPDATE `declaration_form` SET `status` = 11 WHERE `status` = 9;
-- 6(已完成) → 10
UPDATE `declaration_form` SET `status` = 10 WHERE `status` = 6;
-- 5(待发票审核) → 9
UPDATE `declaration_form` SET `status` = 9 WHERE `status` = 5;
-- 4(待发票提交) → 8
UPDATE `declaration_form` SET `status` = 8 WHERE `status` = 4;
