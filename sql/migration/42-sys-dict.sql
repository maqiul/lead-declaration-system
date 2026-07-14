-- ============================================================
-- 系统字典配置表
-- ============================================================

-- 1. 字典类型表
CREATE TABLE IF NOT EXISTS `sys_dict` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `dict_code`   VARCHAR(60)  NOT NULL COMMENT '字典编码（唯一）',
  `dict_name`   VARCHAR(100) NOT NULL COMMENT '字典名称',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `del_flag`    TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志 0-正常 1-删除',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT       DEFAULT NULL,
  `update_by`   BIGINT       DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典类型表';

-- 2. 字典项表
CREATE TABLE IF NOT EXISTS `sys_dict_item` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `dict_code`   VARCHAR(60)  NOT NULL COMMENT '所属字典编码',
  `item_value`  VARCHAR(100) NOT NULL COMMENT '字典项值',
  `item_label`  VARCHAR(100) NOT NULL COMMENT '字典项显示文本',
  `item_color`  VARCHAR(20)  DEFAULT NULL COMMENT '标签颜色（Ant Design Tag 颜色）',
  `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `del_flag`    TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志 0-正常 1-删除',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by`   BIGINT       DEFAULT NULL,
  `update_by`   BIGINT       DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典项表';

-- ============================================================
-- 种子数据：预置字典类型
-- ============================================================
INSERT INTO `sys_dict` (`dict_code`, `dict_name`, `status`, `remark`) VALUES
('process_type', '流程类型', 1, '申报/水单/税务退费流程分类'),
('form_section', '表单区块', 1, '申报表单的功能区块标识'),
('node_type',    '节点类型', 1, 'BPMN流程节点类型');

-- 种子数据：字典项
INSERT INTO `sys_dict_item` (`dict_code`, `item_value`, `item_label`, `item_color`, `sort_order`) VALUES
-- process_type
('process_type', 'declaration', '申报',     'blue',   1),
('process_type', 'remittance',  '水单',     'green',  2),
('process_type', 'taxRefund',   '税务退费', 'orange', 3),
-- form_section
('form_section', 'basic',         'basic - 基本信息',       'blue',   1),
('form_section', 'material',      'material - 资料',        'green',  2),
('form_section', 'supplement',    'supplement - 补充资料',  'orange', 3),
('form_section', 'invoiceAmount', 'invoiceAmount - 开票金额', 'purple', 4),
('form_section', 'invoice',       'invoice - 发票',         'cyan',   5),
-- node_type
('node_type', 'userTask',    '用户任务', 'blue',    1),
('node_type', 'serviceTask', '系统任务', 'default', 2);

