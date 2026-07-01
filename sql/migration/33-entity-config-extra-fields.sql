-- 主体配置新增：纳税人识别号、电话、开户银行
ALTER TABLE `entity_config`
  ADD COLUMN `tax_id` VARCHAR(100) DEFAULT NULL COMMENT '纳税人识别号' AFTER `entity_address_cn`,
  ADD COLUMN `phone` VARCHAR(50) DEFAULT NULL COMMENT '电话' AFTER `tax_id`,
  ADD COLUMN `bank_account` VARCHAR(200) DEFAULT NULL COMMENT '开户银行' AFTER `phone`;
