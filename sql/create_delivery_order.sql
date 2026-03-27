-- ========================================
-- 提货单表创建脚本
-- 生成时间: 2026-03-24
-- 说明: 申报单提货单信息存储
-- ========================================

SET NAMES utf8mb4;

-- 提货单表
CREATE TABLE IF NOT EXISTS `declaration_delivery_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` bigint NOT NULL COMMENT '关联申报单ID',
  `delivery_date` date DEFAULT NULL COMMENT '提货日期',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `file_url` varchar(500) DEFAULT NULL COMMENT '提货单文件URL',
  `file_name` varchar(200) DEFAULT NULL COMMENT '提货单文件名',
  `status` tinyint DEFAULT 0 COMMENT '状态：0-待审核 1-已审核 2-已驳回',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `audit_by` bigint DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申报单提货单表';

-- ========================================
-- 完成提示
-- ========================================
SELECT '提货单表创建完成!' AS message;
