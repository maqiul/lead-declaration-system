-- 退税功能表结构修复补丁
-- 修复逻辑删除字段不一致问题

-- 1. 为现有的退税申请表添加deleted字段（如果不存在）
ALTER TABLE `tax_refund_application` 
ADD COLUMN IF NOT EXISTS `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识(0:未删除,1:已删除)';

-- 2. 为现有的退税附件表添加deleted字段（如果不存在）
ALTER TABLE `tax_refund_attachment` 
ADD COLUMN IF NOT EXISTS `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识(0:未删除,1:已删除)';

-- 3. 为现有的退税审核记录表添加deleted字段（如果不存在）
ALTER TABLE `tax_refund_review_record` 
ADD COLUMN IF NOT EXISTS `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识(0:未删除,1:已删除)';

-- 4. 为现有的退税流程节点表添加deleted字段（如果不存在）
ALTER TABLE `tax_refund_process_node` 
ADD COLUMN IF NOT EXISTS `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识(0:未删除,1:已删除)';

-- 5. 如果原来的del_flag字段存在，将其重命名为deleted
ALTER TABLE `tax_refund_application` 
CHANGE COLUMN IF EXISTS `del_flag` `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识(0:未删除,1:已删除)';

-- 6. 验证表结构
SELECT 
  TABLE_NAME as '表名',
  COLUMN_NAME as '字段名',
  COLUMN_TYPE as '字段类型',
  IS_NULLABLE as '允许为空',
  COLUMN_DEFAULT as '默认值',
  COLUMN_COMMENT as '字段说明'
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME LIKE 'tax_refund_%'
  AND COLUMN_NAME = 'deleted'
ORDER BY TABLE_NAME, ORDINAL_POSITION;