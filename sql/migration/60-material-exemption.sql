-- ============================================================
-- 资料豁免审批记录表
--   当资料提交时必填文件不全，用户选择强制提交后，
--   系统创建豁免记录并启动独立审核流程。
--   主流程 materialSubmit 任务阻塞，直到豁免通过后才 complete。
-- ============================================================

CREATE TABLE IF NOT EXISTS `declaration_material_exemption` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` BIGINT NOT NULL COMMENT '申报单ID',
  `stage` VARCHAR(50) NOT NULL DEFAULT 'MATERIAL_SUBMIT' COMMENT '提交阶段(materialSubmit等)',
  `missing_items` TEXT NOT NULL COMMENT '缺失的必填资料项JSON数组 [{code,name,invoiceMode}]',
  `exemption_type` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '豁免类型: NORMAL-普通文件 INVOICE-发票类 MIXED-混合',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核 1-已通过 2-已驳回',
  `main_task_id` VARCHAR(64) DEFAULT NULL COMMENT '主流程被阻塞的Flowable任务ID',
  `process_instance_id` VARCHAR(64) DEFAULT NULL COMMENT '豁免流程实例ID',
  `audit_by` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资料豁免审批记录表';

-- 豁免审核权限菜单（挂在资料审核菜单下）
INSERT INTO `sys_menu`
  (`menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
   `create_time`, `update_time`)
SELECT '豁免审核', 'exemption-audit', m.id, 3, NULL, NULL,
   'business:declaration:exemption:audit', NULL, 10, 0, 0, 1, 1, 0,
   NOW(), NOW()
FROM `sys_menu` m WHERE m.`permission` = 'business:declaration:audit:material' AND m.`deleted` = 0
LIMIT 1
ON DUPLICATE KEY UPDATE `status` = 1, `deleted` = 0;
