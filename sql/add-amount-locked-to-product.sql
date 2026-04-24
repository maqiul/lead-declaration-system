-- 为产品表添加金额锁定字段
-- 日期: 2026-04-24
-- 说明: 用户手动输入金额后，保存时标记锁定，下次进入不再自动计算

ALTER TABLE `declaration_product`
ADD COLUMN `amount_locked` tinyint DEFAULT 0 COMMENT '金额是否锁定: 1-锁定(用户手动输入), 0-自动计算';
