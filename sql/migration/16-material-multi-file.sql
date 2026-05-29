-- ============================================================
-- 资料项多文件支持：新建附件子表 + 历史数据迁移
-- ============================================================

-- 1. 新建附件子表（一对多）
CREATE TABLE IF NOT EXISTS `declaration_material_attachment` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `item_id`      BIGINT        NOT NULL COMMENT '资料项实例ID',
    `file_name`    VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    `file_url`     VARCHAR(500)  NOT NULL COMMENT '下载地址',
    `file_size`    BIGINT        DEFAULT NULL COMMENT '文件大小(字节)',
    -- 发票类附件结构化字段（仅发票类资料项使用）
    `amount`       DECIMAL(18,4) DEFAULT NULL COMMENT '发票金额',
    `currency`     VARCHAR(10)   DEFAULT NULL COMMENT '币种',
    `invoice_no`   VARCHAR(100)  DEFAULT NULL COMMENT '发票号',
    `invoice_date` DATE          DEFAULT NULL COMMENT '开票日期',
    `extra_data`   TEXT          DEFAULT NULL COMMENT '扩展字段JSON',
    --
    `upload_by`    BIGINT        DEFAULT NULL COMMENT '上传人ID',
    `upload_time`  DATETIME      DEFAULT NULL COMMENT '上传时间',
    `create_by`    BIGINT        DEFAULT NULL COMMENT '创建人ID',
    `update_by`    BIGINT        DEFAULT NULL COMMENT '更新人ID',
    `stage`        VARCHAR(50)   DEFAULT NULL COMMENT '所属环节',
    `create_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资料项附件（多文件）';


-- 2. 附件表补列（表已存在时幂等加列）
ALTER TABLE `declaration_material_attachment`
    ADD COLUMN IF NOT EXISTS `file_size`    BIGINT        DEFAULT NULL COMMENT '文件大小(字节)'  AFTER `file_url`,
    ADD COLUMN IF NOT EXISTS `amount`       DECIMAL(18,4) DEFAULT NULL COMMENT '发票金额'        AFTER `file_size`,
    ADD COLUMN IF NOT EXISTS `currency`     VARCHAR(10)   DEFAULT NULL COMMENT '币种'            AFTER `amount`,
    ADD COLUMN IF NOT EXISTS `invoice_no`   VARCHAR(100)  DEFAULT NULL COMMENT '发票号'          AFTER `currency`,
    ADD COLUMN IF NOT EXISTS `invoice_date` DATE          DEFAULT NULL COMMENT '开票日期'        AFTER `invoice_no`,
    ADD COLUMN IF NOT EXISTS `extra_data`   TEXT          DEFAULT NULL COMMENT '扩展字段JSON'    AFTER `invoice_date`,
    ADD COLUMN IF NOT EXISTS `upload_by`    BIGINT        DEFAULT NULL COMMENT '上传人ID'        AFTER `extra_data`,
    ADD COLUMN IF NOT EXISTS `upload_time`  DATETIME      DEFAULT NULL COMMENT '上传时间'        AFTER `upload_by`,
    ADD COLUMN IF NOT EXISTS `create_by`    BIGINT        DEFAULT NULL COMMENT '创建人ID'        AFTER `upload_time`,
    ADD COLUMN IF NOT EXISTS `update_by`    BIGINT        DEFAULT NULL COMMENT '更新人ID'        AFTER `create_by`,
    ADD COLUMN IF NOT EXISTS `stage`        VARCHAR(50)   DEFAULT NULL COMMENT '所属环节'        AFTER `update_by`;

-- 3. 资料模板表补列（所属环节，表已存在时幂等加列）
ALTER TABLE `declaration_material_template`
    ADD COLUMN IF NOT EXISTS `stage` VARCHAR(50) NULL COMMENT '所属环节' AFTER `enabled`;

-- 4. 迁移历史单文件数据到子表
-- 幂等：同一 item_id + file_url 已存在则跳过
INSERT INTO `declaration_material_attachment` (
    `item_id`, `file_name`, `file_url`,
    `amount`, `currency`, `invoice_no`, `invoice_date`, `extra_data`,
    `upload_by`, `upload_time`,
    `create_by`, `update_by`, `stage`,
    `create_time`
)
SELECT
    `id`, `file_name`, `file_url`,
    `amount`, `currency`, `invoice_no`, `invoice_date`, `extra_data`,
    `upload_by`, `upload_time`,
    `create_by`, `update_by`, `stage`,
    COALESCE(`create_time`, `upload_time`, NOW())
FROM `declaration_material_item`
WHERE `file_url` IS NOT NULL AND `file_url` != ''
  AND NOT EXISTS (
      SELECT 1 FROM `declaration_material_attachment` a
      WHERE a.`item_id` = `declaration_material_item`.`id`
        AND a.`file_url` = `declaration_material_item`.`file_url`
  );
