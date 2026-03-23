-- 操作日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` bigint DEFAULT NULL COMMENT '用户ID',
    `username` varchar(50) DEFAULT NULL COMMENT '用户名',
    `operation_type` varchar(50) NOT NULL COMMENT '操作类型（CREATE/UPDATE/DELETE/QUERY/EXPORT/IMPORT等）',
    `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型（申报管理/合同管理/系统配置等）',
    `method` varchar(10) DEFAULT NULL COMMENT '请求方法',
    `request_url` varchar(255) DEFAULT NULL COMMENT '请求URL',
    `request_params` text COMMENT '请求参数',
    `response_result` text COMMENT '响应结果',
    `ip_address` varchar(50) DEFAULT NULL COMMENT '操作IP地址',
    `location` varchar(255) DEFAULT NULL COMMENT '操作地点',
    `browser` varchar(50) DEFAULT NULL COMMENT '浏览器类型',
    `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
    `status` tinyint(1) DEFAULT '0' COMMENT '操作状态（0成功 1失败）',
    `error_msg` text COMMENT '错误消息',
    `cost_time` bigint DEFAULT NULL COMMENT '操作耗时（毫秒）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_business_type` (`business_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 初始化一些示例数据（可选）
INSERT INTO `sys_operation_log` (`user_id`, `username`, `operation_type`, `business_type`, `method`, `request_url`, `ip_address`, `browser`, `os`, `status`, `cost_time`) VALUES
(1, 'admin', 'LOGIN', '系统管理', 'POST', '/user/login', '127.0.0.1', 'Chrome', 'Windows', 0, 156),
(1, 'admin', 'QUERY', '申报管理', 'GET', '/declaration/list', '127.0.0.1', 'Chrome', 'Windows', 0, 89),
(1, 'admin', 'CREATE', '合同管理', 'POST', '/contract/generate', '127.0.0.1', 'Chrome', 'Windows', 0, 234);