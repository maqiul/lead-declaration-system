-- invoice_split_item 新增 hs_code 列
ALTER TABLE `invoice_split_item`
  ADD COLUMN `hs_code` VARCHAR(20) DEFAULT NULL COMMENT 'HS编码' AFTER `form_id`;
