-- Add image_id to declaration_product
ALTER TABLE `declaration_product` ADD COLUMN `image_id` BIGINT DEFAULT NULL COMMENT '产品图片ID' AFTER `sort_order`;

-- Make form_id nullable in declaration_attachment to support standalone upload
ALTER TABLE `declaration_attachment` MODIFY COLUMN `form_id` BIGINT DEFAULT NULL COMMENT '申报单ID';
