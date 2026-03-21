-- 合同模板和合同生成相关表结构

-- 1. 合同模板表
CREATE TABLE `contract_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码',
  `template_type` varchar(20) NOT NULL COMMENT '模板类型 EXPORT-出口合同 IMPORT-进口合同 OTHER-其他',
  `file_name` varchar(200) NOT NULL COMMENT '模板文件名',
  `file_path` varchar(500) NOT NULL COMMENT '模板文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `description` varchar(500) COMMENT '模板描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同模板表';

-- 2. 合同生成记录表
CREATE TABLE `contract_generation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_no` varchar(50) NOT NULL COMMENT '合同编号',
  `declaration_form_id` bigint NOT NULL COMMENT '关联的申报单ID',
  `template_id` bigint NOT NULL COMMENT '使用的模板ID',
  `generated_file_name` varchar(200) NOT NULL COMMENT '生成的文件名',
  `generated_file_path` varchar(500) NOT NULL COMMENT '生成的文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `generated_by` bigint NOT NULL COMMENT '生成人',
  `generated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-已删除 1-正常',
  `remarks` varchar(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_declaration_form_id` (`declaration_form_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_generated_by` (`generated_by`),
  KEY `idx_generated_time` (`generated_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同生成记录表';

-- 3. 插入默认合同模板数据
INSERT INTO `contract_template` (`template_name`, `template_code`, `template_type`, `file_name`, `file_path`, `description`, `sort`) VALUES
('标准出口合同模板', 'EXPORT_STANDARD_001', 'EXPORT', 'export_contract_template.docx', '/templates/export_standard_template.docx', '标准出口贸易合同模板，包含基本条款和格式', 1),
('标准进口合同模板', 'IMPORT_STANDARD_001', 'IMPORT', 'import_contract_template.docx', '/templates/import_standard_template.docx', '标准进口贸易合同模板，包含基本条款和格式', 2);

-- 4. 创建合同编号生成规则表（可选）
CREATE TABLE `contract_number_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `prefix` varchar(20) COMMENT '编号前缀',
  `date_format` varchar(20) DEFAULT 'yyyyMMdd' COMMENT '日期格式',
  `serial_length` int NOT NULL DEFAULT '6' COMMENT '序号长度',
  `current_serial` int NOT NULL DEFAULT '0' COMMENT '当前序号',
  `reset_type` varchar(20) DEFAULT 'NONE' COMMENT '重置类型 NONE-不重置 DAILY-每日重置 MONTHLY-每月重置 YEARLY-每年重置',
  `description` varchar(500) COMMENT '规则描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同编号生成规则表';

-- 5. 插入默认编号规则
INSERT INTO `contract_number_rule` (`rule_name`, `rule_code`, `prefix`, `date_format`, `serial_length`, `current_serial`, `reset_type`, `description`) VALUES
('出口合同编号规则', 'EXPORT_CONTRACT', 'EC', 'yyyyMMdd', 6, 0, 'DAILY', '出口合同编号生成规则：EC+日期+6位序号'),
('进口合同编号规则', 'IMPORT_CONTRACT', 'IC', 'yyyyMMdd', 6, 0, 'DAILY', '进口合同编号生成规则：IC+日期+6位序号');