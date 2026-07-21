-- ============================================================
-- 申报单增加贸易方式和到达港口字段
-- ============================================================

ALTER TABLE `declaration_form` 
ADD COLUMN `trade_term` VARCHAR(20) DEFAULT NULL COMMENT '贸易方式代码(Incoterms)' AFTER `transport_mode`,
ADD COLUMN `arrival_port` VARCHAR(200) DEFAULT NULL COMMENT '到达港口(C组/D组贸易方式时必填)' AFTER `trade_term`;
