-- Create declaration_attachment table to store multiple files per declaration
CREATE TABLE IF NOT EXISTS `declaration_attachment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `form_id` BIGINT NOT NULL COMMENT '申报单ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件下载路径',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型 (Invoice, PackingList, etc.)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_form_id` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报单附件表';
