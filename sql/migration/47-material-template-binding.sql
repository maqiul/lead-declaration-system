-- ============================================================
-- 资料模板绑定表（流程 + 运输方式 双维度）
-- 每条规则: 资料模板在 某流程 AND 某运输方式 下适用
-- ============================================================

CREATE TABLE `declaration_material_template_binding` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `template_id` BIGINT NOT NULL COMMENT '资料模板ID',
  `flow_template_code` VARCHAR(60) DEFAULT NULL COMMENT '流程模板编码(空=任意流程)',
  `transport_mode_code` VARCHAR(50) DEFAULT NULL COMMENT '运输方式编码(空=任意运输方式)',
  KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料模板绑定表(流程+运输方式)';

-- 匹配规则：
-- - 模板无绑定记录 → 所有流程 + 所有运输方式 都适用（向下兼容）
-- - 模板有绑定记录 → 任一行匹配即可（flow 匹配或空 AND transport 匹配或空）
