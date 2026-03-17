-- 为 declaration_form 表增加 org_id 字段以支持组织隔离
ALTER TABLE `declaration_form` ADD COLUMN `org_id` BIGINT DEFAULT NULL COMMENT '所属组织ID' AFTER `update_by`;

-- 更新现有数据的 org_id (可选，根据实际情况，这里假设管理员为 1)
-- UPDATE `declaration_form` SET `org_id` = 1 WHERE `org_id` IS NULL;
