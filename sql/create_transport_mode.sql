-- 运输方式配置表
CREATE TABLE IF NOT EXISTS transport_mode (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '运输方式名称(英文)',
    chinese_name VARCHAR(100) DEFAULT '' COMMENT '运输方式名称(中文)',
    code VARCHAR(50) DEFAULT '' COMMENT '运输方式代码',
    description VARCHAR(255) DEFAULT '' COMMENT '描述',
    status INT DEFAULT 1 COMMENT '状态(0禁用 1启用)',
    sort INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    del_flag INT DEFAULT 0 COMMENT '删除标志(0正常 1删除)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运输方式配置';

-- 初始数据
INSERT INTO transport_mode (name, chinese_name, code, sort) VALUES
('AIR FREIGHT', '空运', 'AIR', 1),
('TRUCK', '陆运', 'TRUCK', 2),
('BY SEA', '海运', 'SEA', 3),
('EXPRESS', '快递', 'EXPRESS', 4);
