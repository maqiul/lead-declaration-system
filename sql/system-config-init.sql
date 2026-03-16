-- 系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `config_value` text COMMENT '配置值',
  `input_type` tinyint DEFAULT '1' COMMENT '配置输入类型 1-文本框 2-下拉框 3-开关 4-数字输入框',
  `select_options` json COMMENT '下拉框选项JSON(当input_type=2时使用)',
  `is_system_param` tinyint DEFAULT '0' COMMENT '是否系统内置参数 0-否 1-是',
  `config_type` tinyint NOT NULL DEFAULT '1' COMMENT '配置类型 1-系统配置 2-UI配置 3-业务配置',
  `config_group` varchar(50) NOT NULL COMMENT '配置分组',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_group` (`config_group`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 插入系统基本信息配置
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `input_type`, `config_type`, `config_group`, `is_system_param`, `remark`, `sort`) VALUES
('system.name', '系统名称', '线索申报系统', 1, 1, 'basic', 1, '系统显示名称', 1),
('system.version', '系统版本', '1.0.0', 1, 1, 'basic', 1, '系统版本号', 2),
('system.description', '系统描述', '企业线索申报管理系统', 1, 1, 'basic', 1, '系统功能描述', 3),
('system.company', '公司名称', 'XXX科技有限公司', 1, 1, 'basic', 1, '系统所属公司', 4),
('system.copyright', '版权信息', '© 2026 XXX科技有限公司 版权所有', 1, 1, 'basic', 1, '系统版权信息', 5);

-- 插入UI配置
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `input_type`, `select_options`, `config_type`, `config_group`, `is_system_param`, `remark`, `sort`) VALUES
('ui.logo', 'Logo图片', '/logo.png', 1, NULL, 2, 'ui', 1, '系统Logo图片URL', 1),
('ui.favicon', '网站图标', '/favicon.ico', 1, NULL, 2, 'ui', 1, '浏览器标签页图标', 2),
('ui.theme', '主题颜色', '#1890ff', 2, '[{"label":"默认蓝","value":"#1890ff"},{"label":"科技蓝","value":"#001529"},{"label":"活力橙","value":"#fa8c16"},{"label":"清新绿","value":"#52c41a"}]', 2, 'ui', 1, '系统主题色', 3),
('ui.footer.text', '底部文字', '线索申报系统 ©2026 Created by Admin', 1, NULL, 2, 'ui', 1, '页面底部显示文字', 4),
('ui.footer.show', '显示底部', 'true', 3, NULL, 2, 'ui', 1, '是否显示页面底部', 5),
('ui.sidebar.collapsed', '侧边栏折叠', 'false', 3, NULL, 2, 'ui', 1, '侧边栏默认是否折叠', 6),
('ui.language', '系统语言', 'zh-CN', 2, '[{"label":"简体中文","value":"zh-CN"},{"label":"English","value":"en-US"}]', 2, 'ui', 0, '系统显示语言', 7);

-- 插入业务配置
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `input_type`, `select_options`, `config_type`, `config_group`, `is_system_param`, `remark`, `sort`) VALUES
('business.tax-refund.enabled', '税务退费功能', 'true', 3, NULL, 3, 'business', 0, '是否启用税务退费功能', 1),
('business.tax-refund.approval-level', '审批层级', '3', 2, '[{"label":"1级审批","value":"1"},{"label":"2级审批","value":"2"},{"label":"3级审批","value":"3"},{"label":"4级审批","value":"4"},{"label":"5级审批","value":"5"}]', 3, 'business', 0, '税务退费审批层级数', 2),
('business.file.upload.max-size', '文件上传大小限制', '10MB', 1, NULL, 3, 'business', 0, '文件上传最大大小', 3),
('business.notification.email-enabled', '邮件通知', 'true', 3, NULL, 3, 'business', 0, '是否启用邮件通知', 4),
('business.default-department', '默认部门', 'tech', 2, '[{"label":"技术部","value":"tech"},{"label":"销售部","value":"sales"},{"label":"财务部","value":"finance"},{"label":"人事部","value":"hr"}]', 3, 'business', 0, '业务默认归属部门', 5);

-- 插入菜单权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(1, '系统配置', 'system_config', 2, 'config', 'system/config/index', 'system:config:list', 'SettingOutlined', 5, 1),
(110, '配置查询', 'config_query', 3, '', '', 'system:config:query', '', 1, 1),
(110, '配置新增', 'config_add', 3, '', '', 'system:config:add', '', 2, 1),
(110, '配置修改', 'config_update', 3, '', '', 'system:config:update', '', 3, 1),
(110, '配置删除', 'config_delete', 3, '', '', 'system:config:delete', '', 4, 1);

-- 创建索引
CREATE INDEX idx_sys_config_key_status ON sys_config(config_key, status);
CREATE INDEX idx_sys_config_group_type ON sys_config(config_group, config_type);