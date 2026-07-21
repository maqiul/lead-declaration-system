-- ============================================================
-- 贸易方式(Incoterms)与运输方式关联表
--   建立贸易方式和运输方式的多对多关联
-- ============================================================

CREATE TABLE IF NOT EXISTS `trade_term_transport_mode` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trade_term_code` VARCHAR(20) NOT NULL COMMENT '贸易方式代码',
  `transport_mode_code` VARCHAR(50) NOT NULL COMMENT '运输方式代码',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_term_transport` (`trade_term_code`, `transport_mode_code`),
  KEY `idx_transport_mode_code` (`transport_mode_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='贸易方式与运输方式关联表';

-- 初始化关联数据(根据 Incoterms 2020 规则)
-- E组
INSERT INTO `trade_term_transport_mode` (`trade_term_code`, `transport_mode_code`) VALUES
('EXW', 'AIR'), ('EXW', 'TRUCK'), ('EXW', 'SEA'), ('EXW', 'EXPRESS');

-- F组
INSERT INTO `trade_term_transport_mode` (`trade_term_code`, `transport_mode_code`) VALUES
('FCA', 'AIR'), ('FCA', 'TRUCK'), ('FCA', 'SEA'), ('FCA', 'EXPRESS'),
('FAS', 'SEA'),
('FOB', 'SEA');

-- C组
INSERT INTO `trade_term_transport_mode` (`trade_term_code`, `transport_mode_code`) VALUES
('CFR', 'SEA'),
('CIF', 'SEA'),
('CPT', 'AIR'), ('CPT', 'TRUCK'), ('CPT', 'SEA'), ('CPT', 'EXPRESS'),
('CIP', 'AIR'), ('CIP', 'TRUCK'), ('CIP', 'SEA'), ('CIP', 'EXPRESS');

-- D组
INSERT INTO `trade_term_transport_mode` (`trade_term_code`, `transport_mode_code`) VALUES
('DAP', 'AIR'), ('DAP', 'TRUCK'), ('DAP', 'SEA'), ('DAP', 'EXPRESS'),
('DPU', 'AIR'), ('DPU', 'TRUCK'), ('DPU', 'SEA'), ('DPU', 'EXPRESS'),
('DDP', 'AIR'), ('DDP', 'TRUCK'), ('DDP', 'SEA'), ('DDP', 'EXPRESS');
