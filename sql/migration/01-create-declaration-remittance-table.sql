-- 水单信息表
CREATE TABLE `declaration_remittance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `form_id` BIGINT NOT NULL COMMENT '申报单ID',
    `remittance_type` TINYINT NOT NULL DEFAULT 1 COMMENT '水单类型: 1-定金, 2-尾款',
    `remittance_name` VARCHAR(100) NOT NULL COMMENT '收汇名称',
    `remittance_date` DATE NOT NULL COMMENT '收汇日期',
    `remittance_amount` DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '收汇金额($)',
    `exchange_rate` DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '当日汇率',
    `bank_fee` DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '手续费($)',
    `credited_amount` DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '入账金额($)',
    `remarks` TEXT COMMENT '备注',
    `photo_url` VARCHAR(500) COMMENT '水单照片 URL',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_form_id` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水单信息表';
